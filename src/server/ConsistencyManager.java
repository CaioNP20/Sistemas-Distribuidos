package server;

import common.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Classe que gerencia consistência para controle de concorrência e sincronização
 * entre servidores em um sistema distribuído.
 */

public class ConsistencyManager {

    //Fila para armazenar operações de escrita pendentes (garante que seja escrito na ordem de chegada)
    public static final BlockingQueue<WriteWorker> writeQueue = new LinkedBlockingQueue<>();
      
    //Foi usado volatile pq garante visibilidade entre threads
    private static volatile boolean isWriting = false; //Flags que indica se WRITE está em andamento
    private static volatile boolean syncFailed = false; //Flag para indicar se a ultima sincronização falhou

    private static int myPort;

    //Thread que monitora e tenta recuperar falhas de sincronização.
    private static RecoveryThread recoveryThread;

    public static void init(int port) {
        myPort = port;
        
        //Dá inicio a thread de recuperação 
        recoveryThread = new RecoveryThread();
        recoveryThread.start();
    }

    // --- Métodos de Controle de Exclusão Mútua e Fila ---


    //Verifica se pode liberar o prox worker da fila para escrita (só libera se n tiver nenhum em andamento)
    public static synchronized void checkAndReleaseNextWorker() { 
        if (!isWriting) {
            // Tenta pegar próximo worker da fila (não bloqueante)
            WriteWorker nextWorker = writeQueue.poll();

            if (nextWorker != null) {
                isWriting = true; // "Trava" o sistema para escrita
                System.out.println("[ConsistencyManager] Liberando Worker para escrita.");
                syncFailed = false; // Reseta flag de falha
                nextWorker.start(); //Inicia um novo processamento
            }
        }
    }

    //Faz a liberação ou mantem o lock de escrita baseado no resultado de sincronização
    public static synchronized void releaseWritingLock(boolean syncSuccess) { 
        if (syncSuccess) {
            isWriting = false; // Libera o lock para a prox escrita
            System.out.println("[ConsistencyManager] Lock liberado. Próximo worker será checado.");
            checkAndReleaseNextWorker(); //Tenta proxessar o prox
        } else {
            syncFailed = true; //Se falhar, mantem o lock ativado e aciona recovery
            System.err.println("!!! Sincronização FALHOU. Lock mantido. Recovery Thread ativada. !!!");
        }
    }

    // --- FASE 1: PREPARE/VOTE  ---

    // Envia SYNC_WRITE para os outros servidores e espera um ACK.
    public static boolean checkAndSync(String line, int originPort) {
        boolean success = true;
        int[] allPorts = {9101, 9102, 9103};
        
        for (int port : allPorts) {
            if (port != originPort) {
                success &= sendPrepareMessage(line, port); //Faz pergunta a cada servidor, verificando se pode escrever
            }
        }
        return success;
    }
    
    // Envia a mensagem SYNC_WRITE e espera ACK de volta.
    private static boolean sendPrepareMessage(String line, int port) {
        Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            s = new Socket("localhost", port);
            
            // 1. Envia o SYNC_WRITE 
            out = new ObjectOutputStream(s.getOutputStream());
            Message msg = new Message(Message.Type.SYNC_WRITE);
            msg.line = line;
            out.writeObject(msg);
            out.flush();

            // 2. Espera pelo ACK 
            in = new ObjectInputStream(s.getInputStream());
            Message response = (Message) in.readObject();
            
            if (response.type == Message.Type.ACK) {
                return true; //Servidor confirmou, pode escrever
            } else {
                System.err.println("-> Erro: Resposta inesperada de " + port);
                return false;
            }
        } catch (Exception e) {
            System.err.println("-> Falha de conexão/sincronização (PREPARE) com servidor na porta " + port);
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e) {}

            try {
                if (out != null) out.close();
            } catch (Exception e) {}

            try {
                if (s != null) s.close();
            } catch (Exception e) {}
        }

    }

    // --- FASE 2: COMMIT ---

    // Envia COMMIT_WRITE para os outros servidores.
    public static void sendCommit(String line, int originPort) {
        int[] allPorts = {9101, 9102, 9103};
        for (int port : allPorts) {
            if (port != originPort) {
                sendCommitMessage(line, port);
            }
        }
    }

    // Envia a mensagem COMMIT_WRITE (Só é chamado após todos servidores terem ACK na fase 1)
    private static void sendCommitMessage(String line, int port) {
        try (Socket s = new Socket("localhost", port);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {

            //Cria a msg de COMMIT com os dados 
            Message msg = new Message(Message.Type.COMMIT_WRITE);
            msg.line = line;
            out.writeObject(msg);
            out.flush();
            
        } catch (Exception e) {
            // Se falhar aqui, a RecoveryThread precisará corrigir a inconsistência no futuro.
            System.err.println("-> Falha no commit (FASE 2) com servidor na porta " + port);
        }
    }

    // --- Thread de Recuperação ---
    
    //Thread para detectar e recuperar falhas (como falhas em servidores...)
    private static class RecoveryThread extends Thread {

        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000); // Verifica a cada 5 seg se há falhas pendentes
                    
                    //Só age se houve falha na uultima sincronização
                    if (syncFailed) {
                        System.out.println("[RecoveryThread] Tentando retomar o estado de consistência...");
                        
                        //Verifica se todos os servidore estão online novamente
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
        
        //Verifica se todos os servidores estão respondendo (true: se todos os servidores estão acessiveis via socket)
        private boolean checkAllServersAvailable() {
            int[] allPorts = {9101, 9102, 9103};
            for (int port : allPorts) {
                if (port != myPort) {
                    try (Socket s = new Socket("localhost", port)) {
                        // Conexão bem-sucedida
                    } catch (Exception e) {
                        return false; //server off
                    }
                }
            }
            return true;
        }
    }
}