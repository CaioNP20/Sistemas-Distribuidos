package server;

import java.io.*;

public class FileUtils {

    public static synchronized void appendLine(String fileName, String line) throws Exception {
        FileWriter fw = new FileWriter(fileName, true);
        fw.write(line + "\n");
        fw.close();
    }

    public static synchronized int countLines(String fileName) throws Exception {
        File f = new File(fileName);
        if (!f.exists()) return 0;

        BufferedReader br = new BufferedReader(new FileReader(f));
        int c = 0;
        while (br.readLine() != null) c++;
        br.close();
        return c;
    }
}
