package loadbalancer;

import common.Message;
import common.ConsistencyResponse;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConsistencyMonitor {
    private volatile boolean isConsistent = true;
    private static final int[] PORTS = {9101, 9102, 9103};
    private static final int TIMEOUT_MS = 3000; // 3 segundo timeout
    private final ServerRegistry serverRegistry;
    
    public ConsistencyMonitor(ServerRegistry serverRegistry) {
        this.serverRegistry = serverRegistry;
    }
    
    public boolean isSystemConsistent() {
        return isConsistent;
    }
    
    public void setSystemConsistent(boolean consistent) {
        boolean oldState = isConsistent;
        isConsistent = consistent;
        
        if (!oldState && consistent) {
            System.out.println("[ConsistencyMonitor] System is now CONSISTENT");
        } else if (oldState && !consistent) {
            System.out.println("[ConsistencyMonitor] System is now INCONSISTENT");
        }
    }
    
    // Faz checagem real: envia CONSISTENCY_CHECK para cada servidor e verifica se todos têm mesma contagem
    public void checkConsistencyWithServers() {
        try {
            int[] lineCounts = new int[PORTS.length];
            boolean allResponded = true;

            // Solicita contagem de linhas de cada servidor
            for (int i = 0; i < PORTS.length; i++) {
                try {
                    lineCounts[i] = getLineCountFromServer(PORTS[i]);
                } catch (Exception e) {
                    System.err.println("[ConsistencyMonitor] Server " + PORTS[i] + " is OFFLINE: " + e.getMessage());
                    serverRegistry.markServerUnavailable(PORTS[i]);
                    allResponded = false;
                    break;
                }
            }

            // Se todos responderam e contagens são iguais, sistema é consistente
            if (allResponded && lineCounts[0] == lineCounts[1] && lineCounts[1] == lineCounts[2]) {
                setSystemConsistent(true);
                // Marca servidores como disponíveis novamente se ainda não estão
                for (int port : PORTS) {
                    serverRegistry.markServerAvailable(port);
                }
                System.out.println("[ConsistencyMonitor] All servers consistent with " + lineCounts[0] + " lines");
            } else {
                setSystemConsistent(false);
                if (allResponded) {
                    System.out.println("[ConsistencyMonitor] Inconsistency detected: " + 
                        lineCounts[0] + " vs " + lineCounts[1] + " vs " + lineCounts[2]);
                }
            }
        } catch (Exception e) {
            System.err.println("[ConsistencyMonitor] Error during consistency check: " + e.getMessage());
            setSystemConsistent(false);
        }
    }
    
    // Envia CONSISTENCY_CHECK e recebe ConsistencyResponse com timeout
    private int getLineCountFromServer(int port) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            // Define timeout para socket
            socket.setSoTimeout(TIMEOUT_MS);
            
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                Message msg = new Message(Message.Type.CONSISTENCY_CHECK);
                msg.senderPort = 9000;
                out.writeObject(msg);
                out.flush();
                
                ConsistencyResponse response = (ConsistencyResponse) in.readObject();
                // Se conseguiu responder, marca como disponível
                serverRegistry.markServerAvailable(port);
                return response.lineCount;
            }
        } catch (java.net.SocketTimeoutException e) {
            serverRegistry.markServerUnavailable(port);
            throw new Exception("Timeout connecting to server " + port + ": " + e.getMessage());
        }
    }
}