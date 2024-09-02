package Preprocess;

import Configure.RuntimeConfig;
import com.github.gumtreediff.client.Run;
import org.springframework.ui.context.Theme;

import java.io.*;


public class Decompile {
    public static void decmp(String classFile) throws IOException, InterruptedException {
//        String cmd = "java -jar /home/hjf/Downloads/PATEN-master/lib/procyon-decompiler-0.5.36.jar " + classFile + " > " + RuntimeConfig.runtimeTfile;
        String cmd = "java -jar /home/hjf/experiment/paten_mvn_exp/procyon-decompiler-0.5.36.jar " + classFile + " > " + RuntimeConfig.runtimeTfile;
        Process process;
        process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
        errorGobbler.start();
        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream(), "STDOUT");
        // kick off stdout
        outGobbler.start();
        process.waitFor();

//        try{
//            process.waitFor();
//        }catch (Exception e){
//            System.out.println(e);
//        }

//        try {
//            Process p;
//            p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()), 4096);
//            while (br.readLine() != null) {}
//        } catch (Exception e) {}
    }


    public static void unzip(String tPath) throws IOException {
//        File f = new File(tPath);
//        if(!f.exists()){
//            f.mkdir();
//        }
        String cmd = "unzip -o -d " + RuntimeConfig.runtimeProj + " " + tPath;
//        System.out.println(cmd);
//        Runtime.getRuntime().exec(cmd);
        try {
            Process p;
            p = Runtime.getRuntime().exec(cmd);
            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
            errorGobbler.start();
            StreamGobbler outGobbler = new StreamGobbler(p.getInputStream(), "STDOUT");
            // kick off stdout
            outGobbler.start();
            p.waitFor();
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()), 4096);
//            while (br.readLine() != null) {}
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
