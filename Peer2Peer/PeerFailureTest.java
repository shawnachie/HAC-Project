package Peer2Peer;

/**
 * Test class that simulates peer failures and recoveries to test the 
 * dead peer detection and reconnection functionality.
 */
public class PeerFailureTest {
    public static void main(String[] args) {
        try {
            // Start 2 long-running peers
            startPeer(5000, new String[]{"file1.txt", "file2.txt"});
            startPeer(5001, new String[]{"file3.txt", "file4.txt"});
            Thread.sleep(5000); // Let them establish connection
            
            System.out.println("\n----- STARTING FAILURE TEST -----\n");
            
            // Start a peer that will be terminated to simulate failure
            Process failingPeer = startFailingPeer(5002, new String[]{"temp1.txt", "temp2.txt"});
            
            // Let it run for some time to establish connections
            System.out.println("Waiting for peer connections to establish...");
            Thread.sleep(15000);
            
            // Kill the peer to simulate failure
            System.out.println("\n----- SIMULATING PEER FAILURE -----\n");
            failingPeer.destroy();
            System.out.println("Peer on port 8003 was terminated.");
            
            // Wait for the dead peer detection to trigger
            System.out.println("Waiting for dead peer detection...");
            Thread.sleep(35000); // Wait longer than MAX_INTERVAL
            
            // Restart the failed peer to test recovery
            System.out.println("\n----- SIMULATING PEER RECOVERY -----\n");
            startPeer(5002, new String[]{"temp1.txt", "temp2.txt"});
            
            System.out.println("Peer on port 8003 was restarted.");
            System.out.println("The other peers should detect it coming back online.");
            
            System.out.println("\nTest complete. Press Ctrl+C to terminate.");
            
            // Keep the program running
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void startPeer(int port, String[] files) {
        new Thread(() -> {
            try {
                String[] args = new String[files.length + 1];
                args[0] = String.valueOf(port);
                System.arraycopy(files, 0, args, 1, files.length);
                
                System.out.println("Starting peer on port " + port + " with files: " + String.join(", ", files));
                Peer.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private static Process startFailingPeer(int port, String[] files) throws Exception {
        String javaCmd = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");
        
        String[] command = new String[files.length + 5];
        command[0] = javaCmd;
        command[1] = "-cp";
        command[2] = classpath;
        command[3] = "Peer2Peer.Peer";
        command[4] = String.valueOf(port);
        System.arraycopy(files, 0, command, 5, files.length);
        
        System.out.println("Starting separate process peer on port " + port + " with files: " + String.join(", ", files));
        return Runtime.getRuntime().exec(command);
    }
}