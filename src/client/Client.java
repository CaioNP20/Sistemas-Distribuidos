package client;

import common.Message;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;

public class Client {
    public static void main(String[] args) {

        Random r = new Random();

        try (FileWriter log = new FileWriter("client_log.txt", true)) {
            while (true) { // Loop infinito enviando requisições
                // O try-with-resources para garante que o socket seja fechado automaticamente ao final de cada interação
                try (
					//Faz a conexão do cliente com o LoadBalancer
                    Socket s = new Socket("localhost", 9000);
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())
                ) {
                    boolean isWrite = r.nextBoolean();

                    // Verifica a requisição
                    if (isWrite) {
                        // Faz o sorteio de 2 numeros entre 2 e 1.000.000
                        int a = r.nextInt(999999) + 2;
                        int b = r.nextInt(999999) + 2;

                        // Cria e envia uma nova mensagem com os dois num sorteados
                        Message msg = new Message(Message.Type.WRITE, a, b);
                        out.writeObject(msg);
                        out.flush(); // garante envio imediato

                        // Salva os numeros gerados no arquivo client_log
                        log.write(a + "," + b + "\n");
                        log.flush();

                    } else {
                        // Faz a requisição de leitura
                        Message msg = new Message(Message.Type.READ);
                        out.writeObject(msg);
                        out.flush(); //garante envio imediato
                    }

                } catch (IOException e) {
                    System.err.println("Erro de conexão: " + e.getMessage());
                    // Espera um pouco antes de tentar reconectar
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue; 
                }

                // O cliente dorme entre 20 a 50ms depois de cada requisição
                try {
                    Thread.sleep(20 + r.nextInt(31));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Cliente interrompido.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir arquivo de log: " + e.getMessage());
        }
    }
}