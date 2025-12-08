package client;

import common.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.io.FileWriter;

public class Client {
    public static void main(String[] args) throws Exception {

        Random r = new Random();
        try (FileWriter log = new FileWriter("client_log.txt", true)) {
			while (true) {
			    Socket s = new Socket("localhost", 9000);
			    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

			    boolean isWrite = r.nextBoolean();

			    if (isWrite) {
			        int a = r.nextInt(999998) + 2;
			        int b = r.nextInt(999998) + 2;

			        Message msg = new Message(Message.Type.WRITE, a, b);
			        out.writeObject(msg);

			        log.write(a + "," + b + "\n");
			        log.flush();

			    } else {
			        Message msg = new Message(Message.Type.READ);
			        out.writeObject(msg);
			    }

			    s.close();

			    Thread.sleep(20 + r.nextInt(31));
			}
		}
    }
}
