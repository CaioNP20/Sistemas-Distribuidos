package server;

import common.ConsistencyManager;
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
        try {
            // Sleep 100-200ms (requisito do projeto)
            int sleepTime = 100 + new Random().nextInt(101);
            Thread.sleep(sleepTime);
            
            // Calcula MDC
            int mdc = MDCUtils.mdc(msg.value1, msg.value2);
            String line = "The GCD between " + msg.value1 + " and " + msg.value2 + " is " + mdc;
            
            // Synchronized write (ensures atomicity per server)
            synchronized (FileUtils.class) {
                // Escreve no arquivo local
                FileUtils.appendLine(fileName, line);
                System.out.println("[WriteWorker] (port " + port + ") Wrote: " + line + " (requestId=" + msg.requestId + ")");
                
                // Propaga escrita para os outros servidores (silent)
                try {
                    ConsistencyManager.syncWithOthers(line, port);
                    // Small delay to ensure sync messages are processed
                    Thread.sleep(50);
                } catch (Exception e) {
                    System.err.println("[WriteWorker] Failed to sync with others (requestId=" + msg.requestId + "): " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] WriteWorker FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}