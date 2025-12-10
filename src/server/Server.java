package server;

import common.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(args[0]); // 9101, 9102, 9103
        String fileName = "server_" + port + ".txt";

        ConsistencyManager.init(port);

        try (ServerSocket ss = new ServerSocket(port)) {
			System.out.println("[Servidor "+port+"] iniciado.");

			while (true) {
			    Socket client = ss.accept();

			    new Thread(() -> {
			        try {
			            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			            Message msg = (Message) in.readObject();

			            if (msg.type == Message.Type.WRITE) {
			                // NOVO: Cria o worker, enfileira, e tenta liberar o próximo worker
			                WriteWorker worker = new WriteWorker(msg, fileName, port);
			                ConsistencyManager.writeQueue.put(worker); 
                            ConsistencyManager.checkAndReleaseNextWorker();
                            
			            }
			            else if (msg.type == Message.Type.READ) {
			                new ReadWorker(fileName).start();
			            }
			            else if (msg.type == Message.Type.SYNC_WRITE) {
			                // NOVO: Escreve no arquivo e envia ACK de volta ao servidor de origem
			                FileUtils.appendLine(fileName, msg.line);
			                
			                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
			                out.writeObject(new Message(Message.Type.ACK));
			                out.flush();
			            }

			        } catch (Exception e) {
			            e.printStackTrace();
			        } finally {
                        try { client.close(); } catch (Exception e) {}
                    }
			    }).start();
			}
		}
    }
}
