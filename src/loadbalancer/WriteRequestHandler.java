package loadbalancer;

import common.Message;
import common.SocketUtils;

public class WriteRequestHandler implements RequestHandler {

    private final QueueManager queueManager;
    private final ConsistencyMonitor consistencyMonitor;

    public WriteRequestHandler(QueueManager queueManager, ConsistencyMonitor consistencyMonitor) {
        this.queueManager = queueManager;
        this.consistencyMonitor = consistencyMonitor;
    }

    @Override
    public void handleRequest(Message message, ServerRegistry registry) throws Exception {
        // Verifica consistência
        if (!consistencyMonitor.isSystemConsistent()) {
            // REPRESAMENTO: coloca na fila
            queueManager.addPendingWrite(message);
            System.out.println("[WriteHandler] Write queued (system inconsistent). Pending: " + queueManager.getPendingCount() + " (requestId=" + message.requestId + ")");
            return;
        }
        
        // Seleciona UM servidor aleatório
        ServerRegistry.ServerInfo server = registry.getRandomServer();
        
        if (server == null) {
            System.err.println("[WriteHandler] No available servers");
            return;
        }
        
        // forwarding WRITE (silent)

        try {
            // marca que o LB está encaminhando esta WRITE
            message.senderPort = 9000;
            SocketUtils.sendMessage("localhost", server.getPort(), message);
        } catch (Exception e) {
            System.err.println("[WriteHandler] Failed to send WRITE to server " + server.getPort() + ": " + e.getMessage());
            // Marca servidor como indisponível se houve timeout/erro
            registry.markServerUnavailable(server.getPort());
            // Re-queue the message to avoid loss
            queueManager.addPendingWrite(message);
            System.out.println("[WriteHandler] Re-queued WRITE due to send failure. Pending: " + queueManager.getPendingCount() + " (requestId=" + message.requestId + ")");
        }
    }
}