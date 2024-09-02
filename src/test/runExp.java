package test;

import Configure.DataBaseconfig;
import Configure.RuntimeConfig;
import Preprocess.*;
import TokenAnalysis.main.newDecCheck;
import com.github.gumtreediff.client.Run;
import Gumtree.function.tools.getMethod;
import com.github.javaparser.Providers;

import java.io.*;
//import java.util.ArrayList;
import java.lang.reflect.Method;

//import org.objectweb.asm.*;
import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.serialize.ScriptableOutputStream;
import org.objectweb.asm.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static Gumtree.main.GraphBasedMain.run_gumtree_ana;

public class runExp extends ClassLoader {
    static Set<String> expDataSet = new HashSet<>();

    static void getExpSet() {
        String fileName = "/home/experiment/experiment_set.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                expDataSet.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void getFileAll(File file, ArrayList<File> fileList) { // get all class files in project
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    getFileAll(files[i], fileList);
                } else {
                    if (files[i].getName().contains(".class"))
                        fileList.add(files[i]);
                }
            }
        }
        return;
    }

    static List<String[]> searchDatabase(String snyk_id, String commitid, String filename) throws SQLException {
        String sql = "select distinct method_longname,group_id,artifact_id,cve_no,methodname from " + DataBaseconfig.Table_vulapis + " where snyk_id = ? and commitid = ? and filename = ?";
        try {
            PreparedStatement p = DataBaseconfig.conn.prepareStatement(sql);
            p.setString(1, snyk_id);
            p.setString(2, commitid);
            p.setString(3, filename);
//            p.setString(4,methodname);
            ResultSet rs = p.executeQuery();
            List<String[]> resl = new ArrayList<>();
            while (rs.next()) {
                String[] strs = new String[5];
                strs[0] = rs.getString("method_longname");
                strs[1] = rs.getString("group_id");
                strs[2] = rs.getString("artifact_id");
                strs[3] = rs.getString("cve_no");
                strs[4] = rs.getString("methodname");
                resl.add(strs);
            }
            if (resl.size() == 0) return null;
            else return resl;
        } catch (Exception e) {
            return null;
        }
    }

    static void runMatch(String path, String filename, String commitid, String method_longname, String group_id, String artifact_id, String cve_no) throws IOException { // Match Phase
//        new File(RuntimeConfig.resDir).delete(); // remove prior res.json
        List<String> methodList = new ArrayList<>();
        methodList.add(method_longname.split("\\.")[method_longname.split("\\.").length - 1]);
        removeMethod.doDelete(RuntimeConfig.runtimeTfile, methodList);
        if (!getMethod.hasMethod(RuntimeConfig.runtimeTfile)) {
            return;
        }

        try {
            Map<String, String> resMap;
            long start = System.currentTimeMillis();
            // read vast and past from db,and its edit-tree
            // and cp pfile and vfile from db to runtime folder
            DataRecover astInfo = new DataRecover(filename, commitid, method_longname);
            long end = System.currentTimeMillis();
            String resstep1 = newDecCheck.DecCheck(RuntimeConfig.runtimeVfile, RuntimeConfig.runtimePfile, RuntimeConfig.runtimeTfile);  //Step 1


            if (!resstep1.equals("Unknown")) {
                resMap = new HashMap<>();
                resMap.put("Path", path);
                resMap.put("Analysis Res", resstep1);
                // db IO 时间
                resMap.put("IOCost", String.valueOf(end - start));
                resMap.put("Step", "1");
                resMap.put("group_id", group_id);
                resMap.put("artifact_id", artifact_id);
                long endTotal = System.currentTimeMillis();
                resMap.put("method_longname", method_longname);
                resMap.put("cve_no", cve_no);
                resMap.put("TotalCost", String.valueOf(endTotal - start));
                JsonRW.write(resMap, RuntimeConfig.resDir);
                delete_tempFile();
                return;
            }
            // gumtree analysis and put analysis result in resMap
            resMap = run_gumtree_ana(RuntimeConfig.runtimeTfile, astInfo); //args[4] is temp arg

            long endTotal = System.currentTimeMillis();
            resMap.put("Path", path);
            resMap.put("cve_no", cve_no);
            resMap.put("method_longname", method_longname);
            resMap.put("IOCost", String.valueOf(end - start));
            resMap.put("TotalCost", String.valueOf(endTotal - start));
            resMap.put("group_id", group_id);
            resMap.put("artifact_id", artifact_id);
            JsonRW.write(resMap, RuntimeConfig.resDir);
            delete_tempFile();
        } catch (Exception e) {
            Map<String, String> resMap = new HashMap<>();
            resMap.put("Path", path);
            resMap.put("tfile", filename);
            resMap.put("Analysis Res", "Error");
            resMap.put("group_id", group_id);
            resMap.put("artifact_id", artifact_id);
            JsonRW.write(resMap, RuntimeConfig.resDir);
            delete_tempFile();
        }
    }

    public static void main(String[] args) throws Exception { // input: path
        // main entry
        if (args.length != 0) {
            String tpath = args[0];
            String[] vul_informs = tpath.split("/");
            String snyk_id = vul_informs[4];
            String commitid = vul_informs[6];
            String filename = vul_informs[7];
            RuntimeConfig.setRuntimeDir(args[1]);
            String prefix_path = "/home/experiment/experiment_dataset/";
            List<String[]> itemlist = searchDatabase(snyk_id, commitid, filename); //  search suspect iterms which might match
            if (itemlist != null) {
                getExpSet();
                String targetDir = tpath.split(prefix_path)[1].split("tfile.java")[0];
                for (String[] strs : itemlist) {  // for each api, run Match Phase
                    String targetApi = targetDir + strs[4];
                    if (!expDataSet.contains(targetApi)) {
                        continue;
                    }
                    FileUtils.copyFile(new File(tpath), new File(RuntimeConfig.runtimeTfile));
                    runMatch(tpath, filename, commitid, strs[0], strs[1], strs[2], strs[3]);
                }
            }
        }
    }

    private static void delete_tempFile() {
        new File(RuntimeConfig.runtimeVfile).delete();
        new File(RuntimeConfig.runtimePfile).delete();
        new File(RuntimeConfig.runtimeTfile).delete();
    }

    private static void delete_zip_project(String path) {
        File file = new File(path);
        if (file == null || !file.exists()) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    delete_zip_project(f.getAbsolutePath());
                } else {
                    f.delete();
                }
            }
        }
        file.delete();
    }
}
