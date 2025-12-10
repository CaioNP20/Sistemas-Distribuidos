package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        WRITE,
        READ,
        SYNC_WRITE,
        // Adicionais para controle:
        LOCK_REQUEST,      // para exclusão mútua distribuída
        LOCK_GRANTED,
        LOCK_RELEASE,
        CONSISTENCY_CHECK,
        CONSISTENCY_OK,
        ACK
    }

    public Type type;
    public int value1;
    public int value2;
    public String line;
    
    // Novos campos para controle:
    public String requestId;     // ID único para rastreamento
    public int senderPort;       // porta de quem enviou
    public boolean consistent;   // status de consistência
    public long timestamp;       // timestamp lógico/temporal da mensagem
    
    // Construtores existentes...
    public Message(Type type) {
        this.type = type;
        this.requestId = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Message(Type type, int v1, int v2) {
        this.type = type;
        this.value1 = v1;
        this.value2 = v2;
        this.requestId = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message[");
        sb.append("type=").append(type);
        sb.append(", requestId=").append(requestId);
        sb.append(", senderPort=").append(senderPort);
        if (type == Type.WRITE || type == Type.SYNC_WRITE) {
            sb.append(", values=(").append(value1).append(",").append(value2).append(")");
        }
        if (line != null) {
            sb.append(", line=\"").append(line).append("\"");
        }
        sb.append(", ts=").append(timestamp);
        sb.append("]");
        return sb.toString();
    }
}