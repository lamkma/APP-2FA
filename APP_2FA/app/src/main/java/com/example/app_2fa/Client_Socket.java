package com.example.app_2fa;

import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client_Socket {
    private static final String SERVER_IP = "103.146.22.207";
    private static final int SERVER_PORT = 8888;
    private static final int SERVER_PORT1 = 5050;
    private static final int SERVER_PORT_VERIFY = 5100;
    private static final int SERVER_PORT_CLOSE2FA = 5150;
    private final ExecutorService executor;

    public Future<String> sendMessage(final String message) {
        return executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gửi thông điệp đến máy chủ
                    out.println(message);
                   // System.out.println("Check dữ liệu login "+in.readLine());
                    // Đọc và trả về phản hồi từ máy chủ
                    return in.readLine();
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }
        });
    }

    // xử lí 2FA nhưng nhiều kết nối chứ k phải 1
    public Future<String> sendMessageRequest2FA(final String message) {
        return executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT1);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gửi thông điệp đến máy chủ
                    out.println(message);

                    // Đọc và trả về phản hồi từ máy chủ
                    return in.readLine();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }
        });
    }

    public Future<String> sendMessageClose2FA(final String message) {
        return executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT_CLOSE2FA);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gửi thông điệp đến máy chủ
                    out.println(message);

                    // Đọc và trả về phản hồi từ máy chủ
                    return in.readLine();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }
        });
    }
    public Future<String> sendMessageVerify2FA(final String message) {
        return executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT_VERIFY);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gửi thông điệp đến máy chủ
                    out.println(message);

                    // Đọc và trả về phản hồi từ máy chủ
                    return in.readLine();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }
        });
    }
    public Client_Socket() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
