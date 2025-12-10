package server;

import common.Message;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsistencyManager {

    // Fila para represamento de requisições de escrita (FIFO)
    public static final BlockingQueue<WriteWorker> writeQueue = new LinkedBlockingQueue<>();
    
    // Flag de controle de exclusão mútua, sincronizada pelo método checkAndReleaseNextWorker.
    private static volatile boolean isWriting = false; 
    
    // Flag que indica se houve uma falha de sincronização. Usado pela Thread de Recuperação.
    private static volatile boolean syncFailed = false;

    private static int myPort;
    private static RecoveryThread recoveryThread;

    public static void init(int port) {
        myPort = port;
        
        // Inicia a thread de recuperação ao iniciar o servidor
        recoveryThread = new RecoveryThread();
        recoveryThread.start();
    }

    // Método Sincronizado: Garante que apenas uma thread possa verificar o estado e reservar o lock.
    public static synchronized void checkAndReleaseNextWorker() { 
        if (!isWriting) {
            WriteWorker nextWorker = writeQueue.poll();
            if (nextWorker != null) {
                isWriting = true; // Reserva o lock de escrita
                System.out.println("[ConsistencyManager] Liberando Worker para escrita.");
                nextWorker.start(); // Inicia o processamento da escrita
                syncFailed = false; // Se liberou, não estamos mais em modo de falha
            }
        }
    }

    // Libera o lock e tenta iniciar o próximo worker se a sincronização for bem-sucedida.
    public static synchronized void releaseWritingLock(boolean syncSuccess) { 
        // A Exclusão Mútua só é liberada se o processo de escrita E sincronização foi um sucesso.
        if (syncSuccess) {
            isWriting = false;
            System.out.println("[ConsistencyManager] Lock liberado. Próximo worker será checado.");
            checkAndReleaseNextWorker(); // Tenta liberar o próximo da fila
        } else {
            // Se falhou, mantemos o lock (isWriting permanece true), e ativamos a flag de falha.
            syncFailed = true;
            System.err.println("!!! Sincronização FALHOU. Lock mantido. Recovery Thread ativada. !!!");
        }
    }

    // Implementação do 2PC Simplificado: Tenta sincronizar e espera ACK.
    // Retorna true se todos os ACKs forem recebidos.
    public static boolean checkAndSync(String line, int originPort) {
        boolean success = true;
        
        // As portas dos outros servidores
        int[] otherPorts = {9101, 9102, 9103};
        
        for (int port : otherPorts) {
            if (port != originPort) {
                success &= sendSync(line, port);
            }
        }
        return success;
    }
    
    // Envia a mensagem SYNC_WRITE e espera o ACK de volta
    private static boolean sendSync(String line, int port) {
    	Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            s = new Socket("localhost", port);
            
            // 1. CRIAÇÃO do OUTPUT Stream (Escreve o Header para o Servidor de Destino)
            out = new ObjectOutputStream(s.getOutputStream());
            
            // 2. Envia o SYNC_WRITE (o Servidor de Destino o lerá)
            Message msg = new Message(Message.Type.SYNC_WRITE);
            msg.line = line;
            out.writeObject(msg);
            out.flush(); // Garante que a requisição seja enviada

            // 3. CRIAÇÃO do INPUT Stream: 
            // Somente agora o Servidor de Origem espera a resposta. 
            // O Servidor de Destino já leu o SYNC_WRITE e está prestes a escrever o ACK,
            // garantindo que o cabeçalho de resposta esteja disponível.
            in = new ObjectInputStream(s.getInputStream());
            
            // 4. Espera pelo ACK
            Message response = (Message) in.readObject();
            
            if (response.type == Message.Type.ACK) {
                System.out.println("-> ACK recebido de " + port);
                return true;
            } else {
                System.err.println("-> Erro: Resposta inesperada de " + port);
                return false;
            }
        } catch (Exception e) {
            System.err.println("-> Falha de conexão/sincronização com servidor na porta " + port + ": " + e.getMessage());
            return false;
        } finally {
            // Garante que todos os recursos sejam fechados
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (s != null) s.close(); } catch (Exception e) {}
        }
    }    

    // Thread para monitorar e tentar a recuperação após uma falha de sincronização.
    private static class RecoveryThread extends Thread {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000); // Tenta a cada 5 segundos
                    
                    if (syncFailed) {
                        System.out.println("[RecoveryThread] Tentando retomar o estado de consistência...");
                        
                        // Tenta se comunicar com todos os servidores. Se todos estiverem OK, 
                        // isso indica que a rede/servidores estão de volta.
                        if (checkAllServersAvailable()) {
                            System.out.println("[RecoveryThread] Todos os servidores online. Retomando processamento.");
                            // Força a liberação, pois a falha de sincronização foi resolvida (o servidor voltou)
                            // A chamada ao releaseWritingLock forçará a tentativa de liberar o próximo worker.
                            // Nota: Aqui não passamos 'true', pois a escrita anterior falhou.
                            // A única forma de sair do estado travado é chamar checkAndReleaseNextWorker() diretamente
                            // e resetar syncFailed.
                            syncFailed = false; 
                            checkAndReleaseNextWorker(); 
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Verifica se é possível abrir um socket com todos os outros servidores.
        private boolean checkAllServersAvailable() {
            int[] allPorts = {9101, 9102, 9103};
            for (int port : allPorts) {
                if (port != myPort) {
                    try (Socket s = new Socket("localhost", port)) {
                        // Conexão bem-sucedida
                    } catch (Exception e) {
                        return false; // Falha na conexão com algum servidor
                    }
                }
            }
            return true;
        }
    }
}