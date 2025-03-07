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
    private static final String SEPARATOR = "----------------------------------------";

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
                byte[] buffer = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                System.out.println(SEPARATOR);
                System.out.println("[Server] Waiting for client updates...");
                System.out.println(SEPARATOR);

                datagramSocket.receive(datagramPacket);
                
                InetAddress inetAddress = datagramPacket.getAddress();
                int portNumber = datagramPacket.getPort();
                Packet packet = Packet.decode(datagramPacket.getData());

                if (packet != null && !packet.getStatus().equals("Client statuses:")) {
                    System.out.println(SEPARATOR);
                    System.out.println("[Server] Received update from " + inetAddress.getHostAddress() + ":" + portNumber);
                    System.out.println("[Server] Node ID: " + packet.getNodeId());
                    System.out.println("[Server] Status: " + packet.getStatus());
                    System.out.println("[Server] Files:");
                    packet.getFileList().forEach(file -> System.out.println("[Server]  - " + file));
                    System.out.println(SEPARATOR);

                    updateClientData(inetAddress.getHostAddress(), portNumber, packet);
                }

            } catch (IOException e) {
                System.err.println("[Server Error] " + e.getMessage());
                break;
            }
        }
    }

    private void updateClientData(String address, int port, Packet packet) {
        boolean clientExists = false;
        for (ClientData clientData : clientDataList) {
            if (clientData.getIpAddress().equals(address) && clientData.getPort() == port) {
                clientData.setStatus("alive");
                clientData.setTimestamp(Long.toString(packet.getTimestamp()));
                clientData.setFiles(String.join(",", packet.getFileList()));
                clientData.updateLastPacketTime();
                clientExists = true;
                break;
            }
        }

        if (!clientExists) {
            ClientData clientData = new ClientData(address, port, "alive", 
                Long.toString(packet.getTimestamp()), 
                String.join(",", packet.getFileList()));
            clientDataList.add(clientData);
            System.out.println("[Server] New client registered: " + address + ":" + port);
        }
    }

    // Method to send the status of all clients to each client every 30 seconds
    public void sendClientStatuses() {
        while (true) {
            try {
                Thread.sleep(30000);

                if (clientDataList.isEmpty()) {
                    continue;
                }

                System.out.println(SEPARATOR);
                System.out.println("[Server] Broadcasting client status update");
                StringBuilder statusMessage = new StringBuilder("Active Client statuses:\n");

                for (ClientData clientData : clientDataList) {
                    // Mark client as dead if no packet received in the last 30 seconds
                    if (Duration.between(clientData.getLastPacketTime(), Instant.now()).getSeconds() > 30) {
                        clientData.setStatus("dead");
                    }

                    statusMessage.append(String.format("Client: %s:%d - %s\n", 
                        clientData.getIpAddress(), 
                        clientData.getPort(), 
                        clientData.getStatus()));

                    if (!clientData.getFiles().isEmpty()) {
                        for (String file : clientData.getFiles().split(",")) {
                            statusMessage.append(String.format("  - %s\n", file));
                        }
                    }
                }

                byte[] statusBuffer = statusMessage.toString().getBytes();
                for (ClientData clientData : clientDataList) {
                    try {
                        InetAddress clientAddress = InetAddress.getByName(clientData.getIpAddress().substring(1));
                        DatagramPacket statusPacket = new DatagramPacket(
                            statusBuffer, 
                            statusBuffer.length, 
                            clientAddress, 
                            clientData.getPort()
                        );
                        datagramSocket.send(statusPacket);
                    } catch (IOException e) {
                        System.err.println("[Server Error] Failed to send status to " + 
                            clientData.getIpAddress() + ":" + clientData.getPort());
                    }
                }
                System.out.println("[Server] Status broadcast complete");
                System.out.println(SEPARATOR);

            } catch (InterruptedException e) {
                System.err.println("[Server Error] " + e.getMessage());
                break;
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