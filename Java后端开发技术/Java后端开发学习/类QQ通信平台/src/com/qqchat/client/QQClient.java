package com.qqchat.client;

import com.qqchat.protocol.*;

import java.io.*;
import java.net.Socket;

public class QQClient {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ServerListener listener;
    private ChatSession session;
    private volatile boolean running = true;

    public QQClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.session = new ChatSession();
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

            System.out.println("============================================");
            System.out.println("  QQ Chat Client");
            System.out.println("  Connected to " + host + ":" + port);
            System.out.println("  Type /help for available commands");
            System.out.println("============================================");

            // Login/Register loop
            if (!authenticate()) {
                System.out.println("Goodbye!");
                return;
            }

            // Start server message listener
            listener = new ServerListener(socket.getInputStream());
            listener.start();

            // Start ping thread
            startPinger();

            // Enter command loop
            commandLoop();

        } catch (IOException e) {
            System.err.println("Failed to connect to " + host + ":" + port + " - " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private boolean authenticate() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.println();
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            String choice = console.readLine();

            if ("3".equals(choice) || "/quit".equalsIgnoreCase(choice)) {
                return false;
            }

            System.out.print("Username: ");
            String username = console.readLine();
            System.out.print("Password: ");
            String password = console.readLine();

            if ("2".equals(choice)) {
                System.out.print("Nickname: ");
                String nickname = console.readLine();

                ProtocolFrame frame = new ProtocolFrame(Command.REGISTER, username, "", username + "|" + password + "|" + nickname);
                out.println(frame.serialize());
            } else {
                ProtocolFrame frame = new ProtocolFrame(Command.LOGIN, username, "", username + "|" + password);
                out.println(frame.serialize());
            }

            // Read response
            String response = in.readLine();
            if (response == null) {
                System.out.println("Server closed connection.");
                return false;
            }

            ProtocolFrame resp = ProtocolParser.parse(response);
            if (resp != null) {
                System.out.println(resp.getPayload());
                if (resp.getCommand() == Command.AUTH_OK) {
                    session.setUserId(resp.getSenderId().equals("SERVER") ? resp.getTargetId() : resp.getSenderId());
                    session.setNickname(username);
                    return true;
                }
            }
        }
    }

    private void commandLoop() throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (running) {
            if (session.isInChatMode()) {
                System.out.print("[" + session.getCurrentChatTarget() + "] ");
            } else {
                System.out.print("> ");
            }

            String line = console.readLine();
            if (line == null) break;

            String result = processCommand(line.strip());
            if (result != null) {
                if (!result.isEmpty()) {
                    System.out.print(result);
                }

                // For synchronous responses, wait and read
                if (expectsResponse(line)) {
                    String response = in.readLine();
                    if (response != null) {
                        ProtocolFrame frame = ProtocolParser.parse(response);
                        if (frame != null) {
                            System.out.println(frame.getPayload());
                        }
                    }
                }
            }
        }
    }

    private String processCommand(String line) {
        if (line.isEmpty()) return "";

        // In chat mode, treat non-commands as messages
        if (session.isInChatMode() && !line.startsWith("/")) {
            if (session.isInGroupChat()) {
                sendFrame(Command.SEND_GROUP_MSG, session.getCurrentChatTarget(), line);
            } else {
                sendFrame(Command.SEND_MSG, session.getCurrentChatTarget(), line);
            }
            return "";
        }

        // Parse command
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help":
                return showHelp();
            case "/quit":
                running = false;
                sendFrame(Command.LOGOUT, "", "");
                return "Goodbye!\n";
            case "/msg":
                if (parts.length < 3) return "Usage: /msg <username> <message>\n";
                sendFrame(Command.SEND_MSG, parts[1], parts[2]);
                return "";
            case "/chat":
                if (parts.length < 2) return "Usage: /chat <username>\n";
                session.enterChatMode(parts[1], false);
                return "Entering chat with " + parts[1] + ". Type /exit to leave.\n";
            case "/gchat":
                if (parts.length < 2) return "Usage: /gchat <groupId>\n";
                session.enterChatMode(parts[1], true);
                return "Entering group chat with " + parts[1] + ". Type /exit to leave.\n";
            case "/exit":
                if (session.isInChatMode()) {
                    session.exitChatMode();
                    return "Exited chat mode.\n";
                }
                return "Not in chat mode.\n";
            case "/gmsg":
                if (parts.length < 3) return "Usage: /gmsg <groupId> <message>\n";
                sendFrame(Command.SEND_GROUP_MSG, parts[1], parts[2]);
                return "";
            case "/addfriend":
                if (parts.length < 2) return "Usage: /addfriend <username>\n";
                sendFrame(Command.ADD_FRIEND, "", parts[1]);
                return null;
            case "/delfriend":
                if (parts.length < 2) return "Usage: /delfriend <username>\n";
                sendFrame(Command.DEL_FRIEND, "", parts[1]);
                return null;
            case "/friends":
                sendFrame(Command.FRIEND_LIST, "", "");
                return null;
            case "/creategroup":
                if (parts.length < 2) return "Usage: /creategroup <groupName>\n";
                sendFrame(Command.CREATE_GROUP, "", parts[1]);
                return null;
            case "/joingroup":
                if (parts.length < 2) return "Usage: /joingroup <groupId>\n";
                sendFrame(Command.JOIN_GROUP, "", parts[1]);
                return null;
            case "/leavegroup":
                if (parts.length < 2) return "Usage: /leavegroup <groupId>\n";
                sendFrame(Command.LEAVE_GROUP, "", parts[1]);
                return null;
            case "/groups":
                sendFrame(Command.GROUP_LIST, "", "");
                return null;
            case "/groupinfo":
                if (parts.length < 2) return "Usage: /groupinfo <groupId>\n";
                sendFrame(Command.GROUP_INFO, "", parts[1]);
                return null;
            case "/profile":
                sendFrame(Command.GET_PROFILE, "", "");
                return null;
            case "/setnickname":
                if (parts.length < 2) return "Usage: /setnickname <newNickname>\n";
                sendFrame(Command.UPDATE_PROFILE, "", "nickname|" + parts[1]);
                return null;
            case "/search":
                if (parts.length < 2) return "Usage: /search <query>\n";
                sendFrame(Command.SEARCH_USER, "", parts[1]);
                return null;
            case "/online":
                sendFrame(Command.FRIEND_LIST, "", "");
                return null;
            case "/history":
                if (parts.length < 2) return "Usage: /history <username> [count]\n";
                System.out.println("History feature: use /chat <user> and view messages.");
                return "";
            default:
                return "Unknown command: " + cmd + ". Type /help for available commands.\n";
        }
    }

    private boolean expectsResponse(String line) {
        if (line == null) return false;
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        return switch (cmd) {
            case "/addfriend", "/delfriend", "/friends", "/creategroup", "/joingroup",
                 "/leavegroup", "/groups", "/groupinfo", "/profile", "/setnickname",
                 "/search", "/online" -> true;
            default -> false;
        };
    }

    private void sendFrame(Command command, String target, String payload) {
        if (out != null) {
            ProtocolFrame frame = new ProtocolFrame(command, session.getUserId(), target, payload);
            out.println(frame.serialize());
        }
    }

    private void startPinger() {
        Thread pinger = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(20_000);
                    sendFrame(Command.PING, "", "ping");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        pinger.setDaemon(true);
        pinger.setName("Pinger");
        pinger.start();
    }

    private String showHelp() {
        return """
                === QQ Chat Client Commands ===

                --- Authentication ---
                /login <username> <password>     Login to server
                /register <username> <pwd> <nick> Register new account
                /quit                             Exit the client

                --- Friends ---
                /addfriend <username>             Add a friend
                /delfriend <username>             Remove a friend
                /friends                          List all friends

                --- Private Chat ---
                /msg <username> <message>         Send a single message
                /chat <username>                  Enter continuous chat mode

                --- Group Chat ---
                /creategroup <groupName>          Create a new group
                /joingroup <groupId>              Join an existing group
                /leavegroup <groupId>             Leave a group
                /groups                           List my groups
                /groupinfo <groupId>              Show group details
                /gmsg <groupId> <message>         Send group message
                /gchat <groupId>                  Enter group chat mode

                --- Profile ---
                /profile                          View my profile
                /setnickname <newNickname>        Change nickname
                /search <query>                   Search users by name

                --- Chat Mode ---
                /exit                             Exit chat mode
                (type any text to send message directly)

                --- Other ---
                /help                             Show this help
                """;
    }

    private void shutdown() {
        running = false;
        if (listener != null) listener.shutdown();
        try { socket.close(); } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 9999;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }

        new QQClient(host, port).start();
    }
}
