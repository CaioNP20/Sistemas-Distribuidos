package loadbalancer;

import common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class LoadBalancer {

    static String[] servers = {"localhost", "9101", "9102", "9103"};
    static Random r = new Random();

    public static void main(String[] args) throws Exception {
        try (ServerSocket ss = new ServerSocket(9000)) {
			System.out.println("[Balanceador] rodando...");

			while (true) {
			    Socket client = ss.accept();
			    ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			    Message msg = (Message) in.readObject();

			    if (msg.type == Message.Type.WRITE) {
			        // envia para 1 servidor aleatório
			        int serverPort = 9101 + r.nextInt(3);
			        sendToServer(msg, serverPort);
			    }
			    else if (msg.type == Message.Type.READ) {
			        // broadcast
			        sendToServer(msg, 9101);
			        sendToServer(msg, 9102);
			        sendToServer(msg, 9103);
			    }

			    client.close();
			}
		}
    }

    static void sendToServer(Message msg, int port) throws Exception {
        Socket s = new Socket("localhost", port);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(msg);
        s.close();
    }
}
