package com.tcpchat.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    private static final int THREAD_POOL_SIZE = 50;

    private final int port;
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private volatile boolean running = true;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat Server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void broadcast(String message, String sender) {
        clients.forEach((name, handler) -> {
            if (!name.equals(sender)) {
                handler.sendMessage(message);
            }
        });
    }

    public void register(String name, ClientHandler handler) {
        clients.put(name, handler);
        broadcast(name + " joined the chat.", "SYSTEM");
    }

    public void unregister(String name) {
        clients.remove(name);
        broadcast(name + " left the chat.", "SYSTEM");
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new ChatServer(port).start();
    }
}
