package loadbalancer;

import common.Message;
import common.SocketUtils;
import java.util.List;

public class ReadRequestHandler implements RequestHandler {

    @Override
    public void handleRequest(Message message, ServerRegistry registry) throws Exception {
        // Broadcasting READ (silent)

        List<ServerRegistry.ServerInfo> servers = registry.getAllServers();

        for (ServerRegistry.ServerInfo server : servers) {
            if (server.isAvailable()) {
                try {
                    // marca que o LB está encaminhando este READ
                    message.senderPort = 9000;
                    SocketUtils.sendMessage("localhost", server.getPort(), message);
                } catch (Exception e) {
                    System.err.println("[ReadHandler] Failed to send to server " + server.getPort() + ": " + e.getMessage() + " (requestId=" + message.requestId + ")");
                    // Marca servidor como indisponível
                    registry.markServerUnavailable(server.getPort());
                }
            }
        }
    }
}