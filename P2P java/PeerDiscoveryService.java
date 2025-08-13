import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerDiscoveryService {

    private static final int BROADCAST_PORT = 9876;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final int DISCOVERY_INTERVAL_SECONDS = 30;

    private int myPort;
    private final Set<String> discoveredPeers = Collections.synchronizedSet(new HashSet<>());
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private DatagramSocket socket;

    public PeerDiscoveryService(int myPort) {
        this.myPort = myPort;
    }

    public void start() {
        try {
            socket = new DatagramSocket(BROADCAST_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            new Thread(this::listenForPeers).start();
            scheduler.scheduleAtFixedRate(this::broadcastPresence, 0, DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } catch (IOException e) {
            System.err.println("Error starting peer discovery: " + e.getMessage());
        }
    }

    private void broadcastPresence() {
        try {
            String message = "PEER:" + myPort;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_ADDRESS), BROADCAST_PORT);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error broadcasting presence: " + e.getMessage());
        }
    }

    private void listenForPeers() {
        try {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(":");
                if (parts.length == 2 && parts[0].equals("PEER")) {
                    String peerAddress = packet.getAddress().getHostAddress();
                    int peerPort = Integer.parseInt(parts[1]);

                    // Avoid adding self
                    if (!peerAddress.equals(InetAddress.getLocalHost().getHostAddress()) || peerPort != myPort) {
                        discoveredPeers.add(peerAddress + ":" + peerPort);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error listening for peers: " + e.getMessage());
        }
    }

    public Set<String> getDiscoveredPeers() {
        return discoveredPeers;
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}