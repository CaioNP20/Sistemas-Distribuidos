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
        boolean syncSuccess = false; // Inicializa o status de sucesso
        String line = null;
        
        try {
            Thread.sleep(100 + new Random().nextInt(101));

            int a = msg.value1;
            int b = msg.value2;
            int mdc = MDCUtils.mdc(a, b);

            line = "O MDC entre " + a + " e " + b + " é " + mdc;

            // 1. Tenta sincronizar com os outros 2 servidores e espera pelo ACK (PREPARE/COMMIT)
            syncSuccess = ConsistencyManager.checkAndSync(line, port);

            if (syncSuccess) {
                // 2. Se 100% dos servidores confirmaram (ACK), escreve no arquivo local.
                // A atomicidade é garantida: todos online OU nenhum escreve.
                FileUtils.appendLine(fileName, line);
                System.out.println("[ESCRITA] Escrita concluída com sucesso no servidor " + port);
            } else {
                // 3. Se falhou (algum servidor indisponível), não faz a escrita local.
                System.err.println("[ESCRITA] Falha na sincronização. Escrita abortada no servidor " + port);
            }

        } catch (Exception e) {
            e.printStackTrace();
            syncSuccess = false; // Força falha em caso de exceção
        } finally {
            // 4. Libera o lock de escrita (ou o mantém, se syncSuccess for false)
            ConsistencyManager.releaseWritingLock(syncSuccess); 
        }
    }
}
