package loadbalancer;

import common.Message;
import java.io.*;
import java.net.*;

public class LoadBalancer {
    private final ServerRegistry serverRegistry;
    private final QueueManager queueManager;
    private final ConsistencyMonitor consistencyMonitor;
    private final RequestHandler writeHandler;
    private final RequestHandler readHandler;
    
    public LoadBalancer() {
        this.serverRegistry = new ServerRegistry();
        this.queueManager = new QueueManager();
        this.consistencyMonitor = new ConsistencyMonitor(serverRegistry);
        
        // Inicializa handlers
        this.writeHandler = new WriteRequestHandler(queueManager, consistencyMonitor);
        this.readHandler = new ReadRequestHandler();
        
        // Inicia monitor de consistência
        startConsistencyMonitorThread();
    }
    
    public void start() throws IOException {
        System.out.println("[LoadBalancer] Starting on port 9000");
        System.out.println("[LoadBalancer] Servers: 9101, 9102, 9103");
        
        try (ServerSocket serverSocket = new ServerSocket(9000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            Message message = (Message) in.readObject();
            
            // Marca origem
            message.senderPort = 9000;

            // Debug marker: mostra início claro de processamento da requisição
            System.out.println("###########clienteReq#" + message.requestId + "###########");
            
            // Processa
            if (message.type == Message.Type.WRITE) {
                writeHandler.handleRequest(message, serverRegistry);
            } else if (message.type == Message.Type.READ) {
                readHandler.handleRequest(message, serverRegistry);
            }
            
            // FECHA imediatamente (não envia nada de volta)
            clientSocket.close();
            
        } catch (Exception e) {
            System.err.println("[LoadBalancer] Error handling client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignora
            }
        }
    }
    
    private void startConsistencyMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    consistencyMonitor.checkConsistencyWithServers();
                    
                    // Se voltou a ser consistente, processa fila pendente
                    if (consistencyMonitor.isSystemConsistent() && queueManager.hasPendingWrites()) {
                        queueManager.processAllPending(writeHandler, serverRegistry);
                    }
                    
                    Thread.sleep(2000); // Verifica a cada 2 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    
    public static void main(String[] args) throws IOException {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.start();
    }
}