package com.tcpchat.server;

import java.io.*;
import java.net.Socket;

/**
 * 客户端处理器 — 每个连接的客户端一个实例，运行在独立线程中
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))
        ) {
            writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

            // 第一步：读取用户名
            username = reader.readLine();
            if (username == null || username.isBlank()) {
                return;
            }
            username = username.trim();

            System.out.println(username + " 加入了聊天室 [" + socket.getInetAddress() + "]");
            sendMessage("系统: 欢迎 " + username + " 加入聊天室！");
            ChatServer.broadcast("系统: " + username + " 加入了聊天室", this);
            sendUserList();

            // 持续读取消息
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                if ("/users".equals(line)) {
                    sendUserList();
                } else if ("/quit".equals(line)) {
                    break;
                } else {
                    String msg = username + ": " + line;
                    System.out.println(msg);
                    ChatServer.broadcast(msg, this);
                }
            }
        } catch (IOException e) {
            System.err.println(username + " 连接异常: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /** 发送消息给当前客户端 */
    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    /** 发送在线用户列表 */
    private void sendUserList() {
        StringBuilder sb = new StringBuilder("系统: 在线用户 (");
        int count = ChatServer.getClients().size();
        sb.append(count).append("人): ");
        for (ClientHandler c : ChatServer.getClients()) {
            sb.append(c.getUsername()).append(" ");
        }
        sendMessage(sb.toString());
    }

    public String getUsername() {
        return username;
    }

    private void disconnect() {
        ChatServer.removeClient(this);
        if (username != null) {
            System.out.println(username + " 离开了聊天室");
            ChatServer.broadcast("系统: " + username + " 离开了聊天室", this);
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
