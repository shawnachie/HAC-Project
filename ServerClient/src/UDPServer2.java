package ServerClient.src;

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
    private int udpPort;

    // Constructor to initialize the datagram socket and client data list
    public UDPServer2(DatagramSocket datagramSocket, int udpPort) {
        this.datagramSocket = datagramSocket;
        this.clientDataList = new ArrayList<>();
        this.udpPort = udpPort;
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
                Packet packet = Packet.decode(datagramPacket.getData());

                // Ignore broadcast messages
                if (packet != null && packet.getStatus().equals("Client statuses:")) {
                    System.out.println("Ignoring broadcast message: " + packet.getStatus());
                    continue;
                }

                // Process client message
                String status = packet.getStatus();
                long timestamp = packet.getTimestamp();
                List<String> files = packet.getFileList();

                boolean clientExists = false;
                for (ClientData clientData : clientDataList) {
                    // Check if the client already exists in the list
                    if (clientData.getIpAddress().equals(inetAddress.toString()) && clientData.getPort() == portNumber) {
                        // Update existing client data
                        clientData.setStatus("alive");
                        clientData.setTimestamp(Long.toString(timestamp));
                        clientData.setFiles(String.join(",", files));
                        clientData.updateLastPacketTime();
                        clientExists = true;
                        break;
                    }
                }

                // If the client data does not exist, add a new entry
                if (!clientExists) {
                    ClientData clientData = new ClientData(inetAddress.toString(), portNumber, "alive", Long.toString(timestamp), String.join(",", files));
                    clientDataList.add(clientData);
                }

                // Print client information
                System.out.println("Client: " + inetAddress.toString() + ":" + portNumber + " - Status: " + status + ", Timestamp: " + timestamp);
                System.out.println("Files:");
                for (String file : files) {
                    System.out.println(" - " + file);
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
                StringBuilder statusMessage = new StringBuilder("Active Client statuses:\n");
                for (ClientData clientData : clientDataList) {
                    // Mark client as dead if no packet received in the last 45 seconds
                    if (Duration.between(clientData.getLastPacketTime(), Instant.now()).getSeconds() > 45) {
                        clientData.setStatus("dead");
                    }
                    statusMessage.append("Client: ").append(clientData.getIpAddress()).append(":").append(clientData.getPort())
                            .append(" - Status: ").append(clientData.getStatus())
                            .append(", Timestamp: ").append(clientData.getTimestamp())
                            .append(", Files:\n");
                    for (String file : clientData.getFiles().split(",")) {
                        statusMessage.append(" - ").append(file).append("\n");
                    }
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
        // Load configuration
        ConfigLoader config = new ConfigLoader("ServerClient/serverconfig.properties");
        int udpPort = config.getInt("udp.port");

        // Validate configuration
        if (udpPort == 0) {
            System.err.println("Invalid UDP port: " + udpPort);
            return;
        }

        // Create a datagram socket
        DatagramSocket datagramSocket = new DatagramSocket(udpPort);
        UDPServer2 server = new UDPServer2(datagramSocket, udpPort);
        System.err.println("Server has started");

        // Thread to send client statuses every 30 seconds
        new Thread(() -> {
            server.sendClientStatuses();
        }).start();

        // Start receiving and responding to client messages
        server.recieveAndResponse();
    }
}