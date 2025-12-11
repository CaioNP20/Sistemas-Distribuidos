package server;

import common.Message;

import java.util.Random;

/**
 * Worker para processar escritas de forma assíncrona.
 * 
 * Executa em thread separada para não bloquear o servidor principal
 * durante operações lentas (cálculo MDC, rede, disco).
 */

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
        String line = null;
        
        try {
            // Delay simula processamento
            Thread.sleep(100 + new Random().nextInt(101));

            int a = msg.value1;
            int b = msg.value2;
            int mdc = MDCUtils.mdc(a, b);

            line = "O MDC entre " + a + " e " + b + " é " + mdc;

            // FASE 1: PREPARE/VOTE - Verifica se os outros servidores estão prontos para o commit
            prepareSuccess = ConsistencyManager.checkAndSync(line, port);

            if (prepareSuccess) {
                //FASE 2: COMMIT: Se todos votaram ACK (COMMIT), prossegue para a escrita 
                
                // 1- Escreve no arquivo LOCAL (O primário deve comitar primeiro)
                FileUtils.appendLine(fileName, line);
                System.out.println("[ESCRITA] Escrita concluída com sucesso no servidor " + port);

                // 2- Envia COMMIT para os outros servidores
                ConsistencyManager.sendCommit(line, port);
                
                // 3- Notifica a liberação do lock (SUCESSO)
                ConsistencyManager.releaseWritingLock(true); 
                
            } else {
                // ABORT: Falha na votação. Ninguém escreve.
                System.err.println("[ESCRITA] Falha na votação (PREPARE). Escrita abortada no servidor " + port);
                // Notifica a falha para o lock (pode aciona a RecoveryThread)
                ConsistencyManager.releaseWritingLock(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Em caso de qualquer erro faz a liberação do lock com status de falha
            ConsistencyManager.releaseWritingLock(false); 
        } 
    }
}