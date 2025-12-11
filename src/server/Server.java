package server;

import common.Message;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe do Servidor principal e é responsável por:
 * 
 *  - Escutar conexões de clientes em uma porta específica
 *  - Processar requisições de leitura e escrita
 *  - Coordenação com outros servidores via 2-Phase Commit 
 *  - Gerenciamento de concorrência através do ConsistencyManager

 */
public class Server {
    public static void main(String[] args) throws Exception { 

        // 1 - Obtém porta do argumento 
        int port = Integer.parseInt(args[0]); //9101, 9102, 9103
        
        // 2 - Cria o nome de arquivo para este servidor
        String fileName = "server_" + port + ".txt";
        
        // 3 - Inicializa gerenciador de consistência (controle de concorrência)
        ConsistencyManager.init(port);

        // 4 -Cria o servr para escutar conexões na porta que foi especificada
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[Servidor " + port + "] iniciado na porta " + port + ".");
            System.out.println("[Servidor " + port + "] Arquivo de dados: " + fileName);

            // 5 - Loop infinito para aceitar conexões de clientes
            while (true) {

                Socket client = ss.accept();
                
                // 6 - Para cada cliente é criado uma nova thread 
                new Thread(() -> {
                    try {
                        // 7 - Cria streams para a comunicação com o cliente
                        ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                        
                        // 8 É feito a leitura da mensagem do cliente (momento em que a msg é desserializada)
                        Message msg = (Message) in.readObject();

                        // 9 - Faz a verificação de acordo com o tipo e segue com o processo
                        if (msg.type == Message.Type.WRITE) {
                            // WRITE: Operação de escrita com controle de concorrência
                            
                            // É criado o worker que recebe a msg, um arquivo e a porta
                            WriteWorker worker = new WriteWorker(msg, fileName, port);
                            
                            // O worker é adicionado na fila de escritas 
                            ConsistencyManager.writeQueue.put(worker);
                            
                            // E é feita a verificação se pode processar a próxima escrita 
                            ConsistencyManager.checkAndReleaseNextWorker();
                            
                        } else if (msg.type == Message.Type.READ) {
                            // READ: Operação de leitura
                            // Inicia uma thread separada para contar as linhas do arquivo
                            new ReadWorker(fileName).start();
                            
                        } else if (msg.type == Message.Type.SYNC_WRITE) {
                            // SYNC_WRITE: FASE 1 do 2-Phase Commit (Prepare/Vote)
                            // O LoadBalancer faz uma "preparação" para escrever
                            
                            // Ele vota "OK" sem escrever ainda
                            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                            out.writeObject(new Message(Message.Type.ACK)); // Voto: OK para escrever
                            out.flush();
                            
                        } else if (msg.type == Message.Type.COMMIT_WRITE) {
                            // COMMIT_WRITE: FASE 2 do 2-Phase Commit (Commit)
                            // Recebe a ordem final do LoadBalancer e prossegue com a escrita
                            
                            // Executa a escrita final no arquivo local
                            FileUtils.appendLine(fileName, msg.line);
                            System.out.println("[SYNC] Commit concluído no servidor " + port);
                        }

                    } catch (Exception e) {
                        
                        System.err.println("[Servidor " + port + "] Erro ao processar client: " + e.getMessage());
                        e.printStackTrace();
                    } finally {         
                        try { 
                            client.close(); // Garante que socket do client seja fechado
                        } catch (Exception e) {
                            System.err.println("[Servidor " + port + "] Erro ao fechar socket: " + e.getMessage());
                        }
                    }
                }).start(); // Inicia a thread de atendimento
            }
        }        
    }
}