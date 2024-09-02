package Preprocess;

import java.io.*;
import java.sql.*;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.*;


public class remove_method {
    Map<String, ArrayList<String>> map;
    ArrayList<int[]> position;//记录不匹配方法的起始结束
    ArrayList<int[]> position2;//记录匹配方法的起始结束


    void getResult(File file) throws IOException, ClassNotFoundException, SQLException {
        if (file.exists()) {
            if (file.isFile()) {
                String snyk_id = file.getAbsolutePath().split("\\\\")[6];
                String file_name = file.getAbsolutePath().split("\\\\")[9];
//                  System.out.println(snyk_id + " " + file_name);
                getResult(file.getAbsolutePath(), snyk_id, file_name);
            } else {
                File[] list = file.listFiles();
                if (list.length == 0) {
                    System.out.println(file.getAbsolutePath() + " is null");
                } else {
                    for (int i = 0; i < list.length; i++) {
                        getResult(list[i]);//递归调用
                    }
                }
            }
        } else {
            System.out.println("文件不存在！");
        }
    }

    void getResult(String filePath, String id, String fname) throws IOException, ClassNotFoundException, SQLException {


        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        String DB_URL = "jdbc:mysql://localhost:3306/vulnerable_apis?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        Class.forName(JDBC_DRIVER);
        Connection con = DriverManager.getConnection(DB_URL, "root", "thghxf123");
        Statement stmt = con.createStatement();
        String sql = "select DISTINCT snyk_id,filename,methodname,params from vul_apis where snyk_id = '" + id + "' and filename = '" + fname + "';";
        ResultSet rs = stmt.executeQuery(sql);

        String snyk_id, file_name, method_name = "", param, path;
        ArrayList<String> params = new ArrayList<>();
        String method_name1 = "";
        map = new HashMap<>();
        while (rs.next()) {
            method_name = rs.getString("methodname");
            param = rs.getString("params");
            if (method_name.equals(method_name1)) {
                params.add(param);
            } else if (method_name1.equals("")) {
                method_name1 = method_name;
                params.add(param);
            } else {
                map.put(method_name1, params);
                params = new ArrayList<>();
                params.add(param);
                method_name1 = method_name;
            }
//            System.out.println(method_name +"  " + param);
        }
        con.close();
        map.put(method_name, params);
        if (get_method_position(filePath) == true) {
            updatePosition();
            updatePosition2();
            delete_method(filePath);
        } else {
            FileWriter fw = null;
            try {
                File f = new File("D:\\error_log");
                fw = new FileWriter(f, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            PrintWriter pw = new PrintWriter(fw);
            pw.println(filePath);
            pw.flush();
            try {
                fw.flush();
                pw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean get_method_position(String path) throws IOException {
        position = new ArrayList<>();
        position2 = new ArrayList<>();
        ASTParser astParser = ASTParser.newParser(AST.JLS14);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setSource(readFileToString(path).toCharArray());
        CompilationUnit unit = (CompilationUnit) astParser.createAST(null);
        if (unit.types().size() == 0) {
            return false;
        }
        List<Object> class_names = unit.types();
        TypeDeclaration type = (TypeDeclaration) class_names.get(0);

        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                SimpleName name = node.getName();
                int start = unit.getLineNumber(node.getStartPosition());
                int end = unit.getLineNumber(node.getStartPosition() + node.getLength());
                int a[] = new int[2];

                if (!method_is_equal(name.toString(), node.parameters())) {
                    a[0] = start;
                    a[1] = end;
                    position.add(a);
                } else {
                    a[0] = start;
                    a[1] = end;
                    position2.add(a);
                }
                return true;
            }
        });
        return true;
    }

    public static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }


    boolean comParameters(List parameters, String param) {
        if (param.equals("")) {
            if (parameters.size() == 0) {
                return true;
            }
            return false;
        }
        String params[] = param.split(",");
        if (parameters.size() != params.length) {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++) {
            String p1[] = parameters.get(i).toString().split("\\s+");
            String p1_type = p1[p1.length - 2];
            String p2[] = params[i].split("\\s+");
            String p2_type = p2[p2.length - 2];
            if (!p1_type.equals(p2_type)) {
                return false;
            }
        }
        return true;
    }

    boolean method_is_equal(String method_name, List parameters) {
        String methods[] = map.keySet().toArray(new String[0]);
        if (map.containsKey(method_name)) {
            return true;

        }
        return false;
    }

    void updatePosition() {
        int start1, start2;
        int end1, end2;
        for (int i = 0; i < position2.size(); i++) {
            start2 = position2.get(i)[0];
            end2 = position2.get(i)[1];
            for (int j = 0; j < position.size(); j++) {
                start1 = position.get(j)[0];
                end1 = position.get(j)[1];
                if (start1 >= start2 && end1 <= end2) {
                    position.remove(j);
                    j--;
                }
                if (start1 <= start2 && end1 >= end2) {
                    position.remove(j);
                    j--;
                }
            }
        }
    }

    void updatePosition2() {
        for (int i = 0; i < position.size() - 1; i++) {
            int start1 = position.get(i)[0];
            int end1 = position.get(i)[1];
            int start2 = position.get(i + 1)[0];
            int end2 = position.get(i + 1)[1];
            if (start2 >= start1 && end2 <= end1) {
                position.remove(i + 1);
                i--;
            }
        }
    }

    void delete_method(String path) {
        File file = new File(path);
        File temp = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        int line_number = 0;
        int count_position = 0;
        if (position.size() == 0) {
            return;
        }
        int start = position.get(count_position)[0];
        int end = position.get(count_position)[1];
        try {
            temp = File.createTempFile("temp", "temp");
            pw = new PrintWriter(temp);
            br = new BufferedReader(new FileReader(file));
            while (br.ready()) {
                String line = br.readLine();
                line_number++;
                if (line_number == 2200) {
                    int a = 1;
                }
                if (line_number >= start && line_number < end) {
                    continue;
                } else if (line_number == end && count_position < position.size() - 1) {
                    count_position++;
                    start = position.get(count_position)[0];
                    end = position.get(count_position)[1];
                    continue;
                } else if (line_number == end) {
                    continue;
                }
                pw.println(line);
            }
            pw.flush();
            br.close();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            file.delete();
            temp.renameTo(file);
        }
    }
}
