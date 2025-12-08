package server;

public class ReadWorker extends Thread {

    String fileName;

    public ReadWorker(String fileName) {
        this.fileName = fileName;
    }

    public void run() {
        try {
            int lines = FileUtils.countLines(fileName);
            System.out.println("[LEITURA] Arquivo possui " + lines + " linhas.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
