package loadbalancer;

import common.Message;
import java.util.concurrent.*;

public class QueueManager {
    private final BlockingQueue<Message> pendingQueue;
    
    public QueueManager() {
        this.pendingQueue = new LinkedBlockingQueue<>();
    }
    
    public void addPendingWrite(Message message) {
        pendingQueue.offer(message);
    }
    

    
    public int getPendingCount() {
        return pendingQueue.size();
    }
    
    public boolean hasPendingWrites() {
        return !pendingQueue.isEmpty();
    }
    
    public void processAllPending(RequestHandler writeHandler, ServerRegistry registry) {
        System.out.println("[QueueManager] Processing " + getPendingCount() + " pending writes");

        while (hasPendingWrites()) {
            try {
                Message message = pendingQueue.poll();
                if (message != null) {
                    writeHandler.handleRequest(message, registry);
                }
            } catch (Exception e) {
                System.err.println("[QueueManager] Error processing pending write: " + e.getMessage());
            }
        }
        System.out.println("[QueueManager] Finished processing pending writes");
    }
}