package client;

import java.io.IOException;

public class Client {
    public static void main(String[] args) {
        System.out.println("[Client] Starting...");
        
        ClientTask clientTask = null;
        try {
            clientTask = new ClientTask("localhost", 9000, "client_log.txt");
            
            // Loop infinito
            while (true) {
                clientTask.executeRequest();
                Thread.sleep(clientTask.getRandomSleepTime());
            }
            
        } catch (IOException e) {
            // Mostra erro específico
            if (e.getMessage() != null) {
                System.err.println("[Client] IO Error: " + e.getMessage());
            } else if (e instanceof java.io.EOFException) {
                System.err.println("[Client] Error: Server closed connection unexpectedly");
            } else {
                System.err.println("[Client] Connection error");
            }
            
        } catch (InterruptedException e) {
            System.err.println("[Client] Interrupted");
            
        } catch (Exception e) {
            System.err.println("[Client] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            if (clientTask != null) {
                try {
                    clientTask.close();
                    System.out.println("[Client] Log saved: client_log.txt");
                } catch (IOException e) {
                    System.err.println("[Client] Error closing log: " + e.getMessage());
                }
            }
        }
    }
}