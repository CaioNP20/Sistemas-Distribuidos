package common;

import java.io.Serializable;

public class Message implements Serializable {
   
	private static final long serialVersionUID = 1L;

	public enum Type {
        WRITE,
        READ,
        SYNC_WRITE,
        COMMIT_WRITE,
        ACK
    }

    public Type type;
    public int value1;
    public int value2;
    public String line;  // usado em sincronização

    public Message(Type type) {
        this.type = type;
    }

    public Message(Type type, int v1, int v2) {
        this.type = type;
        this.value1 = v1;
        this.value2 = v2;
    }
}
