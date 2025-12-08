package server;

import common.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConsistencyManager {

    static final Object lock = new Object();
    static int lastSizes[] = new int[4];
    static int myPort;

    public static void init(int port) {
        myPort = port;
    }

    public static void waitConsistency() throws Exception {
        while (true) {
            int s1 = FileUtils.countLines("server_9101.txt");
            int s2 = FileUtils.countLines("server_9102.txt");
            int s3 = FileUtils.countLines("server_9103.txt");

            if (s1 == s2 && s2 == s3) return;

            Thread.sleep(50);
        }
    }

    static void sendSync(String line, int port) throws Exception {
        Socket s = new Socket("localhost", port);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        Message msg = new Message(Message.Type.SYNC_WRITE);
        msg.line = line;
        out.writeObject(msg);
        s.close();
    }

    public static void syncWithOthers(String line, int originPort) throws Exception {
        if (originPort != 9101) sendSync(line, 9101);
        if (originPort != 9102) sendSync(line, 9102);
        if (originPort != 9103) sendSync(line, 9103);
    }
}
