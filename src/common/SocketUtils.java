package common;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketUtils {
    private static final int TIMEOUT_MS = 3000; // 3 segundo timeout
    
    public static void sendMessage(String host, int port, Message message) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            // Define timeout para socket
            socket.setSoTimeout(TIMEOUT_MS);
            
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(message);
                out.flush();
            }
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[SocketUtils] Timeout sending to " + host + ":" + port);
            throw new Exception("Timeout: Server " + port + " not responding");
        } catch (Exception e) {
            System.err.println("[SocketUtils] Failed to send to " + host + ":" + port + " - " + e.getMessage());
            throw e;
        }
    }
}
