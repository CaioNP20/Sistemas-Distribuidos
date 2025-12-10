package common;

import java.io.Serializable;

public class ConsistencyResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public int port;
    public int lineCount;
    
    public ConsistencyResponse(int port, int lineCount) {
        this.port = port;
        this.lineCount = lineCount;
    }
}
