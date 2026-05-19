package mafia.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Mafia 게임 TCP 서버.
 * Phase 1: echo 수준 — 받은 텍스트 라인을 그대로 돌려준다.
 *
 * 실행:
 *   mvn -pl mafia-server -am exec:java
 * (또는 IDE에서 main 메서드 Run)
 */
public class MafiaServer {

    public static final int PORT = 5500;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버 시작 (port " + PORT + ")");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[Connected: " + client.getRemoteSocketAddress() + "]");
                Thread t = new Thread(new ClientHandler(client));
                t.setDaemon(false);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
