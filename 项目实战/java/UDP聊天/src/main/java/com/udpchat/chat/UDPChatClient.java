package com.udpchat.chat;

import java.io.*;
import java.net.*;

public class UDPChatClient {
    private static final int DEFAULT_PORT = 9999;
    private final int port;
    private final DatagramSocket socket;
    private volatile boolean running = true;

    public UDPChatClient(int port) throws SocketException {
        this.port = port;
        this.socket = new DatagramSocket(port);
    }

    public void start() {
        // Receiver thread
        Thread receiver = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    System.out.println("[From " + packet.getAddress().getHostAddress() + "] " + message);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Receive error: " + e.getMessage());
                    }
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();

        // Sender loop
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("UDP Chat started on port " + port);
            System.out.println("Enter target host (or 'broadcast' for 255.255.255.255): ");
            String targetHost = consoleReader.readLine();
            System.out.println("Enter target port: ");
            int targetPort = Integer.parseInt(consoleReader.readLine());

            InetAddress targetAddress;
            if ("broadcast".equalsIgnoreCase(targetHost)) {
                targetAddress = InetAddress.getByName("255.255.255.255");
                socket.setBroadcast(true);
            } else {
                targetAddress = InetAddress.getByName(targetHost);
            }

            System.out.println("Start chatting! Type /quit to exit.");
            String message;
            while ((message = consoleReader.readLine()) != null) {
                if ("/quit".equalsIgnoreCase(message)) {
                    break;
                }
                byte[] data = message.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            running = false;
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            new UDPChatClient(port).start();
        } catch (SocketException e) {
            System.err.println("Failed to start UDP chat: " + e.getMessage());
        }
    }
}
