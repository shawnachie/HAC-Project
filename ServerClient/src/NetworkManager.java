package ServerClient.src;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkManager {
    private ExecutorService executorService;
    private boolean isRunning;
    private static final String SEPARATOR = "----------------------------------------";
    private static final int DEFAULT_NUM_CLIENTS = 5; // 1 server + 5 clients = 6 nodes total
    private static final int MIN_CLIENTS = 1;
    private static final int MAX_CLIENTS = 5;

    public NetworkManager(int numberOfClients) {
        // 2 threads for server + 2 threads per client
        this.executorService = Executors.newFixedThreadPool(2 + (2 * numberOfClients));
        this.isRunning = false;
    }

    public void startNetwork(int numberOfClients) {
        try {
            System.out.println(SEPARATOR);
            System.out.println("[System] Initializing HAC (High Availability Cluster)");
            System.out.println("[System] Mode: Client-Server");
            System.out.println("[System] Protocol Version: 1.0");
            System.out.println("[System] Total Nodes: " + (numberOfClients + 1));
            System.out.println(SEPARATOR);

            // Start server first
            startServer();
            
            // Give server time to initialize
            Thread.sleep(1000);
            
            // Start multiple clients
            for (int i = 1; i <= numberOfClients; i++) {
                final int clientNumber = i;
                startClient("node" + clientNumber);
                // Small delay between client starts to prevent network congestion
                Thread.sleep(500);
            }

            System.out.println(SEPARATOR);
            System.out.println("[System] Network started successfully");
            System.out.println("[System] Configuration:");
            System.out.println("[System] - 1 Server node");
            System.out.println("[System] - " + numberOfClients + " Client nodes");
            System.out.println("[System] - Random status interval: 0-30 seconds");
            System.out.println("[System] - Dead node detection: 30 seconds");
            System.out.println("[System] - File monitoring: Enabled");
            System.out.println(SEPARATOR);

        } catch (Exception e) {
            System.err.println("[System Error] " + e.getMessage());
            e.printStackTrace();
            shutdown();
        }
    }

    private void startServer() {
        try {
            // Load server configuration
            ConfigLoader serverConfig = new ConfigLoader("ServerClient/serverconfig.properties");
            int serverPort = serverConfig.getInt("udp.port");
            
            if (serverPort == 0) {
                throw new IllegalArgumentException("Invalid server UDP port");
            }

            // Create server socket and start server
            DatagramSocket serverSocket = new DatagramSocket(serverPort);
            UDPServer2 server = new UDPServer2(serverSocket, serverPort);
            isRunning = true;

            System.out.println(SEPARATOR);
            System.out.println("[Server] Starting HAC server node");
            System.out.println("[Server] Listening on port: " + serverPort);
            System.out.println("[Server] Role: Master node");
            System.out.println(SEPARATOR);

            // Start server threads
            executorService.submit(() -> server.recieveAndResponse());
            executorService.submit(() -> server.sendClientStatuses());

        } catch (Exception e) {
            System.err.println("[Server Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startClient(String nodeId) {
        try {
            // Load client configuration
            ConfigLoader clientConfig = new ConfigLoader("ServerClient/clientconfig.properties");
            String mode = clientConfig.getString("node.mode");
            List<String> nodeIps = clientConfig.getList("node.ips");
            int basePort = clientConfig.getInt("udp.port");

            if (basePort == 0) {
                throw new IllegalArgumentException("Invalid UDP port");
            }

            // Calculate unique port for this client (base port + node number)
            int clientNumber = Integer.parseInt(nodeId.replace("node", ""));
            int clientPort = basePort + clientNumber;

            // Create client socket and start client
            InetAddress serverAddress = InetAddress.getByName(nodeIps.get(0));
            DatagramSocket clientSocket = new DatagramSocket(clientPort);
            UDPClient client = new UDPClient(clientSocket, serverAddress, nodeId, mode, nodeIps, basePort);

            System.out.println(SEPARATOR);
            System.out.println("[Client] Starting HAC client " + nodeId);
            System.out.println("[Client] Local port: " + clientPort);
            System.out.println("[Client] Server address: " + serverAddress.getHostAddress() + ":" + basePort);
            System.out.println("[Client] Role: Data node");
            System.out.println(SEPARATOR);

            // Start client threads
            executorService.submit(() -> client.sendStatus());
            executorService.submit(() -> client.listenForServerStatus());

        } catch (Exception e) {
            System.err.println("[Client Error] Failed to start client " + nodeId);
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (!isRunning) {
            return;
        }

        System.out.println(SEPARATOR);
        System.out.println("[System] Initiating HAC shutdown sequence...");
        isRunning = false;

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        System.out.println("[System] All nodes terminated");
        System.out.println("[System] Shutdown complete");
        System.out.println(SEPARATOR);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java NetworkManager <mode> [nodeId]");
            System.err.println("Modes:");
            System.err.println("  server        - Start as server node");
            System.err.println("  client <num>  - Start as client node number <num>");
            System.err.println("Examples:");
            System.err.println("  Server:  java NetworkManager server");
            System.err.println("  Client1: java NetworkManager client 1");
            System.err.println("  Client2: java NetworkManager client 2");
            return;
        }

        NetworkManager manager = new NetworkManager(1); // Only need 2 threads for single node
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[System] Shutdown hook triggered...");
            manager.shutdown();
        }));

        // Start based on mode
        if (args[0].equals("server")) {
            manager.startServer();
        } else if (args[0].equals("client")) {
            if (args.length < 2) {
                System.err.println("Error: Client mode requires node number");
                return;
            }
            try {
                int nodeNum = Integer.parseInt(args[1]);
                if (nodeNum < 1 || nodeNum > MAX_CLIENTS) {
                    System.err.println("Error: Node number must be between 1 and " + MAX_CLIENTS);
                    return;
                }
                manager.startClient("node" + nodeNum);
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid node number");
            }
        } else {
            System.err.println("Error: Invalid mode. Use 'server' or 'client'");
        }
    }
}