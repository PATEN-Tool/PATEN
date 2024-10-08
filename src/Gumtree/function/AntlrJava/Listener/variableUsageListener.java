package Gumtree.function.AntlrJava.Listener;


import Gumtree.function.AntlrJava.JavaLexer;
import Gumtree.function.AntlrJava.JavaParser;
import Gumtree.function.AntlrJava.JavaParserBaseListener;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import java.util.*;

public class variableUsageListener extends JavaParserBaseListener {
    boolean inMethod = false; //true when get into method
    List<Integer> Lines;
    Iterator<Integer> ite;
    Integer crt;
    boolean done = false;
    public List<String> tokens = new ArrayList<>();
    public List<Integer> start = new ArrayList<>();
    public List<Integer> ending = new ArrayList<>();
    public List<String> methods = new ArrayList<>();
    public Map<String, Integer> tokenPosition = new HashMap<>();

    String tmpmethod = null;


    public variableUsageListener(List<Integer> Lines) {
        this.Lines = Lines;
        ite = Lines.iterator();
        crt = ite.next();
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        inMethod = true;
        for (ParseTree p : ctx.children) {
            if (p instanceof TerminalNodeImpl && ((TerminalNodeImpl) p).getSymbol().getType() == JavaLexer.IDENTIFIER) {
                tmpmethod = p.getText();
            }
        }
    }

    @Override
    public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        inMethod = false;
    }


    @Override
    public void enterPrimary(JavaParser.PrimaryContext ctx) {
        if (done)
            return;
        while (ctx.start.getLine() > crt)
            if (ite.hasNext())
                crt = ite.next();
            else {
                done = true;
                return;
            }
        if (inMethod && ctx.start.getLine() == crt) {
            if (!tokens.contains(ctx.getText())) {
                tokenPosition.put(ctx.getText(), ctx.start.getLine());
                tokens.add(ctx.getText());
                methods.add(tmpmethod);
                start.add(ctx.start.getLine());
                ending.add(ctx.start.getLine());
            } else {
                ending.set(tokens.indexOf(ctx.getText()), ctx.start.getLine());
            }
        }
    }

    @Override
    public void enterVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
        if (done)
            return;
        while (ctx.start.getLine() > crt)
            if (ite.hasNext())
                crt = ite.next();
            else {
                done = true;
                return;
            }
        if (inMethod && ctx.start.getLine() == crt) {
            if (!tokens.contains(ctx.getText())) {
                tokenPosition.put(ctx.getText(), ctx.start.getLine());
                tokens.add(ctx.getText());
                methods.add(tmpmethod);
                start.add(ctx.start.getLine());
                ending.add(ctx.start.getLine());
            } else {
                ending.set(tokens.indexOf(ctx.getText()), ctx.start.getLine());
            }
            if (ite.hasNext()) {
                crt = ite.next();
            }
        }
    }

}
