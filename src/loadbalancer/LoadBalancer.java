package loadbalancer;

import common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

// Classe do balanceador de carga que distribui as requisições do cliente entre os servidores.

public class LoadBalancer {

	//Lista de servidores disp. no sistema
    static String[] servers = {"localhost", "9101", "9102", "9103"};
    static Random r = new Random();

    public static void main(String[] args) throws Exception {
		//Cria socket para escutar conexões na porta 9000
		// O LoadBalancer é o único ponto de entrada conhecido pelos clientes
        try (ServerSocket ss = new ServerSocket(9000)) {
			System.out.println("[Balanceador] rodando...");

			while (true) {
			    Socket client = ss.accept();
				
				//Lê a mensagem do cliente
			    ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			    Message msg = (Message) in.readObject();

				//Envia a escrita para um servidor aleatorio
			    if (msg.type == Message.Type.WRITE) {
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

	//Encaminha uma mensagem para um servidor especifico
    static void sendToServer(Message msg, int port) throws Exception {
        Socket s = new Socket("localhost", port);

		// Serializa e envia a mensagem
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(msg);
        s.close();// Fecha conexão imediatamente após envio
    }
}