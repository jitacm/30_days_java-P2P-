import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class PeerGUI extends JFrame implements Peer.PeerListener {

    private Peer peer;
    private JTextArea logArea;
    private JTextField hostField;
    private JTextField portField;
    private JTextField searchField;
    private JTextField downloadField;

    public PeerGUI(Peer peer, int port) {
        this.peer = peer;
        this.peer.setPeerListener(this);
        this.peer.start();

        setTitle("P2P File Sharer - Port " + port);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel for Inputs and Controls
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(4, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Connect Section
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hostField = new JTextField("localhost", 15);
        portField = new JTextField("9001", 5);
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            try {
                String host = hostField.getText();
                int connPort = Integer.parseInt(portField.getText());
                peer.connect(host, connPort);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        connectPanel.add(new JLabel("Connect to: "));
        connectPanel.add(hostField);
        connectPanel.add(portField);
        connectPanel.add(connectButton);
        controlPanel.add(connectPanel);

        // Discovery Section
        JPanel discoverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton discoverButton = new JButton("Discover Peers");
        discoverButton.addActionListener(e -> peer.discoverPeers());
        discoverPanel.add(discoverButton);
        controlPanel.add(discoverPanel);

        // Search Section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText();
            if (!keyword.isEmpty()) {
                peer.search(keyword);
            }
        });
        searchPanel.add(new JLabel("Search File: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        controlPanel.add(searchPanel);

        // Download Section
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        downloadField = new JTextField(20);
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> {
            String fileName = downloadField.getText();
            if (!fileName.isEmpty()) {
                peer.download(fileName);
            }
        });
        downloadPanel.add(new JLabel("Download File: "));
        downloadPanel.add(downloadField);
        downloadPanel.add(downloadButton);
        controlPanel.add(downloadPanel);

        add(controlPanel, BorderLayout.NORTH);

        // Log Area for Status Messages and Results
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // Handle window closing gracefully
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                peer.shutdown();
            }
        });
    }

    // PeerListener implementation for receiving messages from the Peer class
    @Override
    public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    @Override
    public void onSearchResults(String host, int port, List<String> results) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("--- Search results from " + host + ":" + port + " ---\n");
            if (results.isEmpty()) {
                logArea.append("No files found.\n");
            } else {
                for (String file : results) {
                    logArea.append("  - " + file + "\n");
                }
            }
        });
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PeerGUI <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Peer peer = new Peer(port);
        SwingUtilities.invokeLater(() -> new PeerGUI(peer, port).setVisible(true));
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(peer::shutdown));
    }
}

// PeerDiscoveryService is a new file that must be compiled with Peer.java and PeerGUI.java
// For this example, we will assume it is available.