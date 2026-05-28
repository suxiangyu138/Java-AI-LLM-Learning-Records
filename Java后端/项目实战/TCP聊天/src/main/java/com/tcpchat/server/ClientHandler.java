package com.tcpchat.server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter writer;
    private String clientName;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))
        ) {
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // First message is the nickname
            clientName = reader.readLine();
            server.register(clientName, this);
            sendMessage("Welcome, " + clientName + "! Type /quit to leave.");

            String message;
            while ((message = reader.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(message)) {
                    break;
                }
                server.broadcast(clientName + ": " + message, clientName);
            }
        } catch (IOException e) {
            System.err.println("Connection error for " + clientName + ": " + e.getMessage());
        } finally {
            server.unregister(clientName);
            close();
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
