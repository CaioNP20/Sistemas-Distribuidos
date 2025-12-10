package common;

public class ConsistencyManager {

    static final Object lock = new Object();
    static int lastSizes[] = new int[4];
    static int myPort;

    public static void init(int port) {
        myPort = port;
    }

    static void sendSync(String line, int port, int originPort) throws Exception {
        Message msg = new Message(Message.Type.SYNC_WRITE);
        msg.line = line;
        msg.senderPort = originPort;
        common.SocketUtils.sendMessage("localhost", port, msg);
    }

    public static void syncWithOthers(String line, int originPort) throws Exception {
        int[] ports = new int[]{9101, 9102, 9103};
        for (int p : ports) {
            if (p != originPort) {
                try {
                    sendSync(line, p, originPort);
                } catch (Exception e) {
                    System.err.println("[ConsistencyManager] Failed to sync to " + p + ": " + e.getMessage());
                }
            }
        }
    }
}
