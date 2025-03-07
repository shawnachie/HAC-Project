import java.util.Arrays;

public class UDPTest {
    public static void main(String[] args) throws Exception {
        // Create a test packet
        Packet testPacket = new Packet("P2P", "Node1", "ALIVE", Arrays.asList("file1.txt", "file2.txt"));

        // Send the packet to localhost (127.0.0.1)
        UDPSender.sendPacket(testPacket, "127.0.0.1", 5001);
        
        // Start listening for packets this is suppose to run in a diff thread
        UDPReceiver.ListenForPacket(5001);
    }
}