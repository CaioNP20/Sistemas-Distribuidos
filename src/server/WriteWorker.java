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
        try {
            Thread.sleep(100 + new Random().nextInt(101));

            int a = msg.value1;
            int b = msg.value2;
            int mdc = MDCUtils.mdc(a, b);

            String line = "O MDC entre " + a + " e " + b + " é " + mdc;

            // só escreve quando os 3 arquivos estiverem consistentes
            ConsistencyManager.waitConsistency();

            // escreve no arquivo local
            FileUtils.appendLine(fileName, line);

            // sincroniza com os outros servidores
            ConsistencyManager.syncWithOthers(line, port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
