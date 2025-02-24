import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author cjaiswal
 */
public class UDPClient2 
{
    //allows us to utilize secure random when designating IDs to our nodes
    private SecureRandom secureRandom = new SecureRandom();
    private int nodeId = secureRandom.nextInt(777);

    //private InetAddress IPAddress = 000.0.0.0; //hardcode this in later
    private int serverPort = 0000; //hardcode this in later

    private DatagramSocket socket;

    public static void main(String[] args) {
        UDPClient2 client = new UDPClient2();
        client.sendPackets();
    }
    
    //comment for now since we dont have IP yet 
    /*public UDPClient2() {
        try {
            socket = new DatagramSocket();
            IPAddress = InetAddress.getByName(IPAddress); // replace with the actual server IP
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    } */

    //sets the file path to stary from the "home" directory 
    private String fileListing = new File("home").getAbsolutePath();

    // Method to retrieve the list of files from the home directory
    private List<String> getFileNames() 
    {
        File directory = new File(fileListing);
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Directory does not exist: " + fileListing);
            return List.of();
        }

        String[] files = directory.list((dir, name) -> new File(dir, name).isFile());
        return (files != null) ? Arrays.asList(files) : List.of();
    }

    // Method to create a Packet
    private Packet createPacket(String status) {
        List<String> fileList = getFileNames();
        return new Packet("UPDATE", String.valueOf(nodeId), status, fileList);
    }

    // Method to send the packet at random intervals
    private void sendPackets() {
        try {
            while (true) {
                String status = "ALIVE";
                Packet packet = createPacket(status); // Create a packet with status and file list

                // Send the packet as a JSON string
                DatagramPacket sendPacket = new DatagramPacket(packet.encode().getBytes(), packet.encode().length(), IPAddress, serverPort);
                socket.send(sendPacket);

                System.out.println("Sent packet: " + packet); // Print the sent packet for debugging

                // Random delay between 1-30 seconds
                long delay = secureRandom.nextInt(30) + 1; // Random delay between 1 and 30 seconds
                TimeUnit.SECONDS.sleep(delay);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
