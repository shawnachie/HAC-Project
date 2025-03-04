import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UDPServer2
{
    private DatagramSocket datagramSocket;
    // buffer to store data received by client
    private byte[] buffer = new byte[1024];
    // List to store client data
    private List<ClientData> clientDataList;

    // constructor to initialize the datagram socket and list
    public UDPServer2(DatagramSocket datagramSocket)
    {
        this.datagramSocket = datagramSocket;
        this.clientDataList = new ArrayList<>();
    }

    // method will receive the data from client and send a response back to client
    public void recieveAndResponse() 
    {   
        // infinite loop to keep the server running
        while (true)
        {
            try {
                System.out.println("Waiting to receive packet...");
                // datagram allows connectionless communication
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                // blocking method that halts program until datagram is received, and stores data
                datagramSocket.receive(datagramPacket);
                System.out.println("Packet received.");
            
                // gets the address and port number of the current client
                InetAddress inetAddress = datagramPacket.getAddress();
                int portNumber = datagramPacket.getPort();

                // converts the data received from client from byte to string
                String clientMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

                // Check if the message is a broadcast message sent by the server itself
                if (clientMessage.startsWith("Client statuses:")) {
                    System.out.println("Ignoring broadcast message: " + clientMessage);
                    continue;
                }

                // Parse the data received from the client
                String[] parts = clientMessage.split(",");
                if (parts.length >= 3) {
                    String statusPart = parts[0];
                    String timestampPart = parts[1];
                    String filesPart = parts[2];
                    
                    if (statusPart.startsWith("Status:") && timestampPart.startsWith("Timestamp:") && filesPart.startsWith("Files:")) {
                        String status = statusPart.split(":")[1];
                        String timestamp = timestampPart.split(":")[1];
                        String files = filesPart.substring(6); // Remove "Files:" prefix
                        
                        // Check if the client data already exists
                        boolean clientExists = false;
                        for (ClientData clientData : clientDataList) {
                            if (clientData.getIpAddress().equals(inetAddress.toString()) && clientData.getPort() == portNumber) {
                                // Update the existing client data
                                clientData.setStatus("alive");
                                clientData.setTimestamp(timestamp);
                                clientData.setFiles(files);
                                clientData.updateLastPacketTime();
                                clientExists = true;
                                break;
                            }
                        }
                        
                        // If the client data does not exist, add a new entry
                        if (!clientExists) {
                            ClientData clientData = new ClientData(inetAddress.toString(), portNumber, "alive", timestamp, files);
                            clientDataList.add(clientData);
                        }
                        
                        System.out.println("Client: " + inetAddress.toString() + ":" + portNumber + " - Status: " + status + ", Timestamp: " + timestamp);
                        System.out.println("Files:");
                        for (String file : files.split(",")) {
                            System.out.println(" - " + file);
                        }
                    } else {
                        System.out.println("Invalid message format: " + clientMessage);
                    }
                } else {
                    System.out.println("Invalid message format: " + clientMessage);
                }
                
                // Prepare response packet
                String responseMessage = "Received: Status and files";
                byte[] responseBuffer = responseMessage.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, inetAddress, portNumber);
                datagramSocket.send(responsePacket);
                
                // Clear the buffer
                buffer = new byte[1024];
            } catch (IOException e) 
            {
                e.printStackTrace();
                break;
            }
        }
    }

    // method to print the status of all clients
    public void printClientStatuses() {
        System.out.println("Current client statuses:");
        for (ClientData clientData : clientDataList) {
            System.out.println(clientData);
        }
    }

    // method to send the status of all clients
    public void sendClientStatuses() {
        try {
            // Create a StringBuilder to construct the status message
            StringBuilder statusMessage = new StringBuilder("Client statuses:\n");
            
            // Iterate through the clientDataList to append each client's status to the message
            for (ClientData clientData : clientDataList) {
                // Check if the client is "dead"
                if (Duration.between(clientData.getLastPacketTime(), Instant.now()).getSeconds() > 45) {
                    clientData.setStatus("dead");
                }
                statusMessage.append(clientData).append("\n");
            }
            
            // Convert the status message to a byte array
            byte[] statusBuffer = statusMessage.toString().getBytes();
            
            // Define the broadcast address to send the status message to all clients
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255"); // Broadcast address
            
            // Create a DatagramPacket to send the status message
            DatagramPacket statusPacket = new DatagramPacket(statusBuffer, statusBuffer.length, broadcastAddress, 9876);
            
            // Send the status packet
            datagramSocket.send(statusPacket);
        } catch (IOException e) {
            // Print the stack trace if an IOException occurs
            e.printStackTrace();
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws SocketException 
    {
        // Create a datagram socket on port 9876
        DatagramSocket datagramSocket = new DatagramSocket(9876); 
        // Create a new UDPServer2 instance
        UDPServer2 server = new UDPServer2(datagramSocket); 
        System.err.println("Server has started");
        
        // Thread to send client statuses every 30 seconds
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Send statuses every 30 seconds
                    server.sendClientStatuses();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        // Start receiving and responding to client messages
        server.recieveAndResponse(); 
    }
}