package Peer2Peer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Peer {
    private static final String CONFIG_FILE = "Peer2Peer" + File.separator + "config.txt";
    private static final int MAX_INTERVAL = 30;

    private Map<Integer, PeerInfo> peers;
    private List<String> peerIps;
    private List<Integer> peerPorts;
    private Map<Integer, Long> alivePeers;
    private List<Integer> deadPeers;

    private int port;
    private int peerId;
    private List<String> fileListing;

    // Logger for this peer
    private Logger logger;

    private static final SecureRandom rand = new SecureRandom();

    public Peer(int port, List<String> fileListing) throws SocketException {
        this.peers = new ConcurrentHashMap<>();
        this.peerId = port * 2;
        this.peerIps = new ArrayList<>();
        this.peerPorts = new ArrayList<>();
        this.alivePeers = new ConcurrentHashMap<>();
        this.deadPeers = new ArrayList<>();
        this.fileListing = fileListing;
        this.port = port;
        
        // Set up logging
        setupLogger();
    }

    /**
     * Set up the logger for this peer
     */
    private void setupLogger() {
        try {
            // Create a logger with the peer's port number
            logger = Logger.getLogger("Peer-" + port);
            
            // Remove default handlers to avoid duplicate logging
            logger.setUseParentHandlers(false);
            
            // Create a file handler for this peer's log file
            FileHandler fileHandler = new FileHandler("peer_" + port + ".log", true);
            
            // Use a custom formatter for better log readability
            fileHandler.setFormatter(new PeerLogFormatter());
            logger.addHandler(fileHandler);
            
            // Also log to console with peer identification
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new PeerConsoleFormatter(port));
            logger.addHandler(consoleHandler);
            
            // Set the log level
            logger.setLevel(Level.INFO);
            
            logger.info("Logger initialized for peer on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to set up logger: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Custom formatter for log files
     */
    private static class PeerLogFormatter extends Formatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date(record.getMillis()))).append(" ");
            sb.append("[").append(record.getLevel()).append("] ");
            sb.append(record.getMessage()).append("\n");
            return sb.toString();
        }
    }
    
    /**
     * Custom formatter for console output that includes peer port
     */
    private static class PeerConsoleFormatter extends Formatter {
        private final int port;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        public PeerConsoleFormatter(int port) {
            this.port = port;
        }
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Port ").append(port).append("] ");
            sb.append(dateFormat.format(new Date(record.getMillis()))).append(" ");
            sb.append(record.getMessage()).append("\n");
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Peer <port> <file1> <file2> ...");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            List<String> fileListing = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));

            Peer network = new Peer(port, fileListing);
            network.loadConfig();
            network.startListening();
            network.startSending();
            network.checkAlive();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                peerIps.add(ip);
                peerPorts.add(port);
                logger.info("Added peer: " + ip + ":" + port);
            }
        }
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                socket.setSoTimeout(30000);

                byte[] buffer = new byte[4096];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        
                        InetAddress address = packet.getAddress();

                        Packet p = Packet.decode(packet.getData());
                        logger.fine("Updated alive timestamp for peer " + p.getNodeId());
                        alivePeers.put(Integer.parseInt(p.getNodeId()), System.currentTimeMillis());

                        handleAlive(Integer.parseInt(p.getNodeId()));
                        PeerInfo newPeer = new PeerInfo(p.getMode(), p.getStatus(),
                                p.getFileList(), address.getHostAddress());

                        peers.put(Integer.parseInt(p.getNodeId()), newPeer);

                        // Log at FINE level to reduce console clutter
                        logger.fine("Received: " + p);

                        displayPeers();

                    } catch (SocketTimeoutException e) {
                        logger.warning("Socket timed out, no response in 30 seconds");
                    } catch (IOException e) {
                        logger.severe("Error receiving packet: " + e.getMessage());
                    }
                }
            } catch (SocketException e) {
                logger.severe("Socket error: " + e.getMessage());
            }
        });
        listenerThread.start();
    }

    private void startSending() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                boolean packetSent = false;

                for (int counter = 0; counter < peerIps.size(); counter++) {
                    try {
                        Packet p = new Packet("P2P", String.valueOf(peerId), "ALIVE", fileListing);
                        byte[] buffer = p.encode();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                InetAddress.getByName(peerIps.get(counter)), peerPorts.get(counter));
                        socket.send(packet);
                        logger.info("Sent packet to " + peerIps.get(counter) + ":" + peerPorts.get(counter));
                        packetSent = true;
                    } catch (IOException e) {
                        logger.warning("Failed to send packet to " + peerIps.get(counter) + ":" + 
                                      peerPorts.get(counter) + " - " + e.getMessage());
                    }
                }

                if (!packetSent) {
                    logger.warning("Failed to send any packets");
                }
            } catch (SocketException e) {
                logger.severe("Socket error: " + e.getMessage());
            }
        }, 0, rand.nextInt(MAX_INTERVAL), TimeUnit.SECONDS);
    }

    private void handleAlive(int peerId) {
        if (deadPeers.contains(peerId)) {
            logger.info("Peer " + peerId + " is back online");
            deadPeers.remove(Integer.valueOf(peerId));
        } else {
            logger.fine("Peer " + peerId + " is still online");
        }
    }

    private void checkAlive() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        logger.info("Running alive check at " + new Date());
    
        executor.scheduleAtFixedRate(() -> {
            long currTime = System.currentTimeMillis();
            List<Integer> peersToRemove = new ArrayList<>();
    
            alivePeers.forEach((peerId, lastAlive) -> {
                if (currTime - lastAlive > 30000) {
                    logger.info("Peer " + peerId + " is dead");
                    deadPeers.add(peerId);
                    peersToRemove.add(peerId);
                }
            });
    
            peersToRemove.forEach(peerId -> {
                alivePeers.remove(peerId);
                peers.remove(peerId);
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void displayPeers() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====== PEER NETWORK STATUS (Port: ").append(this.port).append(") ======\n");
        
        if (peers.isEmpty()) {
            sb.append("No peers currently connected.\n");
            logger.info(sb.toString());
            return;
        }
        
        sb.append("ID\tIP Address\tMode\tStatus\tFiles\n");
        sb.append("----------------------------------------\n");
        
        for (Map.Entry<Integer, PeerInfo> entry : peers.entrySet()) {
            Integer id = entry.getKey();
            PeerInfo info = entry.getValue();
            
            String fileList = String.join(", ", info.getFileList());
            if (fileList.length() > 40) {
                fileList = fileList.substring(0, 37) + "...";
            }
            
            sb.append(id).append("\t")
              .append(info.getIp()).append("\t")
              .append(info.getMode()).append("\t")
              .append(info.getStatus()).append("\t")
              .append(fileList).append("\n");
        }
        
        logger.info(sb.toString());
    }
}