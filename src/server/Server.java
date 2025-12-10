package server;

import common.Message;
import common.ConsistencyManager;
import common.ConsistencyResponse;

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
						// received message (silent)

			            // No Server.java, quando recebe WRITE:
						if (msg.type == Message.Type.WRITE) {
							// start write worker
							Message copy = new Message(Message.Type.WRITE, msg.value1, msg.value2);
							copy.requestId = msg.requestId;
							copy.senderPort = msg.senderPort;
							new WriteWorker(copy, fileName, port).start();
						}
			            else if (msg.type == Message.Type.READ) {
			                new ReadWorker(fileName).start();
			            }
						else if (msg.type == Message.Type.SYNC_WRITE) {
							// sincronização automática
							synchronized (FileUtils.class) {
								FileUtils.appendLine(fileName, msg.line);
								System.out.println("[Server " + port + "] Applied SYNC_WRITE (requestId=" + msg.requestId + "): " + msg.line);
							}
						}
						else if (msg.type == Message.Type.CONSISTENCY_CHECK) {
							// responder com contagem de linhas
							int lineCount = FileUtils.countLines(fileName);
							System.out.println("[Server " + port + "] CONSISTENCY_CHECK received from senderPort=" + msg.senderPort + " (requestId=" + msg.requestId + ") -> lines=" + lineCount);
							ConsistencyResponse response = new ConsistencyResponse(port, lineCount);
							try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {
								out.writeObject(response);
							}
						}

			        } catch (Exception e) {
			            e.printStackTrace();
			        } finally {
			            try {
			                client.close();
			            } catch (Exception e) {
			                // ignore
			            }
			        }
			    }).start();
			}
		}
    }
}