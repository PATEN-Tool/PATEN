
package Gumtree.function.tools;

import Gumtree.function.AntlrJava.JavaLexer;
import Gumtree.function.AntlrJava.JavaParser;
import Gumtree.function.AntlrJava.Listener.slicingListenerDefUse;
import Gumtree.function.AntlrJava.Listener.variableUsageListener;
import com.github.difflib.algorithm.DiffException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class doSlicing {
    String vfile;
    String pfile;
    String tfile;
    public boolean addFlag = true;
    public List<Integer> slicedLine = new ArrayList();
    public Map<String, Map<Integer, List<Integer>>> def_use = new HashMap();

    public doSlicing(String vfile, String pfile, String tfile) throws IOException, DiffException {
        this.vfile = vfile;
        this.pfile = pfile;
        this.tfile = tfile;
        this.calcSim();
    }

    private void calcSim() throws IOException, DiffException {
        ParseTree treeAdd = this.initParseTrees(this.pfile);
        List<List<Integer>> res = getDiffLines.compare(this.vfile, this.pfile);
        List<Integer> addLines = (List) res.get(0);
        variableUsageListener tokenAddListener = this.getRelatedTokensSyn(this.pfile, treeAdd, addLines);
        if (tokenAddListener != null && tokenAddListener.tokens.size() != 0) {
            this.getLinesList(tokenAddListener.tokens, tokenAddListener.methods, tokenAddListener.tokenPosition, addLines, treeAdd);
        } else {
            ParseTree treeDel = this.initParseTrees(this.vfile);
            List<Integer> delLines = (List) res.get(1);
            variableUsageListener tokenDelListener = this.getRelatedTokensSyn(this.vfile, treeDel, delLines);
            if (tokenDelListener != null && tokenDelListener.tokens.size() != 0) {
                this.getLinesList(tokenDelListener.tokens, tokenDelListener.methods, tokenDelListener.tokenPosition, delLines, treeDel);
                this.addFlag = false;
            }

        }
    }

    private void getLinesList(List<String> addTokens, List<String> methodsAdd, Map<String, Integer> tokenPositionAdd, List<Integer> addLines, ParseTree treeAdd) throws IOException {
        slicingListenerDefUse s_add = new slicingListenerDefUse(addTokens, methodsAdd, tokenPositionAdd);
        ParseTreeWalker.DEFAULT.walk(s_add, treeAdd);
        Iterator var7 = s_add.slicedLineSig.keySet().iterator();

        while (var7.hasNext()) {
            String token = (String) var7.next();
            Iterator var9 = ((List) s_add.slicedLineSig.get(token)).iterator();

            while (var9.hasNext()) {
                int line = (Integer) var9.next();
                if (!this.slicedLine.contains(line)) {
                    this.slicedLine.add(line);
                }
            }
        }

        this.def_use = s_add.def_use;
    }

    private ParseTree initParseTrees(String file) {
        CharStream inputsDel = CharStreams.fromString(readFile(file));
        JavaLexer lexerDel = new JavaLexer(inputsDel);
        CommonTokenStream tokensDel = new CommonTokenStream(lexerDel);
        JavaParser parserDel = new JavaParser(tokensDel);
        ParseTree treeDel = parserDel.compilationUnit();
        return treeDel;
    }

    private variableUsageListener getRelatedTokensSyn(String file, ParseTree tree, List<Integer> Lines) throws IOException {
        if (Lines.size() == 0) {
            return null;
        } else {
            variableUsageListener listener = new variableUsageListener(Lines);
            ParseTreeWalker.DEFAULT.walk(listener, tree);
            List<String> lex = this.getRelatedTokensLex(file, Lines);

            for (int i = 0; i < listener.tokens.size(); ++i) {
                if (!lex.contains(listener.tokens.get(i))) {
                    listener.tokenPosition.remove(listener.tokens.get(i));
                    listener.tokens.remove(i);
                    listener.start.remove(i);
                    listener.ending.remove(i);
                    --i;
                }
            }

            if (listener.tokens.size() == 0) {
                return null;
            } else {
                return listener;
            }
        }
    }

    private List<String> getRelatedTokensLex(String file, List<Integer> Lines) throws IOException {
        List<String> content = getContent_withline(file, Lines);
        List<String> tokens = new ArrayList<>();
        String s = "";
        for (String t : content)
            s = s + t + "\n";
        CharStream inputs = CharStreams.fromString(s);
        JavaLexer lexer = new JavaLexer(inputs);
        for (Token t : lexer.getAllTokens()) {
            if (t.getType() == JavaLexer.IDENTIFIER) {
                tokens.add(t.getText());
            }
        }
        return tokens;
    }

    public static String readFile(String filepath) {
        String lineTxt = "";

        try {
            String encoding = "GBK";
            File file = new File(filepath);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);

                BufferedReader bufferedReader;
                String tmp;
                for (bufferedReader = new BufferedReader(read); (tmp = bufferedReader.readLine()) != null; lineTxt = lineTxt + tmp + "\n") {
                }

                bufferedReader.close();
                read.close();
            } else {
                System.out.println("cannot find target files");
            }
        } catch (Exception var7) {
            System.out.println("read error");
            var7.printStackTrace();
        }

        return lineTxt;
    }

    public static List<String> readFiletoList(String filepath) {
        List<String> lineTxt = new ArrayList();

        try {
            String encoding = "GBK";
            File file = new File(filepath);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);

                String tmp;
                while ((tmp = bufferedReader.readLine()) != null) {
                    lineTxt.add(tmp);
                }

                bufferedReader.close();
                read.close();
            } else {
                System.out.println("cannot find target files");
            }
        } catch (Exception var7) {
            System.out.println("read error");
            var7.printStackTrace();
        }

        return lineTxt;
    }

    private List<String> getContent_withline(String file, List<Integer> lines) throws IOException {
        if (lines.size() == 0) {
            return new ArrayList();
        } else {
            List<String> content = new ArrayList();
            int cnt = 0;
            BufferedReader bf = new BufferedReader(new FileReader(file));
            int lineNO = 0;
            int targetline = (Integer) lines.get(0);

            String s;
            while ((s = bf.readLine()) != null) {
                ++lineNO;
                if (lineNO == targetline) {
                    content.add(s.replace("this.", "").replace("final", ""));
                    ++cnt;
                    if (cnt == lines.size()) {
                        break;
                    }

                    targetline = (Integer) lines.get(cnt);
                }
            }

            bf.close();
            return content;
        }
    }
}
