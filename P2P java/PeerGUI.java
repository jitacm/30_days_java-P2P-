import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class PeerGUI extends JFrame implements Peer.PeerListener {

    private Peer peer;
    private JTextArea logArea;
    private JTextField hostField;
    private JTextField portField;
    private JTextField searchField;
    private DefaultListModel<String> searchResultsModel;
    private JList<String> searchResultsList;
    private JProgressBar progressBar;

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
        
        add(controlPanel, BorderLayout.NORTH);

        // Center panel for search results and log
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Search Results List
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setBorder(BorderFactory.createTitledBorder("Search Results (Double click to download)"));
        searchResultsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedFile = searchResultsList.getSelectedValue();
                    if (selectedFile != null) {
                        peer.download(selectedFile);
                    }
                }
            }
        });
        centerPanel.add(new JScrollPane(searchResultsList));

        // Log Area for Status Messages
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        centerPanel.add(new JScrollPane(logArea));

        add(centerPanel, BorderLayout.CENTER);

        // Progress Bar at the bottom
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(progressBar, BorderLayout.SOUTH);

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
            // Clear existing results and add new ones
            searchResultsModel.clear();
            if (results.isEmpty()) {
                onMessageReceived("No files found on " + host + ":" + port);
            } else {
                onMessageReceived("--- Search results from " + host + ":" + port + " ---");
                for (String file : results) {
                    searchResultsModel.addElement(file);
                }
            }
        });
    }
    
    @Override
    public void onDownloadProgress(String fileName, long totalBytes, long downloadedBytes) {
        SwingUtilities.invokeLater(() -> {
            int progress = (int) ((downloadedBytes * 100) / totalBytes);
            progressBar.setValue(progress);
            progressBar.setString(fileName + " " + progress + "%");
        });
    }

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
        
        Runtime.getRuntime().addShutdownHook(new Thread(peer::shutdown));
    }
}