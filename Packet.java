import com.google.gson.Gson;
import java.util.List;

public class Packet {
    private String mode;      
    private String nodeId;    // senderID
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




    // Convert this object to a JSON string for UDP sending
    public String encode() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Decode a JSON string back into a Packet object
    public static Packet decode(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Packet.class);
    }

    // Getters (we might need them)
    public String getMode() { return mode; }
    public String getNodeId() { return nodeId; }
    public String getStatus() { return status; }
    public List<String> getFileList() { return fileList; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return encode(); // Display as JSON - we can use this to debug the code
}
