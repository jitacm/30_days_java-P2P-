import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class PeerGUI extends JFrame implements Peer.PeerListener {

    private final Peer peer;
    private JTextArea logArea;
    private JTextField hostField;
    private JTextField portField;
    private JTextField searchField;
    private DefaultListModel<String> searchResultsModel;
    private JList<String> searchResultsList;
    private JProgressBar progressBar;
    private DefaultListModel<String> peerStatusListModel;
    private JTable transferHistoryTable;
    private DefaultTableModel transferHistoryModel;

    public PeerGUI(Peer peer, int port) {
        this.peer = peer;

        setTitle("P2P File Sharer - Port " + port);
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ==== TOP CONTROL PANEL ====
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(5, 1, 5, 5));
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

        // Shared Directory Selection
        JPanel sharedDirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton chooseDirButton = new JButton("Set Shared Folder");
        chooseDirButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = chooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File selectedDir = chooser.getSelectedFile();
                peer.setSharedDirectory(selectedDir.toPath());
            }
        });
        sharedDirPanel.add(new JLabel("Shared Dir:"));
        sharedDirPanel.add(chooseDirButton);
        controlPanel.add(sharedDirPanel);

        add(controlPanel, BorderLayout.NORTH);

        // ==== CENTER PANEL (Left: search, peers, Right: logs/history) ====
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.4);

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        // Search results list
        searchResultsModel = new DefaultListModel<>();
        searchResultsList = new JList<>(searchResultsModel);
        searchResultsList.setBorder(BorderFactory.createTitledBorder("Search Results (Double click to download)"));
        searchResultsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = searchResultsList.getSelectedValue();
                    if (selected != null && !selected.trim().isEmpty()) {
                        // Extract just the file name from the metadata
                        String fileName = selected.split("\t")[0];
                        peer.download(fileName);
                    }
                }
            }
        });
        leftPanel.add(new JScrollPane(searchResultsList), BorderLayout.CENTER);

        // Peer status list
        peerStatusListModel = new DefaultListModel<>();
        JList<String> peerStatusList = new JList<>(peerStatusListModel);
        peerStatusList.setBorder(BorderFactory.createTitledBorder("Peers Status"));
        leftPanel.add(new JScrollPane(peerStatusList), BorderLayout.SOUTH);

        centerSplit.setLeftComponent(leftPanel);

        // Right panel with Logs and Transfer History tabs
        JTabbedPane rightTabs = new JTabbedPane();

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        rightTabs.addTab("Activity Log", new JScrollPane(logArea));

        // Transfer History Table
        String[] columnNames = {"File", "Type", "Status", "Peer", "Timestamp"};
        transferHistoryModel = new DefaultTableModel(columnNames, 0);
        transferHistoryTable = new JTable(transferHistoryModel);
        rightTabs.addTab("Transfer History", new JScrollPane(transferHistoryTable));

        centerSplit.setRightComponent(rightTabs);
        add(centerSplit, BorderLayout.CENTER);

        // Progress Bar at the bottom
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(progressBar, BorderLayout.SOUTH);

        // Handle graceful shutdown
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                peer.shutdown();
            }
        });
    }

    // ==== PeerListener Implementation ====

    @Override
    public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    @Override
    public void onSearchResults(String host, int port, List<String> results) {
        SwingUtilities.invokeLater(() -> {
            searchResultsModel.clear();
            if (results.isEmpty()) {
                onMessageReceived("No files found on " + host + ":" + port);
            } else {
                onMessageReceived("--- Search results from " + host + ":" + port + " ---");
                for (String item : results) {
                    searchResultsModel.addElement(item);
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

    @Override
    public void onPeerStatusUpdate(Map<String, Boolean> peerStatusMap) {
        SwingUtilities.invokeLater(() -> {
            peerStatusListModel.clear();
            for (Map.Entry<String, Boolean> e : peerStatusMap.entrySet()) {
                String status = e.getKey() + " - " + (e.getValue() ? "Online" : "Offline");
                peerStatusListModel.addElement(status);
            }
        });
    }

    @Override
    public void onTransferHistoryUpdated(List<Peer.TransferRecord> history) {
        SwingUtilities.invokeLater(() -> {
            transferHistoryModel.setRowCount(0);
            for (Peer.TransferRecord record : history) {
                transferHistoryModel.addRow(
                    new Object[]{record.fileName, record.type, record.status, record.peer, record.timestamp});
            }
        });
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PeerGUI <port>");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);
            Peer peer = new Peer(port);
            SwingUtilities.invokeLater(() -> {
                PeerGUI gui = new PeerGUI(peer, port);
                peer.setPeerListener(gui);
                peer.start();
                gui.setVisible(true);
            });
            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(peer::shutdown));
        } catch (NumberFormatException e) {
            System.err.println("Error: Port must be integer.");
        }
    }
}
