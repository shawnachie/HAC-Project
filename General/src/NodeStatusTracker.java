import java.util.HashMap;
import java.util.Map;


public class NodeStatusTracker {
    private final Map<String, Long> nodeTimestamps;
    private final long timeout;
    private final Map<String, String> nodeStatuses;

    public NodeStatusTracker(long timeoutSeconds) {
        this.nodeTimestamps = new HashMap<>();
        this.timeout = timeoutSeconds * 1000;
        this.nodeStatuses = new HashMap<>();
    }
    
    public void updateNode(String nodeId) {
        long currentTime = System.currentTimeMillis();
        nodeTimestamps.put(nodeId, currentTime);

        if ("DEAD".equals(getNodeStatus(nodeId))) {
            System.out.println("Node " + nodeId + " is back online");
        }

        nodeStatuses.put(nodeId, getNodeStatus(nodeId));
    }


    public String getNodeStatus(String nodeId) {
        Long lastAlive = nodeTimestamps.get(nodeId);
        if (lastAlive == null || System.currentTimeMillis() - lastAlive > timeout) {
            nodeStatuses.put(nodeId, "DEAD");
            return "DEAD";
        }
        return "ALIVE";
    }

    public Map<String, String> getAllNodeStatuses() {
        Map<String, String> statuses = new HashMap<>();
        for (Map.Entry<String, Long> entry : nodeTimestamps.entrySet()) {
            statuses.put(entry.getKey(), getNodeStatus(entry.getKey()));
        }
        return statuses;
    }
}
