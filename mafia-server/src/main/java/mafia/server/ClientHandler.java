package mafia.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 한 클라이언트 연결을 담당하는 Runnable. readLine → echo 반복.
 * 클라이언트가 끊으면 readLine이 null 반환 → loop 종료 → 자원 정리.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String addr = socket.getRemoteSocketAddress().toString();
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Received from " + addr + "]: " + line);
                out.println(line);  // echo
            }
        } catch (IOException e) {
            System.err.println("[Error on " + addr + "]: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) { }
            System.out.println("[Disconnected: " + addr + "]");
        }
    }
}
