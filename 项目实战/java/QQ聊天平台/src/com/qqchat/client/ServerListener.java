package com.qqchat.client;

import com.qqchat.protocol.*;

import java.io.*;

public class ServerListener extends Thread {
    private final BufferedReader in;
    private volatile boolean running = true;

    public ServerListener(InputStream inputStream) {
        this.in = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
        setDaemon(true);
        setName("ServerListener");
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                ProtocolFrame frame = ProtocolParser.parse(line);
                if (frame == null) continue;
                handleMessage(frame);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("\n[!] Connection lost: " + e.getMessage());
            }
        }
    }

    private void handleMessage(ProtocolFrame frame) {
        switch (frame.getCommand()) {
            case RECV_MSG:
                System.out.println("\n[PM] " + frame.getSenderId() + ": " + frame.getPayload());
                printPrompt();
                break;
            case RECV_GROUP_MSG:
                String[] parts = frame.getPayload().split("\\|", 3);
                if (parts.length >= 3) {
                    System.out.println("\n[Group:" + parts[0] + "] " + parts[1] + ": " + parts[2]);
                } else {
                    System.out.println("\n[Group] " + frame.getSenderId() + ": " + frame.getPayload());
                }
                printPrompt();
                break;
            case FRIEND_ONLINE:
                System.out.println("\n[*] " + frame.getSenderId() + " is now online");
                printPrompt();
                break;
            case FRIEND_OFFLINE:
                System.out.println("\n[*] " + frame.getSenderId() + " went offline");
                printPrompt();
                break;
            case FRIEND_REQUEST:
                System.out.println("\n[*] " + frame.getSenderId() + " added you as friend");
                printPrompt();
                break;
            case OFFLINE_MSGS:
                System.out.println("\n[PM] " + frame.getSenderId() + ": " + frame.getPayload());
                printPrompt();
                break;
            case MSG_DELIVERED:
                // Silent confirmation
                break;
            case AUTH_FAIL:
                System.out.println("[!] " + frame.getPayload());
                break;
            case ERROR:
                System.out.println("[!] Error: " + frame.getPayload());
                break;
            case SERVER_SHUTDOWN:
                System.out.println("\n[!] Server: " + frame.getPayload());
                break;
            default:
                // Other responses handled by ConsoleReader synchronously
                break;
        }
    }

    private void printPrompt() {
        System.out.print("> ");
        System.out.flush();
    }

    public void shutdown() {
        running = false;
        try { in.close(); } catch (IOException ignored) {}
    }
}
