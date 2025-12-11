package server;

/**
 *  Classe para calcular o MDC dos 2 números aleatorios gerados pelo Cliente
 */ 

public class MDCUtils {

    public static int mdc(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}