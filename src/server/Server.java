package server;

import common.Message;

import java.io.ObjectInputStream;
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
			                new WriteWorker(msg, fileName, port).start();
			            }
			            else if (msg.type == Message.Type.READ) {
			                new ReadWorker(fileName).start();
			            }
			            else if (msg.type == Message.Type.SYNC_WRITE) {
			                // sincronização automática
			                FileUtils.appendLine(fileName, msg.line);
			            }

			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			    }).start();
			}
		}
    }
}
