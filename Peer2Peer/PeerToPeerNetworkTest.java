package Peer2Peer;

import java.util.ArrayList;
import java.util.List;

public class PeerToPeerNetworkTest {

    public static void main(String[] args) {
        try {
            // Create and start nodes in separate threads
            List<Thread> nodeThreads = new ArrayList<>();

            List<String> fileListing1 = List.of("file1.txt", "file2.jpg", "file3.pdf");
            List<String> fileListing2 = List.of("file4.docx", "file5.png", "file6.mp3");
            List<String> fileListing3 = List.of("file7.zip", "file8.mp4", "file9.csv");

            nodeThreads.add(createAndStartNode(5000, fileListing1));
            nodeThreads.add(createAndStartNode(5001, fileListing2));
            nodeThreads.add(createAndStartNode(5002, fileListing3));

            // Wait for all threads to finish
            for (Thread thread : nodeThreads) {
                thread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Thread createAndStartNode(int port, List<String> fileListing) {
        Thread nodeThread = new Thread(() -> {
            try {
                String[] args = new String[fileListing.size() + 1];
                args[0] = String.valueOf(port);
                for (int i = 0; i < fileListing.size(); i++) {
                    args[i + 1] = fileListing.get(i);
                }
                Peer.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        nodeThread.start();
        return nodeThread;
    }
}