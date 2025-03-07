package Peer2Peer;

/**
 * Test launcher that starts multiple peers on different ports
 * to test the Peer2Peer network functionality.
 */
public class TestLauncher {
    public static void main(String[] args) {
        try {
            // Start 3 peers on different ports with different file listings
            startPeer(5000, new String[]{"file1.txt", "file2.txt", "shared1.txt"});
            Thread.sleep(2000); // Wait for the first peer to initialize
            
            startPeer(5001, new String[]{"file3.txt", "file4.txt", "shared1.txt", "shared2.txt"});
            Thread.sleep(2000); // Wait for the second peer to initialize
            
            startPeer(5002, new String[]{"file5.txt", "file6.txt", "shared2.txt"});
            
            System.out.println("All peers started. Press Ctrl+C to terminate.");
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
}