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

    public Peer(int port) {
        this.port = port;
    }

    public void start() {
        // Start the server thread
        new Thread(this::startServer).start();

        // Start CLI for client commands
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

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().toLowerCase().contains(keyword)) {
                out.println(file.getName());
            }
        }
        out.println("END");
    }

    // Send a file to the requesting peer
    private void handleDownload(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 2) return;
        String fileName = parts[1];
        File file = new File(SHARED_DIR + "/" + fileName);

        DataOutputStream dataOut = new DataOutputStream(outStream);

        if (!file.exists()) {
            dataOut.writeLong(-1);
            return;
        }

        long fileSize = file.length();
        dataOut.writeLong(fileSize);

        try (FileInputStream fileIn = new FileInputStream(file)) {
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
                    int port = Integer.parseInt(parts[2]);
                    connections.add(new ConnectionHandler(host, port));
                    System.out.println("Connected to " + host + ":" + port);
                    break;

                case "search":
                case "download":
                    if (connections.isEmpty()) {
                        System.out.println("No active connections.");
                        continue;
                    }
                    for (ConnectionHandler conn : connections) {
                        conn.sendCommand(command);
                    }
                    break;

                case "exit":
                    System.out.println("Exiting...");
                    scanner.close();
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
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                InputStream inStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            ) {
                out.println(command);

                if (command.startsWith("search")) {
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

                    DataInputStream dataIn = new DataInputStream(inStream);
                    long fileSize = dataIn.readLong();
                    if (fileSize == -1) {
                        System.out.println("File not found on peer.");
                        return;
                    }

                    try (FileOutputStream fileOut = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long totalRead = 0;
                        int bytesRead;
                        while (totalRead < fileSize && (bytesRead = dataIn.read(buffer)) != -1) {
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

        // Ensure directories exist
        new File(SHARED_DIR).mkdirs();
        new File(DOWNLOAD_DIR).mkdirs();

        int port = Integer.parseInt(args[0]);
        new Peer(port).start();
    }
}
