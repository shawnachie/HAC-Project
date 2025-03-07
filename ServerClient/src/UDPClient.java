package ServerClient.src;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;

public class UDPClient {
    private DatagramSocket datagramSocket;
    private InetAddress serverAddress;
    private SecureRandom secureRandom = new SecureRandom();
    private static final String HOME_DIRECTORY = System.getProperty("user.home");
    private String nodeId;
    private String mode;
    private List<String> nodeIps;
    private int udpPort;

    // Constructor to initialize the datagram socket and server address
    public UDPClient(DatagramSocket datagramSocket, InetAddress serverAddress, String nodeId, String mode, List<String> nodeIps, int udpPort) {
        this.datagramSocket = datagramSocket;
        this.serverAddress = serverAddress;
        this.nodeId = nodeId;
        this.mode = mode;
        this.nodeIps = nodeIps;
        this.udpPort = udpPort;
    }

    // Method to retrieve the list of files from the home directory
    private List<String> getFileNames() {
        File directory = new File(HOME_DIRECTORY);
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Directory does not exist: " + HOME_DIRECTORY);
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

                // Create a Packet object
                Packet packet = new Packet(mode, nodeId, status, fileNames);

                // Encode the Packet object to a byte array
                byte[] buffer = packet.encode();

                // Create and send UDP packet to the server
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, serverAddress, udpPort);
                System.out.println("Sending packet to server at " + serverAddress.getHostAddress() + ":" + udpPort);

                // Print the files in the desired format
                System.out.println("Files:");
                for (String file : fileNames) {
                    System.out.println(" - " + file);
                }

                datagramSocket.send(datagramPacket);

                // Wait for a random delay before sending the next status
                long delay = secureRandom.nextInt(30) + 1;
                TimeUnit.SECONDS.sleep(delay);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    // Method to listen for server status messages
    public void listenForServerStatus() {
        boolean headerPrinted = false; // Flag to check if the header has been printed
        while (true) {
            try {
                // Prepare buffer to receive data
                byte[] buffer = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                // Receive packet from the server
                datagramSocket.receive(datagramPacket);
                String serverMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                InetAddress serverAddress = datagramPacket.getAddress();
                int serverPort = datagramPacket.getPort();

                // Process the received message
                if (serverMessage.startsWith("Active Client statuses:")) {
                    if (!headerPrinted) {
                        System.out.println("Active Client statuses:");
                        headerPrinted = true;
                    }
                    String[] clientStatuses = serverMessage.split("\n");
                    for (String clientStatus : clientStatuses) {
                        if (!clientStatus.equals("Active Client statuses:")) {
                            System.out.println(clientStatus);
                        }
                    }
                } else {
                    System.out.println("Server Received from " + serverAddress + ":" + serverPort + " - " + serverMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
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