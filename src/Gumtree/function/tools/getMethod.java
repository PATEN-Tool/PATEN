package Gumtree.function.tools;

import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class getMethod {
    ArrayList<String> methods;


    public static boolean hasMethod(String tfile) throws IOException {
        getMethod a = new getMethod(tfile);
        ArrayList<String> methods = a.get_methods();
        if (methods.size() == 0)
            return false;
        else
            return true;
    }

    private static void travelDir(String path) throws IOException {
        File f = new File(path);
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isFile() && file.getName().contains("tfile.java")) {

                getMethod g = new getMethod(file.getAbsolutePath());
                if (g.methods.size() == 0) {
                    File res = new File("./res");
                    BufferedWriter bf = new BufferedWriter(new FileWriter(res, true));
                    System.out.println(file.getAbsolutePath());
                    bf.write(file.getAbsolutePath() + "\n");
                    bf.close();
                }
            }
            if (file.isDirectory()) {
                travelDir(file.getAbsolutePath());
            }
        }
    }


    public getMethod(String path) throws IOException {
        methods = new ArrayList<>();
        ASTParser astParser = ASTParser.newParser(AST.JLS14);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setSource(FileReaders.readFileToString(path).toCharArray());
        CompilationUnit unit = (CompilationUnit) astParser.createAST(null);
        if (unit.types().size() == 0) {
            return;
        }
        List<Object> class_names = unit.types();
        TypeDeclaration type = (TypeDeclaration) class_names.get(0);

        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                node.getName();
                if (node.getBody() == null)
                    return true;
                methods.add(node.getBody().toString());
                return true;
            }
        });
    }


    public ArrayList<String> get_methods() {
        return methods;
    }

}