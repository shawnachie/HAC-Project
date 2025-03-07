package Peer2Peer;

import java.util.List;

public class PeerInfo {
    private String mode;
    private String status;
    private List<String> fileList;
    private String ip;

    public PeerInfo(String mode, String status, List<String> fileList, String ip) {
        this.mode = mode;
        this.status = status;
        this.ip = ip;
        this.fileList = fileList;
    }

    public String getMode() { return mode; }
    public String getStatus() { return status; }
    public String getIp() { return ip; }
    public List<String> getFileList() { return fileList; }
}
