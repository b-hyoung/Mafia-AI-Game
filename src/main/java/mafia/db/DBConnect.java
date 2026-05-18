package mafia.db;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.sql.Connection;
import java.sql.Driver;
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
    public static Connection conn=null;

    public static Connection connect() {
        try {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(URL, DB_ID, DB_PWD);

            if (conn != null) {
                System.out.println("DB 연결 성공");
            } else {
                System.out.println("DB 연결 실패");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found: " + DB_DRIVER, e);
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("DB 오류");
            alert.setHeaderText("DB 연결 실패");
            alert.setContentText("데이터베이스에 연결할 수 없습니다.\n" + e.getMessage());
            alert.showAndWait();
        }
        return conn;
    }
    public static void close() throws Exception{
        if(conn!=null) {
            conn.close();
            System.out.println("DB 연결 해제");
        }else {
            System.out.println("이미 해제 상태");
        }
    }
}
