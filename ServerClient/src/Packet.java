package ServerClient.src;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Packet {
    private final String mode;      
    private final String nodeId;    // senderID
    private String status;    // "ALIVE" or "DEAD"
    private List<String> fileList; // List of files available on this node
    private long timestamp;   // When the message was created

    // Constructors
    public Packet(String mode, String nodeId, String status, List<String> fileList) {
        this.mode = (mode != null) ? mode : "UNKNOWN";
        this.nodeId = (nodeId != null) ? nodeId : "UNKNOWN";
        this.status = (status != null) ? status : "UNKNOWN";
        this.fileList = (fileList != null) ? fileList : List.of();
        this.timestamp = System.currentTimeMillis();
    }

    // Convert to binary for UDP sending
    public byte[] encode() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            putString(buffer, mode);
            putString(buffer, nodeId);
            putString(buffer, status);
            buffer.putLong(timestamp);
            putString(buffer, String.join(",", fileList));

            System.out.println("Encoded packet: " + mode + " " + nodeId + " " + status + " " + fileList + " " + timestamp);
            return buffer.array();
        } catch (Exception e) {
            System.err.println("Error encoding packet: " + e.getMessage());
            return new byte[0];
        }
    }

    // Decode a JSON string back into a Packet object
    public static Packet decode(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String mode = getString(buffer);
            String nodeId = getString(buffer);
            String status = getString(buffer);
            long timestamp = buffer.getLong();
            List<String> fileList = Arrays.asList(getString(buffer).split(","));

            System.out.println("Decoded packet: "+ mode + " " + nodeId + " " + status + " " + fileList + " " + timestamp);

            return new Packet(mode, nodeId, status, fileList);
        } catch (Exception e) {
            System.err.println("Error decoding packet: " + e.getMessage());
            return null;
        }
    }

    private static void putString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private static String getString(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Getters (we might need them)
    public String getMode() { return mode; }
    public String getNodeId() { return nodeId; }
    public String getStatus() { return status; }
    public List<String> getFileList() { return fileList; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return mode + "|" + nodeId + "|" + status + "|" + fileList + "|" + timestamp;
    }
}