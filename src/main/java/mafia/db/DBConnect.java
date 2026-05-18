package mafia.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL 커넥션 팩토리.
 * 호출자가 try-with-resources로 닫는다 (정적 conn 캐싱 X).
 */
public class DBConnect {

    public static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String URL =
        "jdbc:mysql://localhost:3306/mafia_game?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul";
    public static final String DB_ID = "root";
    public static final String DB_PWD = "1234"; // TODO: 본인 환경에 맞게 변경 (또는 properties/env로 분리)

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found: " + DB_DRIVER, e);
        }
    }

    private DBConnect() {
        // utility class — no instances
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, DB_ID, DB_PWD);
    }
}
