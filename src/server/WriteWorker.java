package server;

import common.Message;

import java.util.Random;

public class WriteWorker extends Thread {

    Message msg;
    String fileName;
    int port;

    public WriteWorker(Message msg, String fileName, int port) {
        this.msg = msg;
        this.fileName = fileName;
        this.port = port;
    }

    public void run() {
        boolean prepareSuccess = false; 
        boolean commitSuccess = false; // Novo flag
        String line = null;
        
        try {
            Thread.sleep(100 + new Random().nextInt(101));

            int a = msg.value1;
            int b = msg.value2;
            int mdc = MDCUtils.mdc(a, b);

            line = "O MDC entre " + a + " e " + b + " é " + mdc;

            // FASE 1: PREPARE/VOTE (Envia SYNC_WRITE, espera ACK dos outros dois)
            prepareSuccess = ConsistencyManager.checkAndSync(line, port);

            if (prepareSuccess) {
                // FASE 2: COMMIT - Envia COMMIT_WRITE e espera ACK de volta de TODOS os remotos
                commitSuccess = ConsistencyManager.syncAndCommit(line, port);
                
                if (commitSuccess) {
                    // SUCESSO! Apenas agora o primário comita localmente.
                    FileUtils.appendLine(fileName, line);
                    System.out.println("[ESCRITA] Escrita concluída com sucesso no servidor " + port);
                    
                    // Libera o lock
                    ConsistencyManager.releaseWritingLock(true); 
                } else {
                    // ABORT: Falha no Commit (Um servidor falhou em confirmar a escrita)
                    System.err.println("[ESCRITA] Falha na fase de COMMIT. Escrita abortada no servidor " + port);
                    // Notifica a falha para o lock (mantém o lock ativado e aciona a RecoveryThread)
                    ConsistencyManager.releaseWritingLock(false);
                }
            } else {
                // ABORT: Falha na votação (PREPARE). Nenhum servidor escreve.
                System.err.println("[ESCRITA] Falha na votação (PREPARE). Escrita abortada no servidor " + port);
                // Notifica a falha para o lock (mantém o lock ativado e aciona a RecoveryThread)
                ConsistencyManager.releaseWritingLock(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Garante que o lock seja liberado/mantido em modo de falha em caso de exceção.
            ConsistencyManager.releaseWritingLock(false); 
        } 
    }
}