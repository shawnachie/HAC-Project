package ServerClient.src;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UDPClient {
    private DatagramSocket datagramSocket;
    private InetAddress serverAddress;
    private SecureRandom secureRandom = new SecureRandom();
    private static final String PROJECT_HOME = "ServerClient/home";
    private String nodeId;
    private String mode;
    private List<String> nodeIps;
    private int udpPort;
    private static final String SEPARATOR = "----------------------------------------";

    // Constructor to initialize the datagram socket and server address
    public UDPClient(DatagramSocket datagramSocket, InetAddress serverAddress, String nodeId, String mode, List<String> nodeIps, int udpPort) {
        this.datagramSocket = datagramSocket;
        this.serverAddress = serverAddress;
        this.nodeId = nodeId;
        this.mode = mode;
        this.nodeIps = nodeIps;
        this.udpPort = udpPort;
        
        // Create project home directory if it doesn't exist
        File homeDir = new File(PROJECT_HOME);
        if (!homeDir.exists()) {
            if (homeDir.mkdirs()) {
                System.out.println("[Client] Created project home directory: " + PROJECT_HOME);
            } else {
                System.err.println("[Client Error] Failed to create project home directory: " + PROJECT_HOME);
            }
        }
    }

    // Method to retrieve the list of files from the project home directory
    private List<String> getFileNames() {
        File directory = new File(PROJECT_HOME);
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("[Client Error] Directory does not exist: " + PROJECT_HOME);
            return List.of();
        }
        String[] files = directory.list((dir, name) -> new File(dir, name).isFile());
        return (files != null) ? Arrays.asList(files) : List.of();
    }

    // Method to send status and timestamp to the server
    public void sendStatus() {
        while (true) {
            try {
                // Prepare status message
                String status = "alive";
                long timestamp = System.currentTimeMillis();
                List<String> fileNames = getFileNames();

                // Create and send packet
                Packet packet = new Packet(mode, nodeId, status, fileNames);
                byte[] buffer = packet.encode();
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, serverAddress, udpPort);

                System.out.println(SEPARATOR);
                System.out.println("[Client] Status Update");
                System.out.println("[Client] Node: " + nodeId);
                System.out.println("[Client] Files available:");
                if (fileNames.isEmpty()) {
                    System.out.println("[Client] No files found");
                } else {
                    fileNames.forEach(file -> System.out.println("[Client]  - " + file));
                }
                System.out.println(SEPARATOR);

                datagramSocket.send(datagramPacket);

                // Wait for a random delay between 0 and 30 seconds
                long delay = secureRandom.nextInt(31); // 0 to 30 inclusive
                TimeUnit.SECONDS.sleep(delay);
            } catch (IOException | InterruptedException e) {
                System.err.println("[Client Error] " + e.getMessage());
                break;
            }
        }
    }

    // Method to listen for server status messages
    public void listenForServerStatus() {
        boolean headerPrinted = false;
        while (true) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);
                
                String serverMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

                if (serverMessage.startsWith("Active Client statuses:")) {
                    System.out.println(SEPARATOR);
                    System.out.println("[Server Update] Client Status Report");
                    String[] clientStatuses = serverMessage.split("\n");
                    for (String clientStatus : clientStatuses) {
                        if (!clientStatus.equals("Active Client statuses:")) {
                            System.out.println(clientStatus);
                        }
                    }
                    System.out.println(SEPARATOR);
                }
            } catch (IOException e) {
                System.err.println("[Client Error] " + e.getMessage());
                break;
            }
        }
    }

    // Method to get the local IP address of the device
    private static InetAddress getLocalIpAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                    return inetAddress;
                }
            }
        }
        throw new SocketException("No suitable local IP address found");
    }

    // Main method to start the client
    public static void main(String[] args) throws Exception {
        // Load configuration
        ConfigLoader config = new ConfigLoader("ServerClient/clientconfig.properties");
        String nodeId = config.getString("node.id");
        String mode = config.getString("node.mode");
        List<String> nodeIps = config.getList("node.ips");
        int udpPort = config.getInt("udp.port");

        // Validate configuration
        if (udpPort == 0) {
            System.err.println("Invalid UDP port: " + udpPort);
            return;
        }

        // Get the server IP address from the configuration
        InetAddress serverAddress = InetAddress.getByName(nodeIps.get(0));

        // Create a datagram socket
        DatagramSocket datagramSocket = new DatagramSocket();

        // Get the local IP address of the device
        InetAddress inetAddress = getLocalIpAddress();
        UDPClient client = new UDPClient(datagramSocket, serverAddress, nodeId, mode, nodeIps, udpPort);
        System.out.println("Client has started with IP address: " + inetAddress.getHostAddress());

        // Thread to listen for server status messages
        new Thread(() -> {
            client.listenForServerStatus();
        }).start();

        // Thread to send status messages
        new Thread(() -> {
            client.sendStatus();
        }).start();
    }
}