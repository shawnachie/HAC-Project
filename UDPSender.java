import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;



public class UDPSender {
    public static void sendPacket(Packet packet, String destinationIPaddress, int port) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = packet.encode();

        InetAddress address = InetAddress.getByName(destinationIPaddress);
        DatagramPacket packetWeAreSending = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packetWeAreSending);
        socket.close();
    }    
}
