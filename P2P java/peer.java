import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {

    private static final int BUFFER_SIZE = 4096;
    private static final String SHARED_DIR = "shared";
    private static final String DOWNLOAD_DIR = "downloads";

    private int port;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private List<ConnectionHandler> connections = new ArrayList<>();
    private PeerDiscoveryService discoveryService;

    public Peer(int port) {
        this.port = port;
        this.discoveryService = new PeerDiscoveryService(port);
    }

    public void start() {
        discoveryService.start();
        new Thread(this::startServer).start();
        startCLI();
    }

    // Server-side: Listen for incoming peer connections
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening for peers on port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Handle incoming connection
    private void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream outStream = socket.getOutputStream();
        ) {
            String command = in.readLine();
            if (command.startsWith("search")) {
                handleSearch(command, outStream);
            } else if (command.startsWith("download")) {
                handleDownload(command, outStream);
            }
        } catch (IOException e) {
            System.err.println("Client handling error: " + e.getMessage());
        }
    }

    // Search for a file in the shared folder
    private void handleSearch(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 2) return;
        String keyword = parts[1].toLowerCase();
        File folder = new File(SHARED_DIR);
        PrintWriter out = new PrintWriter(outStream, true);

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(keyword)) {
                    out.println(file.getName());
                }
            }
        }
        out.println("END");
    }

    // Send a file to the requesting peer, supporting download resume
    private void handleDownload(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 3) return;
        String fileName = parts[1];
        long offset = Long.parseLong(parts[2]);
        File file = new File(SHARED_DIR + "/" + fileName);

        DataOutputStream dataOut = new DataOutputStream(outStream);

        if (!file.exists()) {
            dataOut.writeLong(-1);
            return;
        }

        long fileSize = file.length();
        if (offset >= fileSize) {
            // File already fully downloaded, or offset is invalid
            dataOut.writeLong(0); // Tell client to not expect any more bytes
            return;
        }

        dataOut.writeLong(fileSize - offset);

        try (RandomAccessFile fileIn = new RandomAccessFile(file, "r")) {
            fileIn.seek(offset); // Start reading from the specified offset
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
        }
    }

    // CLI for user commands
    private void startCLI() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            String[] parts = command.split(" ");

            if (parts.length == 0) continue;

            switch (parts[0]) {
                case "connect":
                    if (parts.length != 3) {
                        System.out.println("Usage: connect <host> <port>");
                        continue;
                    }
                    String host = parts[1];
                    int newPort = Integer.parseInt(parts[2]);
                    connections.add(new ConnectionHandler(host, newPort));
                    System.out.println("Connected to " + host + ":" + newPort);
                    break;
                case "discover":
                    System.out.println("Discovered peers:");
                    Set<String> peers = discoveryService.getDiscoveredPeers();
                    if (peers.isEmpty()) {
                        System.out.println("No peers found. Waiting for broadcasts...");
                    } else {
                        peers.forEach(System.out::println);
                    }
                    break;
                case "search":
                case "download":
                    if (connections.isEmpty()) {
                        System.out.println("No active connections. Use 'discover' or 'connect' first.");
                        continue;
                    }
                    for (ConnectionHandler conn : connections) {
                        conn.sendCommand(command);
                    }
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    discoveryService.shutdown();
                    scanner.close();
                    threadPool.shutdownNow();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }

    // Handles outgoing connections to another peer
    private class ConnectionHandler {
        private String host;
        private int port;

        public ConnectionHandler(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public void sendCommand(String command) {
            try (
                Socket socket = new Socket(host, port);
                InputStream inStream = socket.getInputStream();
            ) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                if (command.startsWith("search")) {
                    out.println(command);
                    BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                    System.out.println("[" + host + ":" + port + "] Search results:");
                    String line;
                    while ((line = in.readLine()) != null && !line.equals("END")) {
                        System.out.println(" - " + line);
                    }
                } else if (command.startsWith("download")) {
                    String[] parts = command.split(" ");
                    if (parts.length < 2) return;
                    String fileName = parts[1];
                    File outFile = new File(DOWNLOAD_DIR + "/" + fileName);

                    long existingSize = 0;
                    if (outFile.exists()) {
                        existingSize = outFile.length();
                        System.out.println("Resuming download of " + fileName + " from " + existingSize + " bytes.");
                    }
                    
                    out.println(command + " " + existingSize);

                    DataInputStream dataIn = new DataInputStream(inStream);
                    long remainingSize = dataIn.readLong();
                    if (remainingSize == -1) {
                        System.out.println("File not found on peer.");
                        return;
                    }
                    if (remainingSize == 0) {
                        System.out.println("File already fully downloaded: " + fileName);
                        return;
                    }

                    try (RandomAccessFile fileOut = new RandomAccessFile(outFile, "rw")) {
                        fileOut.seek(existingSize); // Start writing from the end of the existing file
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long totalRead = 0;
                        int bytesRead;
                        while (totalRead < remainingSize && (bytesRead = dataIn.read(buffer)) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }
                        System.out.println("File downloaded: " + fileName);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection to " + host + ":" + port + " failed: " + e.getMessage());
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Peer <port>");
            return;
        }
        new File(SHARED_DIR).mkdirs();
        new File(DOWNLOAD_DIR).mkdirs();

        int port = Integer.parseInt(args[0]);
        new Peer(port).start();
    }
}