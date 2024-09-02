// Token change analysis using vfile.java, pfile and corresponding add/delete line number
package TokenAnalysis.TokenAna;

import TokenAnalysis.AntlrJava.JavaLexer;
import TokenAnalysis.TokenAna.Tools.otherTools;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class TokenRev {
    public Map<String, Integer> TokenTotalRev;
    public Map<String, Integer> TokenRevAdd_SingleMethod;
    public Map<String, Integer> TokenRevDel_SingleMethod;
    public ArrayList<Map<String, Integer>> TokenRevAddList;
    public ArrayList<Map<String, Integer>> TokenRevDelList;

    public static List<String> specialTokenList = new ArrayList<>() {
        {
            this.add("String");
        }
    };

    public TokenRev(String vul, String pat, ArrayList<String> addLines, ArrayList<String> delLines) throws IOException {
        this.TokenTotalRev = calcTokenTotalRev(vul, pat);
        this.TokenRevAddList = calcAddList(pat, addLines);
        this.TokenRevDelList = calcDelList(vul, delLines);
    }

    private ArrayList<Map<String, Integer>> calcAddList(String pat, ArrayList<String> addLines) throws IOException {
        if (addLines == null)
            return null;
        ArrayList<Map<String, Integer>> addlist = new ArrayList<>();
        for (String add : addLines) {
            String content = getContent_withline(pat, add);
            addlist.add(getTokenNm(content, "P"));
        }
        return addlist;
    }

    private ArrayList<Map<String, Integer>> calcDelList(String vul, ArrayList<String> delLines) throws IOException {
        if (delLines == null)
            return null;
        ArrayList<Map<String, Integer>> dellist = new ArrayList<>();
        for (String del : delLines) {
            String content = getContent_withline(vul, del);
            dellist.add(getTokenNm(content, "P"));
        }
        return dellist;
    }

    private Map<String, Integer> calcTokenTotalRev(String vul, String pat) {
        String contentDel = otherTools.readFile(vul);
        String contentAdd = otherTools.readFile(pat);
        return tokenAna(contentDel, contentAdd);
    }

    private String getContent_withline(String pat, String addlines) throws IOException { //Input: revision lines Out: Content of lines
        String content = "";
        List<Integer> lines = new ArrayList<>();
        for (String line : addlines.split(","))
            lines.add(Integer.valueOf(line));
        Iterator<Integer> lines_ite = lines.iterator();
        BufferedReader bf = new BufferedReader(new StringReader(pat));
        String s;
        int lineNO = 0;
        int targetline = lines_ite.next();
        while ((s = bf.readLine()) != null) {
            lineNO++;
            if (lineNO == targetline) {
                content += s;
                if (!lines_ite.hasNext())
                    break;
                targetline = lines_ite.next();
            }
        }
        return content;
    }


    public static Map<String, Integer> tokenChange(Map<String, Integer> del, Map<String, Integer> add) {
        Map<String, Integer> MapSum = new HashMap<>();
        for (String key : del.keySet()) {
            if (add.containsKey(key)) {
                if (del.get(key) + add.get(key) == 0) {
                    add.remove(key);
                    continue;
                }
                MapSum.put(key, del.get(key) + add.get(key));
                add.remove(key);
            } else
                MapSum.put(key, del.get(key));
        }
        MapSum.putAll(add);
        return MapSum;
    }

    public static Map<String, Integer> getTokenNm(String content, String mode) {
        int increase;
        if (mode.equals("P"))
            increase = 1;
        else
            increase = -1;
        CharStream inputs = CharStreams.fromString(content);
        JavaLexer lexer = new JavaLexer(inputs);
        Map<String, Integer> tokens = new HashMap<>();
        Iterator ite = lexer.getAllTokens().iterator();
        while (ite.hasNext()) {
            Token s = (Token) ite.next();
            if (s.getType() == JavaLexer.IDENTIFIER && !specialTokenList.contains(s.getText())) {
                if (tokens.containsKey(s.getText()))
                    tokens.put(s.getText(), tokens.get(s.getText()) + increase);
                else
                    tokens.put(s.getText(), increase);
            }
        }
        return tokens;
    }

    public static List<String> getTokenListTotal(String content) {
        List<String> tokens = new ArrayList<>();
        CharStream inputs = CharStreams.fromString(content);
        JavaLexer lexer = new JavaLexer(inputs);
        Iterator<? extends Token> ite = lexer.getAllTokens().iterator();
        while (ite.hasNext()) {
            Token tmp = ite.next();
            tokens.add(tmp.getText());
        }
        return tokens;
    }

    public static Map<String, Integer> tokenAna(String del, String add) {
        Map<String, Integer> del_tokens = getTokenNm(del, "V");
        Map<String, Integer> add_tokens = getTokenNm(add, "P");
        Map sum = tokenChange(del_tokens, add_tokens);
        return sum;
    }

    public static Map<String, Integer>[] getMaxToken(Map<String, Integer> tokenlist) {
        Map maxmap[] = new Map[2];
        Map<String, Integer> maxmapAdd = new HashMap<>();
        Map<String, Integer> maxmapDel = new HashMap<>();
        int maxadd = 0;
        int maxdel = 0;
        maxmap[0] = maxmapAdd;
        maxmap[1] = maxmapDel;
        for (String s : tokenlist.keySet()) {
            int value = tokenlist.get(s);
            if (value > 0) {
                if (value < maxadd)
                    continue;
                else if (value == maxadd)
                    maxmapAdd.put(s, value);
                else {
                    maxmapAdd.clear();
                    maxmapAdd.put(s, value);
                    maxadd = value;
                }
            } else {
                if (value > maxdel)
                    continue;
                else if (value == maxdel)
                    maxmapDel.put(s, value);
                else {
                    maxmapDel.clear();
                    maxmapDel.put(s, value);
                    maxdel = value;
                }
            }
        }
        return maxmap;
    }
}
