package com.tcpchat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP聊天 - 服务端
 * 监听端口，接受客户端连接，广播消息给所有在线用户
 */
public class ChatServer {

    private static final int PORT = 8888;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        System.out.println("=== TCP聊天服务端启动 ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务端监听端口: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("服务端异常: " + e.getMessage());
        }
    }

    /** 广播消息给所有客户端 */
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /** 移除客户端 */
    public static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    /** 获取在线用户列表 */
    public static Set<ClientHandler> getClients() {
        return clients;
    }
}
