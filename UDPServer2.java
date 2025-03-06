import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UDPServer2 {
    private DatagramSocket datagramSocket;
    private List<ClientData> clientDataList;

    // Constructor to initialize the datagram socket and client data list
    public UDPServer2(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
        this.clientDataList = new ArrayList<>();
    }

    // Method to receive data from clients and respond
    public void recieveAndResponse() {
        while (true) {
            try {
                System.out.println("Waiting to receive packet...");
                byte[] buffer = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                // Receive packet from client
                datagramSocket.receive(datagramPacket);
                System.out.println("Packet received.");

                InetAddress inetAddress = datagramPacket.getAddress();
                int portNumber = datagramPacket.getPort();
                String clientMessage = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

                // Ignore broadcast messages
                if (clientMessage.startsWith("Client statuses:")) {
                    System.out.println("Ignoring broadcast message: " + clientMessage);
                    continue;
                }

                // Process client message
                String[] parts = clientMessage.split(",");
                if (parts.length >= 3) {
                    String statusPart = parts[0];
                    String timestampPart = parts[1];
                    String filesPart = parts[2];

                    if (statusPart.startsWith("Status:") && timestampPart.startsWith("Timestamp:") && filesPart.startsWith("Files:")) {
                        String status = statusPart.split(":")[1];
                        String timestamp = timestampPart.split(":")[1];
                        String files = filesPart.substring(6);

                        boolean clientExists = false;
                        for (ClientData clientData : clientDataList) {
                            if (clientData.getIpAddress().equals(inetAddress.toString()) && clientData.getPort() == portNumber) {
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

                        // Print client information
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
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    // Method to print the status of all clients
    public void printClientStatuses() {
        System.out.println("Current client statuses:");
        for (ClientData clientData : clientDataList) {
            System.out.println(clientData);
        }
    }

    // Method to send the status of all clients to each client every 30 seconds
    public void sendClientStatuses() {
        while (true) {
            try {
                Thread.sleep(30000); // Send statuses every 30 seconds

                // Prepare status message
                StringBuilder statusMessage = new StringBuilder("Client statuses:\n");
                for (ClientData clientData : clientDataList) {
                    if (Duration.between(clientData.getLastPacketTime(), Instant.now()).getSeconds() > 45) {
                        clientData.setStatus("dead");
                    }
                    statusMessage.append(clientData).append("\n");
                }

                // Send status message to each client
                byte[] statusBuffer = statusMessage.toString().getBytes();
                for (ClientData clientData : clientDataList) {
                    InetAddress clientAddress = InetAddress.getByName(clientData.getIpAddress().substring(1)); // Remove leading slash
                    DatagramPacket statusPacket = new DatagramPacket(statusBuffer, statusBuffer.length, clientAddress, clientData.getPort());
                    datagramSocket.send(statusPacket);
                }
                System.out.println("Broadcast message sent: " + statusMessage.toString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws SocketException {
        // Create a datagram socket
        DatagramSocket datagramSocket = new DatagramSocket(9876);
        UDPServer2 server = new UDPServer2(datagramSocket);
        System.err.println("Server has started");

        // Thread to send client statuses every 30 seconds
        new Thread(() -> {
            server.sendClientStatuses();
        }).start();

        // Start receiving and responding to client messages
        server.recieveAndResponse();
    }
}