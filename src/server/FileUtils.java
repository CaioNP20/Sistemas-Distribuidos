package server;

import java.io.*;

/**
 * Classe para auxiliar operações de arquivos e sincroniza-los de forma segura em ambientes concorrentes.
 * Foi utilizado o modificador synchronized para garantir que apenas uma thread seja executada por vez
 */

public class FileUtils {

    //Adiciona uma linha ao final de um arquivo
    public static synchronized void appendLine(String fileName, String line) throws Exception {
        FileWriter fw = new FileWriter(fileName, true);
        fw.write(line + "\n");
        fw.close();
    }

    //Faz a contagem de linhas dentro dos arquivos

    public static synchronized int countLines(String fileName) throws Exception {
        File f = new File(fileName);
        //Verifica se o arquivo existe
        if (!f.exists()){
             return 0;
        }

        BufferedReader br = new BufferedReader(new FileReader(f));      
        int c = 0;

        //Varre todas as linhas do arquivo e incrementa no contador, depois fecha o arquivo e retorna a quantidade.
        while (br.readLine() != null){
            c++;
        }
        br.close();
        return c;
    }
}