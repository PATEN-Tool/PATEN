package TokenAnalysis.Test;

import TokenAnalysis.TokenAna.FileTokenAna;
import TokenAnalysis.TokenAna.TokenRev;
import TokenAnalysis.TokenAna.Tools.ItemsFromsql;
import TokenAnalysis.TokenAna.Tools.JsonRW;
import TokenAnalysis.TokenAna.Tools.getDiffLines;
import TokenAnalysis.TokenAna.Tools.otherTools;
import TokenAnalysis.TokenAna.newDeclToken;
import com.github.difflib.algorithm.DiffException;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class tokenstat {

    public static void runtest(File f, File resf, String jsonpath) throws IOException, SQLException, DiffException {
        BufferedReader bfr = new BufferedReader(new FileReader(f));
        BufferedWriter bfw = new BufferedWriter(new FileWriter(resf));
        String tmp;
        ArrayList<String> done = new ArrayList<>();
        Map<String, Integer> tokenRev;   //generally analysis
        ArrayList<Map<String, Integer>> TokenRevAddList;
        Map<String, Integer>[] max;      // tokens changed most
        String vfilecnt = "";
        String pfilecnt = "";
        int[] addLines = null;
        int[] delLines = null;
        List<String> newAdd = null;
        List<String> newDel = null;
        ArrayList<String> methodList = null;
        String vfile = null;
        String pfile = null;

        int count = 0;
        int totalcount = 0, Ttotal = 0, Ftotal = 0;
        int T = 0, F = 0;
        int Tright = 0, Fright = 0;
        boolean tag;

        while ((tmp = bfr.readLine()) != null) {
            tmp = tmp.split(":")[0];
            totalcount += 1;
            if (tmp.contains("uninfluence")) {
                Ftotal += 1;
                tag = false;
            } else {
                Ttotal += 1;
                tag = true;
            }
            String[] s = tmp.split("/");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(s[i]);
                sb.append("/");
            }
            if (!done.contains(sb.toString())) {
                done.add(sb.toString());
                vfile = sb.toString() + "vfile.java.java";
                pfile = sb.toString() + "pfile.java";
                String snykid = tmp.split("/")[6];
                String commitid = tmp.split("/")[8];
                String filename = tmp.split("/")[9];
                // get info in from mysql
                ItemsFromsql ite = new ItemsFromsql(snykid, commitid, filename);
                methodList = ite.methodList;
                ArrayList<String> addlines = ite.addlines;
                ArrayList<String> dellines = ite.dellines;
                TokenRev rev = new TokenRev(vfile, pfile, addlines, dellines);
                JsonRW.write(jsonpath, methodList);
                vfilecnt = otherTools.readFile(vfile);
                pfilecnt = otherTools.readFile(pfile);
                tokenRev = rev.TokenTotalRev;
                max = TokenRev.getMaxToken(tokenRev);
                List<List<Integer>> res = getDiffLines.compare(vfile, pfile);
                addLines = otherTools.ListtoArray(res.get(0));
                delLines = otherTools.ListtoArray(res.get(1));
                newDeclToken newDeclToken = new newDeclToken(pfilecnt, vfilecnt, addLines, delLines);
                newAdd = newDeclToken.newAddtk;
                newDel = newDeclToken.newDeltk;
            }
            String tfile = tmp;
            if (newAdd.size() != 0) {
                FileTokenAna tgtana = new FileTokenAna(tfile);
                List<String> tokenTotal = tgtana.TotalTokenList;
                FileTokenAna vfana = new FileTokenAna(vfile);
                List<String> vtokenTotal = vfana.TotalTokenList;
                for (String token : newAdd) {
                    if (tokenTotal.contains(token) && !vtokenTotal.contains(token)) {
                        F += 1;
                        if (!tag)
                            Fright += 1;
                        break;

                    }
                }
            } else if (newDel.size() != 0) {
                FileTokenAna tgtana = new FileTokenAna(tfile);
                List<String> tokenTotal = tgtana.TotalTokenList;
                FileTokenAna pfana = new FileTokenAna(pfile);
                List<String> ptokenTotal = pfana.TotalTokenList;
                for (String token : newDel) {
                    if (tokenTotal.contains(token) && !ptokenTotal.contains(token)) {
                        T += 1;
                        if (tag)
                            Tright += 1;
                        else {
                            bfw.write(tfile);
                            bfw.write("Same Token Found:" + token + "\n");
                        }
                        break;
                    }
                }
            }
            System.out.println("Done:" + tfile);
            System.out.println(totalcount + " " + Ttotal + " " + T + " " + Tright + " " + Ftotal + " " + F + " " + Fright);
        }
        System.out.println(totalcount + " " + Ttotal + " " + T + " " + Ftotal + " " + F);
    }

    public static void main(String[] args) throws IOException, SQLException, DiffException {
        // load properties
        Properties props = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream("./src/Path.properties"));
        props.load(in);
        String jsonpath = props.getProperty("jsonMethod");

        File f2 = new File("./DataandRes/resTotal");
        File resf2 = new File("./DataandRes/FP");
        runtest(f2, resf2, jsonpath);
    }


}
