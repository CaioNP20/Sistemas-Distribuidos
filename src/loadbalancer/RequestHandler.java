package loadbalancer;

import common.Message;

public interface RequestHandler {
    void handleRequest(Message message, ServerRegistry registry) throws Exception;
}