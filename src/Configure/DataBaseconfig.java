package Configure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseconfig {
    public static String Table_fileast = "fileast";   // table of ASTs
    public static String Table_vulapis = "vul_apis2";  //table of basic vulnerability information
    public static String Table_changelist = "changelist";  //table of changed subtrees
    public static Connection conn;
    public static Statement stmt;

    static {
        final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/vul_database_exp?useSSL=false&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=UTC";
        final String USER = "root";
        final String PASS = "asdasd";

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
