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

public class Peer {
    private static final String CONFIG_FILE = "Peer2Peer" + File.separator + "config.txt";
    private static final int MAX_INTERVAL = 30;

    private Map<Integer, PeerInfo> peers;
    private List<String> peerIps;
    private List<Integer> peerPorts;
    private Map<Integer, Long> alivePeers;
    private List<Integer> deadPeers;

    private int port;

    private List<String> fileListing;

    private static final SecureRandom rand = new SecureRandom();
    private int peerId;

    public Peer(int port, List<String> fileListing) throws SocketException {
        this.peers = new ConcurrentHashMap<>();
        this.peerId = port * 2;
        this.peerIps = new ArrayList<>();
        this.peerPorts = new ArrayList<>();
        this.alivePeers = new ConcurrentHashMap<>();
        this.deadPeers = new ArrayList<>();
        this.fileListing = fileListing;
        this.port = port;
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
                syncPrint("From " + this.port + " Added peer: " + ip + ":" + port);
                //System.out.println();
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
                        alivePeers.put(Integer.parseInt(p.getNodeId()), System.currentTimeMillis());

                        handleAlive(Integer.parseInt(p.getNodeId()));
                        PeerInfo newPeer = new PeerInfo(p.getMode(), p.getStatus(),
                                p.getFileList(), address.getHostAddress());

                        peers.put(Integer.parseInt(p.getNodeId()), newPeer);

                        //syncPrint("Port " + this.port + " received: " + p);
                        //syncPrint("From " + this.port + " peers: " + peers.toString());

                        // System.out.println("Port " + this.port + " received: " + p);
                        // System.out.println("From " + this.port + " peers: " + peers.toString());

                        displayPeers();

                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timed out, no response in 30 seconds");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
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
                        syncPrint("Sent: packet from " + this.port + ":" + peerId + " to " + peerIps.get(counter) + ":"
                                        + peerPorts.get(counter));
                        /*System.out.println(
                                "Sent: packet from " + this.port + ":" + peerId + " to " + peerIps.get(counter) + ":"
                                        + peerPorts.get(counter));*/
                        packetSent = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!packetSent) {
                    System.out.println("Failed to send packet");
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

        }, 0, rand.nextInt(MAX_INTERVAL), TimeUnit.SECONDS);
    }

    private void handleAlive(int peerId) {
        if (deadPeers.contains(peerId)) {
            syncPrint("Peer " + peerId + " is back online");
            //System.out.println("Peer " + peerId + " is back online");
            deadPeers.remove(peerId);
        } else {
            syncPrint("Peer " + peerId + " is still online");
            //System.out.println("Peer " + peerId + " is still online");
        }
    }

    private void checkAlive() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            long currTime = System.currentTimeMillis();

            alivePeers.forEach((peerId, lastAlive) -> {
                if (currTime - lastAlive > 30000) {
                    syncPrint("Peer " + peerId + " is dead");
                    //System.out.println("Peer " + peerId + " is dead");
                    deadPeers.add(peerId);
                    alivePeers.remove(peerId);
                    peers.remove(peerId);
                }
            });

        }, 0, MAX_INTERVAL, TimeUnit.SECONDS);
    }

    private static final Object PRINT_LOCK = new Object();

    public void displayPeers() {
        synchronized (PRINT_LOCK) {
            if (peers.isEmpty()) {
                System.out.println("No peers currently connected.");
                return;
            }

            System.out.println("\n====== PEER NETWORK STATUS (Port: " + this.port + ") ======");
            System.out.println("ID\tIP Address\tMode\tStatus\tFiles");
            System.out.println("----------------------------------------");

            for (Map.Entry<Integer, PeerInfo> entry : peers.entrySet()) {
                Integer id = entry.getKey();
                PeerInfo info = entry.getValue();

                String fileList = String.join(", ", info.getFileList());
                if (fileList.length() > 40) {
                    fileList = fileList.substring(0, 37) + "...";
                }

                System.out.println(id + "\t" +
                        info.getIp() + "\t" +
                        info.getMode() + "\t" +
                        info.getStatus() + "\t" +
                        fileList);
            }
            System.out.println();
        }
    }

    private void syncPrint(String message) {
        synchronized(PRINT_LOCK) {
            System.out.println("[Port " + this.port + "] " + message);
        }
    }
}
