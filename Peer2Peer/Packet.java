package Peer2Peer;

import java.nio.ByteBuffer;
import java.util.List;

public class Packet {
    private final String mode;      
    private final String nodeId;    // senderID
    private String status;    // "ALIVE" or "DEAD"
    private List<String> fileList; // List of files available on this node
    private long timestamp;   // When the message was created

    // Constructors
    public Packet(String mode, String nodeId, String status, List<String> fileList) {
        this.mode = mode;
        this.nodeId = nodeId;
        this.status = status;
        this.fileList = fileList;
        this.timestamp = System.currentTimeMillis();
    }

    // Convert to binary for UDP sending
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        putString(buffer, mode);
        putString(buffer, nodeId);
        putString(buffer, status);
        buffer.putLong(timestamp);
        putString(buffer, String.join(",", fileList)); // Idea here is to store the file list as one string
        return buffer.array();
    }

    // Decode a JSON string back into a Packet object
    public static Packet decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        String mode = getString(buffer);
        String nodeId = getString(buffer);
        String status = getString(buffer);
        long timestamp = buffer.getLong();
        List<String> fileList = List.of(getString(buffer).split(","));
        return new Packet(mode, nodeId, status, fileList);
    }

    private static void putString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    private static String getString(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes);
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
