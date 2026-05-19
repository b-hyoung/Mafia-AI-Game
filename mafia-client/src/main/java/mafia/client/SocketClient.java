package mafia.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 콘솔 클라이언트 — Phase 1 검증용.
 * 서버에 접속해서 사용자가 콘솔에 입력한 줄을 송신하고 서버 응답을 출력한다.
 * "/quit" 입력 시 종료.
 *
 * 실행:
 *   mvn -pl mafia-client -am exec:java -Dexec.mainClass=mafia.client.SocketClient
 * (또는 IDE에서 main 메서드 Run)
 *
 * UI 클라이언트(MainGame)와는 별개. 미래에 LobbyScene 등이 같은 소켓 인프라를 쓰게 될 것.
 */
public class SocketClient {

    public static final String HOST = "localhost";
    public static final int PORT = 5500;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(HOST, PORT);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Scanner stdin = new Scanner(System.in)
        ) {
            System.out.println("서버에 연결됨 (" + HOST + ":" + PORT + ")");
            System.out.println("메시지 입력 후 Enter ('/quit'로 종료)");

            // 백그라운드 수신
            Thread receiver = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("서버 응답: " + line);
                    }
                } catch (IOException e) {
                    // 정상 종료(socket close)에서도 발생할 수 있어 조용히 무시
                }
            });
            receiver.setDaemon(true);
            receiver.start();

            // 사용자 입력 처리
            while (stdin.hasNextLine()) {
                String line = stdin.nextLine();
                if ("/quit".equals(line)) {
                    System.out.println("연결 종료");
                    break;
                }
                out.println(line);
            }
        } catch (IOException e) {
            System.err.println("클라이언트 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
