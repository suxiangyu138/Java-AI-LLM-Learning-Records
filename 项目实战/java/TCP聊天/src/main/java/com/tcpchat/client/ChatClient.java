package com.tcpchat.client;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (
            Socket socket = new Socket(host, port);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            System.out.print("Enter your nickname: ");
            String nickname = consoleReader.readLine();
            writer.println(nickname);

            // Thread for receiving messages from server
            Thread receiver = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            receiver.setDaemon(true);
            receiver.start();

            // Main thread for sending messages
            String userInput;
            while ((userInput = consoleReader.readLine()) != null) {
                writer.println(userInput);
                if ("/quit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        new ChatClient(host, port).start();
    }
}
