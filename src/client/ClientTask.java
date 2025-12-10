package client;

import common.Message;
import java.io.*;
import java.net.Socket;
import java.util.Random;

public class ClientTask {
    private String serverHost;
    private int serverPort;
    private String logFilename;
    private Random random;
    private FileWriter logWriter;
    
    public ClientTask(String serverHost, int serverPort, String logFilename) throws IOException {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.logFilename = logFilename;
        this.random = new Random();
        this.logWriter = new FileWriter(logFilename, true); // APPEND
    }
    
    public void executeRequest() throws IOException, InterruptedException {
        // Socket SIMPLES: apenas envia, NÃO tenta ler resposta
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            boolean isWrite = random.nextBoolean();
            
            if (isWrite) {
                int num1 = generateRandomNumber();
                int num2 = generateRandomNumber();
                
                Message msg = new Message(Message.Type.WRITE, num1, num2);
                out.writeObject(msg);
                
                // LOG IMEDIATO (garante escrita)
                logWriteRequest(num1, num2);
                
                System.out.println("[Client] Write sent: " + num1 + ", " + num2);
                
            } else {
                Message msg = new Message(Message.Type.READ);
                out.writeObject(msg);
                System.out.println("[Client] Read sent");
            }
            
            // Fecha socket (NÃO tenta ler resposta)
            
        } catch (IOException e) {
            // Relança a exceção para tratamento no Client
            throw e;
        }
    }
    
    private int generateRandomNumber() {
        return random.nextInt(999998) + 2; // 2-1,000,000
    }
    
    private void logWriteRequest(int num1, int num2) throws IOException {
        synchronized (logWriter) {
            logWriter.write(num1 + "," + num2 + "\n");
            logWriter.flush(); // FORÇA ESCRITA IMEDIATA
        }
    }
    
    public int getRandomSleepTime() {
        return 20 + random.nextInt(31); // 20-50ms
    }
    
    public void close() throws IOException {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}