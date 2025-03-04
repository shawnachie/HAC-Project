import java.time.Instant;

public class ClientData {
    private String ipAddress;
    private int port;
    private String status;
    private String timestamp;
    private String files;
    private Instant lastPacketTime;

    public ClientData(String ipAddress, int port, String status, String timestamp, String files) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = status;
        this.timestamp = timestamp;
        this.files = files;
        this.lastPacketTime = Instant.now();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFiles() {
        return files;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    public Instant getLastPacketTime() {
        return lastPacketTime;
    }

    public void updateLastPacketTime() {
        this.lastPacketTime = Instant.now();
    }

    @Override
    public String toString() {
        return "Client: " + ipAddress + ":" + port + " - Status: " + status + ", Timestamp: " + timestamp + ", Files: " + files;
    }
}