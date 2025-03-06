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
    private InetAddress inetAddress;
    private SecureRandom secureRandom = new SecureRandom();
    private static final String HOME_DIRECTORY = System.getProperty("user.home");

    // Constructor to initialize the datagram socket and server address
    public UDPClient(DatagramSocket datagramSocket, InetAddress inetAddress) {
        this.datagramSocket = datagramSocket;
        this.inetAddress = inetAddress;
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
                String messageToSend = "Status:" + status + ",Timestamp:" + timestamp + ",Files:" + String.join(",", fileNames);
                byte[] buffer = messageToSend.getBytes();

                // Create and send UDP packet to the server
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, 9876);
                System.out.println("Sending packet to server.");
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
                if (serverMessage.startsWith("Client statuses:")) {
                    
                    String[] clientStatuses = serverMessage.split("\n");
                    for (String clientStatus : clientStatuses) {
                        System.out.println(clientStatus);
                    }
                } else {
                    System.out.println("Server Received Packet from " + serverAddress + ":" + serverPort + " - " + serverMessage);
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
        // Create a datagram socket
        DatagramSocket datagramSocket = new DatagramSocket();

        // Get the local IP address of the device
        InetAddress inetAddress = getLocalIpAddress();
        UDPClient client = new UDPClient(datagramSocket, inetAddress);
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