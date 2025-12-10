package server;

import common.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsistencyManager {

    public static final BlockingQueue<WriteWorker> writeQueue = new LinkedBlockingQueue<>();
    
    private static volatile boolean isWriting = false; 
    private static volatile boolean syncFailed = false;

    private static int myPort;
    private static RecoveryThread recoveryThread;

    public static void init(int port) {
        myPort = port;
        
        recoveryThread = new RecoveryThread();
        recoveryThread.start();
    }

    // --- Métodos de Controle de Exclusão Mútua e Fila ---

    public static synchronized void checkAndReleaseNextWorker() { 
        if (!isWriting) {
            WriteWorker nextWorker = writeQueue.poll();
            if (nextWorker != null) {
                isWriting = true; // Reserva o lock de escrita
                System.out.println("[ConsistencyManager] Liberando Worker para escrita.");
                syncFailed = false; 
                nextWorker.start();
            }
        }
    }

    public static synchronized void releaseWritingLock(boolean syncSuccess) { 
        if (syncSuccess) {
            isWriting = false; // Libera o lock
            System.out.println("[ConsistencyManager] Lock liberado. Próximo worker será checado.");
            checkAndReleaseNextWorker(); 
        } else {
            syncFailed = true;
            System.err.println("!!! Sincronização FALHOU. Lock mantido. Recovery Thread ativada. !!!");
        }
    }

    // --- FASE 1: PREPARE/VOTE (checkAndSync) ---

    // Envia SYNC_WRITE para os outros 2 servidores e espera ACK.
    public static boolean checkAndSync(String line, int originPort) {
        boolean success = true;
        int[] allPorts = {9101, 9102, 9103};
        
        for (int port : allPorts) {
            if (port != originPort) {
                success &= sendPrepareMessage(line, port);
            }
        }
        return success;
    }
    
    // Envia a mensagem SYNC_WRITE e espera o ACK de volta.
    private static boolean sendPrepareMessage(String line, int port) {
        Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            s = new Socket("localhost", port);
            
            // 1. Envia o SYNC_WRITE (Prepare)
            out = new ObjectOutputStream(s.getOutputStream());
            Message msg = new Message(Message.Type.SYNC_WRITE);
            msg.line = line;
            out.writeObject(msg);
            out.flush();

            // 2. Espera pelo ACK (Vote Commit)
            in = new ObjectInputStream(s.getInputStream());
            Message response = (Message) in.readObject();
            
            if (response.type == Message.Type.ACK) {
                return true;
            } else {
                System.err.println("-> Erro: Resposta inesperada de " + port);
                return false;
            }
        } catch (Exception e) {
            System.err.println("-> Falha de conexão/sincronização (PREPARE) com servidor na porta " + port);
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (s != null) s.close(); } catch (Exception e) {}
        }
    }

    // --- FASE 2: COMMIT (sendCommit) ---
 // Envia COMMIT_WRITE para os outros 2 servidores e espera ACK de volta.
    public static boolean syncAndCommit(String line, int originPort) {
        boolean success = true;
        int[] allPorts = {9101, 9102, 9103};
        
        for (int port : allPorts) {
            if (port != originPort) {
                success &= sendCommitMessage(line, port); // Espera ACK de volta
            }
        }
        return success; // Só retorna true se TODOS os commits foram ACK'd
    }

    // Envia COMMIT_WRITE para os outros 2 servidores.
    public static void sendCommit(String line, int originPort) {
        int[] allPorts = {9101, 9102, 9103};
        for (int port : allPorts) {
            if (port != originPort) {
                sendCommitMessage(line, port);
            }
        }
    }
    

    // Envia a mensagem COMMIT_WRITE (Não espera resposta, commit é fire-and-forget após sucesso na fase 1)
    private static boolean sendCommitMessage(String line, int port) {
        Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            s = new Socket("localhost", port);
            
            // 1. Envia o COMMIT_WRITE
            out = new ObjectOutputStream(s.getOutputStream());
            Message msg = new Message(Message.Type.COMMIT_WRITE);
            msg.line = line;
            out.writeObject(msg);
            out.flush();

            // 2. Espera pelo ACK (Confirmação de que a escrita foi feita)
            in = new ObjectInputStream(s.getInputStream());
            Message response = (Message) in.readObject();
            
            if (response.type == Message.Type.ACK) {
                return true;
            } else {
                System.err.println("-> Erro: Resposta inesperada de " + port);
                return false;
            }
        } catch (Exception e) {
            System.err.println("-> Falha de conexão/commit com servidor na porta " + port);
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (s != null) s.close(); } catch (Exception e) {}
        }
    }

    // --- Recovery Thread ---
    
    private static class RecoveryThread extends Thread {
        // ... (o código da RecoveryThread permanece o mesmo)
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000); // Tenta a cada 5 segundos
                    
                    if (syncFailed) {
                        System.out.println("[RecoveryThread] Tentando retomar o estado de consistência...");
                        
                        if (checkAllServersAvailable()) {
                            System.out.println("[RecoveryThread] Todos os servidores online. Retomando processamento.");
                            
                            // Se todos voltaram, tentamos liberar o próximo item na fila.
                            syncFailed = false; 
                            checkAndReleaseNextWorker(); 
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        private boolean checkAllServersAvailable() {
            int[] allPorts = {9101, 9102, 9103};
            for (int port : allPorts) {
                if (port != myPort) {
                    try (Socket s = new Socket("localhost", port)) {
                        // Conexão bem-sucedida
                    } catch (Exception e) {
                        return false; 
                    }
                }
            }
            return true;
        }
    }
}