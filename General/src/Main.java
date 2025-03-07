import java.util.List;

public class Main {
    public static void main(String[] args) {
        ConfigLoader config = new ConfigLoader("General/config.properties");

        int port = config.getInt("udp.port");
        String nodeId = config.getString("node.id");
        String mode = config.getString("node.mode");
        List<String> nodeIps = config.getList("node.ips");
        int interval = config.getInt("udp.broadcast_interval");

        // Start UDPReceiver in a new thread
        Thread receiverThread = new Thread(new UDPReceiver(port));
        receiverThread.start();

        // Start UDPSender in a new thread
        Thread senderThread = new Thread(new UDPSender(nodeIps, port, nodeId, mode, interval));
        senderThread.start();
    }
}