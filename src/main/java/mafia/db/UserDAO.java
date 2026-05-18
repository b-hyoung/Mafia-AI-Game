package mafia.db;

import com.sun.jdi.request.DuplicateRequestException;
import mafia.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String sql;

    private UserDAO() {}

    UserDAO instance = new UserDAO();

    public UserDAO getInstance() {
        return instance;
    }

    // ① 닉네임으로 사용자(또는 hash) 조회
    public String findPasswordHash(String nickname) throws SQLException {
        try (Connection conn = DBConnect.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM users WHERE nickname = ?")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;          // 닉네임 없음
                return rs.getString("password_hash");
            }
        }
    }

    public boolean RegisterUser(String userid,String password) throws SQLException {
        if (findPasswordHash(userid) != null) {
            return false;   // 이미 존재
        }

        String sql = "insert into users(nickname,password_hash) values(?,?)";
        String hashPassword = BCrypt.hashpw(password,BCrypt.gensalt());

        try{
            conn = DBConnect.connect();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userid);
            pstmt.setString(2, hashPassword);
            pstmt.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }finally {
            DBConnect.close();
            pstmt.close();
        }
        return true;
    }

    public boolean LoginUser(String nickname,String password) throws SQLException {
        String storedPassword = findPasswordHash(nickname);
        if(storedPassword == null){
            return false;
        }
        return BCrypt.checkpw(password,storedPassword);
    }
}
