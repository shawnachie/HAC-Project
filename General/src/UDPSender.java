import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UDPSender implements Runnable {
    private List<String> nodeIps;
    private int port;
    private String nodeId;
    private String mode;
    private int interval;

    public UDPSender(List<String> nodeIps, int port, String nodeId, String mode, int interval) {
        this.nodeIps = nodeIps;
        this.port = port;
        this.nodeId = nodeId;
        this.mode = mode;
        this.interval = interval;
    }

    @Override
    public void run() {
        try {
            while (true) {
                List<String> fileList = getHomeDirectoryFiles();

                Packet testPacket = new Packet(mode, nodeId, "ALIVE", fileList);
                for (String ip : nodeIps) {
                    System.out.println("Sending packet to " + ip + ":" + port);
                    sendPacket(testPacket, ip, port);
                }

                Thread.sleep(interval * 1000);
            }
        } catch (Exception e) {
            System.err.println("Error in UDPSender: " + e.getMessage());
        }
    }

    private void sendPacket(Packet packet, String ipAddress, int port) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = packet.encode();
        InetAddress address = InetAddress.getByName(ipAddress);
        DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(udpPacket);
        System.out.println("Packet sent to " + ipAddress + ":" + port);
        socket.close();
    }

    private List<String> getHomeDirectoryFiles() {
        File homeDir = new File("/home/");
        if (!homeDir.exists() || !homeDir.isDirectory()) {
            System.err.println("/home/ directory does not exist!");
            return List.of();
        }
        return Arrays.stream(homeDir.listFiles())
                     .filter(File::isFile)
                     .map(File::getName)
                     .collect(Collectors.toList());
    }
}