import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class UDPReceiver implements Runnable {
    private int port;
    private static NodeStatusTracker tracker = new NodeStatusTracker(30);
    private static Map<String, String> fileMap = new HashMap<>();


    public UDPReceiver(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            System.out.println("Listening on port " + port);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Packet decodedPacket = Packet.decode(packet.getData());

                if (decodedPacket == null) {
                    System.err.println("Received invalid packet, ignoring");
                    continue;
                }

                String nodeId = decodedPacket.getNodeId();
                String fileList = String.join(", ", decodedPacket.getFileList());

                //Upodate node status
                tracker.updateNode(decodedPacket.getNodeId());
                
                //Store file list from the node
                fileMap.put(nodeId, fileList);

                // Print updated node statuses
                System.out.println("\nUpdated Node Statuses:");
                tracker.getAllNodeStatuses().forEach((id, status) -> 
                    System.out.println("  " + id + " -> " + status)
                );

                // Print updated file lists
                System.out.println("\nUpdated File Lists:");
                fileMap.forEach((id, files) -> 
                    System.out.println("  " + id + " has: " + files)
                );
            }
        } catch (Exception e) {
            System.err.println("Error in UDPReceiver: " + e.getMessage());
        }
    }
}