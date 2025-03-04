import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;

public class UDPClient
{
    private DatagramSocket datagramSocket;
    private InetAddress inetAddress;
    private byte[] buffer = new byte[1024];
    private SecureRandom secureRandom = new SecureRandom();
    private static final String HOME_DIRECTORY = System.getProperty("user.home");

    // Constructor to initialize the datagram socket and inet address
    public UDPClient(DatagramSocket datagramSocket, InetAddress inetAddress)
    {
        this.datagramSocket = datagramSocket;
        this.inetAddress = inetAddress;
    }

    // Method to retrieve the list of files from the home directory
    private List<String> getFileNames()
    {
        // Define the home directory
        File directory = new File(HOME_DIRECTORY);
        
        // Check if the directory exists and is a directory
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Directory does not exist: " + HOME_DIRECTORY);
            return List.of();
        }

        // List the files in the directory (excluding subfolders)
        String[] files = directory.list((dir, name) -> new File(dir, name).isFile());
        
        // Return the list of files as a List<String>
        return (files != null) ? Arrays.asList(files) : List.of();
    }
    
    // Method to send status and timestamp to the server
    public void sendStatus()
    {
        while (true)
        {
            try
            {
                // Generate the status and timestamp
                String status = "alive";
                long timestamp = System.currentTimeMillis();
                
                // Retrieve the list of files from the home directory
                List<String> fileNames = getFileNames();
                
                // Create a message to send to the server, including status, timestamp, and file names
                String messageToSend = "Status:" + status + ",Timestamp:" + timestamp + ",Files:" + String.join(",", fileNames);
                
                // Convert the message to byte array
                buffer = messageToSend.getBytes();
                
                // Create a datagram packet to send to the server
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, 9876);
                
                // Send the packet to the server
                datagramSocket.send(datagramPacket);
                
                // Sleep for a while before sending the next packet
                long delay = secureRandom.nextInt(30) + 1; // Random delay between 1 and 30 seconds
                TimeUnit.SECONDS.sleep(delay);
            }
            catch (IOException | InterruptedException e)
            {
                // Print the stack trace if an exception occurs
                e.printStackTrace();
                break;
            }
        }
    }

    // Method to listen for server status messages
    public void listenForServerStatus()
    {
        while (true)
        {
            try
            {
                // Create a new datagram packet to receive the response from the server
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);
                String serverMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                if (serverMessage.startsWith("Client statuses:")) {
                    System.out.println("Active Clients:");
                    String[] clientStatuses = serverMessage.split("\n");
                    for (String clientStatus : clientStatuses) {
                        System.out.println(clientStatus);
                    }
                } else {
                    System.out.println("Server Received: " + serverMessage);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                break;
            }
        }
    }

    // Main method to start the client
    public static void main(String[] args) throws Exception
    {
        // Create a datagram socket
        DatagramSocket datagramSocket = new DatagramSocket();
        // Get the inet address of the server
        InetAddress inetAddress = InetAddress.getByName("localhost");   
        // Create a new UDPClient instance
        UDPClient client = new UDPClient(datagramSocket, inetAddress);
        System.out.println("Client has started");

        // Thread to listen for server status messages
        new Thread(() -> {
            client.listenForServerStatus();
        }).start();

        // Start sending status messages
        client.sendStatus();
    }
}