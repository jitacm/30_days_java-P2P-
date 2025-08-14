// --- full revised Peer.java with enhancements ---

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Peer {

    public interface PeerListener {
        void onMessageReceived(String message);
        void onSearchResults(String host, int port, List<String> results);
        void onDownloadProgress(String fileName, long totalBytes, long downloadedBytes);
        void onPeerStatusUpdate(Map<String, Boolean> peerStatusMap);
        void onTransferHistoryUpdated(List<TransferRecord> history);
    }

    public static class TransferRecord {
        public final String fileName;
        public final String type; // "UPLOAD" or "DOWNLOAD"
        public final String status; // "SUCCESS", "FAILED", etc.
        public final String timestamp;
        public final String peer;

        public TransferRecord(String fileName, String type, String status, String peer) {
            this.fileName = fileName;
            this.type = type;
            this.status = status;
            this.peer = peer;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }
    }

    private static final int BUFFER_SIZE = 4096;
    private static final String DEFAULT_SHARED_DIR = "shared";
    private static final String DOWNLOAD_DIR = "downloads";
    private static final String KEY_STORE_PATH = "keystore.jks";
    private static final String TRUST_STORE_PATH = "truststore.jks";
    private static final String STORE_PASSWORD = "password";

    private Path sharedDirPath;
    private int port;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private List<ConnectionHandler> connections = new ArrayList<>();
    private PeerDiscoveryService discoveryService;
    private PeerListener listener;
    private SSLContext sslContext;
    private final Map<String, Integer> connectionFailures = new ConcurrentHashMap<>();
    private final Map<String, Boolean> peerStatusMap = new ConcurrentHashMap<>();
    private final List<TransferRecord> transferHistory = Collections.synchronizedList(new ArrayList<>());

    public Peer(int port) {
        this.port = port;
        this.discoveryService = new PeerDiscoveryService(port);
        try {
            sharedDirPath = Paths.get(DEFAULT_SHARED_DIR);
            Path downloadDirPath = Paths.get(DOWNLOAD_DIR);
            if (Files.notExists(sharedDirPath)) {
                Files.createDirectories(sharedDirPath);
            }
            if (Files.notExists(downloadDirPath)) {
                Files.createDirectories(downloadDirPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
        try {
            this.sslContext = createSSLContext();
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Failed to create SSL Context: " + e.getMessage());
        }
    }

    public void setPeerListener(PeerListener listener) {
        this.listener = listener;
    }

    public void start() {
        discoveryService.start();
        new Thread(this::startServer).start();
    }

    public void setSharedDirectory(Path newDir) {
        try {
            if (!Files.exists(newDir)) {
                Files.createDirectories(newDir);
            }
            this.sharedDirPath = newDir;
            if (listener != null) {
                listener.onMessageReceived("Shared directory set to: " + newDir.toAbsolutePath());
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onMessageReceived("Failed to set shared directory: " + e.getMessage());
            }
        }
    }

    private SSLContext createSSLContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreStream = new FileInputStream(KEY_STORE_PATH)) {
            keyStore.load(keyStoreStream, STORE_PASSWORD.toCharArray());
        }
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStoreStream = new FileInputStream(TRUST_STORE_PATH)) {
            trustStore.load(trustStoreStream, STORE_PASSWORD.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, STORE_PASSWORD.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return context;
    }

    private void startServer() {
        try {
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port)) {
                if (listener != null) {
                    listener.onMessageReceived("Listening for peers securely on port " + port + "...");
                }
                while (true) {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    threadPool.execute(() -> handleClient(clientSocket));
                }
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onMessageReceived("Server error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream outStream = socket.getOutputStream();
        ) {
            String command = in.readLine();
            if (command.startsWith("search")) {
                handleSearch(command, outStream);
            } else if (command.startsWith("download")) {
                handleDownload(command, outStream, socket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onMessageReceived("Client handling error: " + e.getMessage());
            }
        }
    }

    private void handleSearch(String command, OutputStream outStream) throws IOException {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) return;
        String keyword = parts[1].trim();
        PrintWriter out = new PrintWriter(outStream, true);
        try {
            Files.walk(sharedDirPath)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    boolean match = matchesPattern(fileName, keyword);
                    if (match) {
                        try {
                            long size = Files.size(p);
                            String modDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Files.getLastModifiedTime(p).toMillis()));
                            out.println(fileName + "\t" + size + "\t" + modDate);
                        } catch (IOException ignored) {}
                    }
                });
        } catch (IOException ignored) {}
        out.println("END");
    }

    private boolean matchesPattern(String text, String pattern) {
        try {
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", ".*");
                return text.matches("(?i)" + regex);
            } else {
                // regex search if pattern looks regexy, else substring match
                if (pattern.startsWith("regex:")) {
                    return text.matches(pattern.substring(6));
                }
                return text.toLowerCase().contains(pattern.toLowerCase());
            }
        } catch (Exception e) {
            return text.toLowerCase().contains(pattern.toLowerCase());
        }
    }

    private void handleDownload(String command, OutputStream outStream, String peerAddr) throws IOException {
        String[] parts = command.split(" ");
        if (parts.length < 3) return;
        String fileName = parts[1];
        long offset = Long.parseLong(parts[2]);
        Path filePath = sharedDirPath.resolve(fileName);

        DataOutputStream dataOut = new DataOutputStream(outStream);

        if (Files.notExists(filePath)) {
            dataOut.writeUTF("NOCHECKSUM");
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
            try (RandomAccessFile fileIn = new RandomAccessFile(filePath.toFile(), "r")) {
                fileIn.seek(offset);
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            }
            recordTransfer(new TransferRecord(fileName, "UPLOAD", "SUCCESS", peerAddr));
        } catch (NoSuchAlgorithmException e) {
            dataOut.writeUTF("NOCHECKSUM");
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
        for (byte b : hashedBytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public void connect(String host, int port) {
        connections.add(new ConnectionHandler(host, port));
        updatePeerStatus(host + ":" + port, true);
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
        if (connections.isEmpty()) {
            if (listener != null) {
                listener.onMessageReceived("No active connections. Use 'connect' or 'discover' first.");
            }
            return;
        }
        for (ConnectionHandler conn : connections) {
            conn.sendCommand("download " + fileName);
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

    private void updatePeerStatus(String peer, boolean online) {
        peerStatusMap.put(peer, online);
        if (listener != null) {
            listener.onPeerStatusUpdate(new HashMap<>(peerStatusMap));
        }
    }

    private void recordTransfer(TransferRecord record) {
        transferHistory.add(record);
        if (listener != null) {
            listener.onTransferHistoryUpdated(new ArrayList<>(transferHistory));
        }
    }

    private class ConnectionHandler {
        private final String host;
        private final int port;

        public ConnectionHandler(String host, int port) {
            this.host = host; this.port = port;
        }

        public void sendCommand(String command) {
            try {
                SSLSocketFactory ssf = sslContext.getSocketFactory();
                try (SSLSocket socket = (SSLSocket) ssf.createSocket(host, port)) {
                    socket.startHandshake();
                    try (
                        InputStream inStream = socket.getInputStream();
                        OutputStream outStream = socket.getOutputStream();
                        PrintWriter out = new PrintWriter(outStream, true);
                    ) {
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
                            long existingSize = Files.exists(downloadPath) ? Files.size(downloadPath) : 0;
                            out.println(command + " " + existingSize);
                            DataInputStream dataIn = new DataInputStream(inStream);
                            String remoteChecksum = dataIn.readUTF();
                            long remainingSize = dataIn.readLong();
                            if (remainingSize == -1) {
                                if (listener != null) listener.onMessageReceived("File not found on peer.");
                                return;
                            }
                            if (remainingSize == 0) {
                                if (listener != null) listener.onMessageReceived("File already fully downloaded: " + fileName);
                                return;
                            }
                            try (RandomAccessFile fileOut = new RandomAccessFile(downloadPath.toFile(), "rw")) {
                                fileOut.seek(existingSize);
                                byte[] buffer = new byte[BUFFER_SIZE];
                                long totalRead = existingSize;
                                long bytesToRead = remainingSize;
                                int bytesRead;
                                while (bytesToRead > 0 && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                                    fileOut.write(buffer, 0, bytesRead);
                                    totalRead += bytesRead;
                                    bytesToRead -= bytesRead;
                                    if (listener != null) {
                                        long totalFileSize = existingSize + remainingSize;
                                        listener.onDownloadProgress(fileName, totalFileSize, totalRead);
                                    }
                                }
                                String localChecksum = getFileChecksum(downloadPath.toFile());
                                if (!"NOCHECKSUM".equals(remoteChecksum) && localChecksum.equals(remoteChecksum)) {
                                    listener.onMessageReceived("File downloaded successfully: " + fileName);
                                    recordTransfer(new TransferRecord(fileName, "DOWNLOAD", "SUCCESS", host+":"+port));
                                } else {
                                    listener.onMessageReceived("Checksum mismatch for: " + fileName);
                                    recordTransfer(new TransferRecord(fileName, "DOWNLOAD", "FAILED", host+":"+port));
                                }
                            }
                        }
                    }
                }
                updatePeerStatus(host + ":" + port, true);
            } catch (IOException e) {
                connectionFailures.merge(host + ":" + port, 1, Integer::sum);
                if (connectionFailures.get(host + ":" + port) >= 3) {
                    updatePeerStatus(host + ":" + port, false);
                }
                if (listener != null) {
                    listener.onMessageReceived("Connection to " + host + ":" + port + " failed: " + e.getMessage());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onMessageReceived("Error talking to peer " + host + ":" + port + ": " + e.getMessage());
                }
            }
        }
    }
}
