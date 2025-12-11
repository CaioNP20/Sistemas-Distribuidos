package common;

import java.io.Serializable;

/**
    * Classe para representar as mensagens que são trocadas no sistema.
    * Ela é um interface Serializable porque a comunicação no sistema é via socket TCP, que permite apenas bytes.
*/ 
 

public class Message implements Serializable {
   
	private static final long serialVersionUID = 1L;

    // Enum que define os tipos de mensagens suportadas pelo sistema.
     
	public enum Type {
        WRITE,          //Usada na requisição de escrita
        READ,           //Usada na requisição de leitura
        SYNC_WRITE,     //Msg de sincronização entre os servidores 
        COMMIT_WRITE,   //Confirmação de um commit de uma escrita
        ACK             //Confirmação de recebimento
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
