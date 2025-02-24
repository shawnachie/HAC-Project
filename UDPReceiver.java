import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver {
    public static void ListenForPacket(int port) throws Exception {
        DatagramSocket socket = new DatagramSocket(port);
        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            Packet receivedPacket = Packet.decode(packet.getData());

            byte[] receivedData = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, receivedData, 0, packet.getLength());

            Packet decodedPacket = Packet.decode(receivedData);
            System.out.println("Received: " + decodedPacket); // take this line out later
        }
    }
    public static void main(String[] args) throws Exception {
        int port = 5001;
        ListenForPacket(port);
    }
}
