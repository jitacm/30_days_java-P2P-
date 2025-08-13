import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class Peer {

    // A nested interface to allow the Peer class to communicate back to the GUI
    public interface PeerListener {
        void onMessageReceived(String message);
        void onSearchResults(String host, int port, List<String> results);
        void onDownloadProgress(String fileName, long totalBytes, long downloadedBytes);
    }

    private static final int BUFFER_SIZE = 4096;
    private static final String SHARED_DIR = "shared";
    private static final String DOWNLOAD_DIR = "downloads";

    private int port;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private List<ConnectionHandler> connections = new ArrayList<>();
    private PeerDiscoveryService discoveryService;
    private PeerListener listener;

    public Peer(int port) {
        this.port = port;
        this.discoveryService = new PeerDiscoveryService(port);
        // Use java.nio.file.Path for modern file operations
        Path sharedDirPath = Paths.get(SHARED_DIR);
        Path downloadDirPath = Paths.get(DOWNLOAD_DIR);
        try {
            if (Files.notExists(sharedDirPath)) {
                Files.createDirectories(sharedDirPath);
            }
            if (Files.notExists(downloadDirPath)) {
                Files.createDirectories(downloadDirPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
    }

    public void setPeerListener(PeerListener listener) {
        this.listener = listener;
    }

    public void start() {
        discoveryService.start();
        new Thread(this::startServer).start();
    }
    
    // Server-side: Listen for incoming peer connections
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            if (listener != null) {
                listener.onMessageReceived("Listening for peers on port " + port + "...");
            }
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onMessageReceived("Server error: " + e.getMessage());
            }
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
            if (listener != null) {
                listener.onMessageReceived("Client handling error: " + e.getMessage());
            }
        }
    }

    // Search for a file in the shared folder
    private void handleSearch(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 2) return;
        String keyword = parts[1].toLowerCase();
        Path sharedPath = Paths.get(SHARED_DIR);
        PrintWriter out = new PrintWriter(outStream, true);

        try (var stream = Files.list(sharedPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().toLowerCase().contains(keyword))
                  .forEach(p -> out.println(p.getFileName().toString()));
        }
        out.println("END");
    }

    // Send a file to the requesting peer, including a checksum
    private void handleDownload(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 3) return;
        String fileName = parts[1];
        long offset = Long.parseLong(parts[2]);
        Path filePath = Paths.get(SHARED_DIR, fileName);

        DataOutputStream dataOut = new DataOutputStream(outStream);

        if (Files.notExists(filePath)) {
            dataOut.writeLong(-1);
            return;
        }

        try {
            String checksum = getFileChecksum(filePath.toFile());
            dataOut.writeUTF(checksum);

            long fileSize = Files.size(filePath);
            if (offset >= fileSize) {
                dataOut.writeLong(0);
                return;
            }

            dataOut.writeLong(fileSize - offset);

            // Use try-with-resources for RandomAccessFile
            try (RandomAccessFile fileIn = new RandomAccessFile(filePath.toFile(), "r")) {
                fileIn.seek(offset);
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            if (listener != null) {
                listener.onMessageReceived("Checksum algorithm not found: " + e.getMessage());
            }
            dataOut.writeLong(-1);
        }
    }

    private String getFileChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashedBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Public methods for GUI to call
    public void connect(String host, int port) {
        connections.add(new ConnectionHandler(host, port));
        if (listener != null) {
            listener.onMessageReceived("Connected to " + host + ":" + port);
        }
    }

    public void search(String keyword) {
        String command = "search " + keyword;
        if (connections.isEmpty()) {
            if (listener != null) {
                listener.onMessageReceived("No active connections. Use 'connect' or 'discover' first.");
            }
            return;
        }
        for (ConnectionHandler conn : connections) {
            conn.sendCommand(command);
        }
    }

    public void download(String fileName) {
        String command = "download " + fileName;
        if (connections.isEmpty()) {
            if (listener != null) {
                listener.onMessageReceived("No active connections. Use 'connect' or 'discover' first.");
            }
            return;
        }
        for (ConnectionHandler conn : connections) {
            conn.sendCommand(command);
        }
    }

    public void discoverPeers() {
        if (listener != null) {
            listener.onMessageReceived("Discovered peers:");
            Set<String> peers = discoveryService.getDiscoveredPeers();
            if (peers.isEmpty()) {
                listener.onMessageReceived("No peers found. Waiting for broadcasts...");
            } else {
                peers.forEach(listener::onMessageReceived);
            }
        }
    }

    public void shutdown() {
        discoveryService.shutdown();
        threadPool.shutdownNow();
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
                OutputStream outStream = socket.getOutputStream();
            ) {
                PrintWriter out = new PrintWriter(outStream, true);
                
                if (command.startsWith("search")) {
                    out.println(command);
                    BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                    List<String> results = new ArrayList<>();
                    String line;
                    while ((line = in.readLine()) != null && !line.equals("END")) {
                        results.add(line);
                    }
                    if (listener != null) {
                        listener.onSearchResults(host, port, results);
                    }
                } else if (command.startsWith("download")) {
                    String[] parts = command.split(" ");
                    if (parts.length < 2) return;
                    String fileName = parts[1];
                    Path downloadPath = Paths.get(DOWNLOAD_DIR, fileName);

                    long existingSize = 0;
                    if (Files.exists(downloadPath)) {
                        existingSize = Files.size(downloadPath);
                        if (listener != null) {
                            listener.onMessageReceived("Resuming download of " + fileName + " from " + existingSize + " bytes.");
                        }
                    }
                    
                    out.println(command + " " + existingSize);

                    DataInputStream dataIn = new DataInputStream(inStream);
                    String remoteChecksum = dataIn.readUTF();
                    long remainingSize = dataIn.readLong();

                    if (remainingSize == -1) {
                        if (listener != null) {
                            listener.onMessageReceived("File not found on peer.");
                        }
                        return;
                    }
                    if (remainingSize == 0) {
                        if (listener != null) {
                            listener.onMessageReceived("File already fully downloaded: " + fileName);
                        }
                        return;
                    }

                    try (RandomAccessFile fileOut = new RandomAccessFile(downloadPath.toFile(), "rw")) {
                        fileOut.seek(existingSize);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long totalRead = existingSize;
                        int bytesRead;
                        long bytesToRead = remainingSize;
                        
                        while (bytesToRead > 0 && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            bytesToRead -= bytesRead;
                            
                            // Report progress to the GUI
                            if (listener != null) {
                                long totalFileSize = existingSize + remainingSize;
                                listener.onDownloadProgress(fileName, totalFileSize, totalRead);
                            }
                        }
                        
                        // Verify checksum after download
                        String localChecksum = getFileChecksum(downloadPath.toFile());
                        if (localChecksum.equals(remoteChecksum)) {
                            if (listener != null) {
                                listener.onMessageReceived("File downloaded successfully: " + fileName);
                            }
                        } else {
                            if (listener != null) {
                                listener.onMessageReceived("File download failed! Checksum mismatch.");
                                listener.onMessageReceived("Local: " + localChecksum);
                                listener.onMessageReceived("Remote: " + remoteChecksum);
                            }
                        }
                    } catch (NoSuchAlgorithmException e) {
                        if (listener != null) {
                            listener.onMessageReceived("Checksum verification failed: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                if (listener != null) {
                    listener.onMessageReceived("Connection to " + host + ":" + port + " failed: " + e.getMessage());
                }
            }
        }
    }

    // Main method to launch the GUI
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PeerGUI <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Peer peer = new Peer(port);
        SwingUtilities.invokeLater(() -> {
            PeerGUI gui = new PeerGUI(peer, port);
            peer.setPeerListener(gui);
            gui.setVisible(true);
        });
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(peer::shutdown));
    }
}