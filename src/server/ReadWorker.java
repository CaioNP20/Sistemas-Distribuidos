package server;

/**
 * Worker para processar leituras de forma assíncrona.
 * 
 * Executa em thread separada para contar linhas de arquivo
 * sem bloquear o servidor principal.
 */

public class ReadWorker extends Thread {

    String fileName;

    public ReadWorker(String fileName) {
        this.fileName = fileName;
    }

    // O método é executado quando a thread é iniciada.
    // Faz a contagem de linhas do arquivo e imprime no console.
    @Override
    public void run() {
        try {
            int lines = FileUtils.countLines(fileName);
            System.out.println("[LEITURA] Arquivo possui " + lines + " linhas.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
