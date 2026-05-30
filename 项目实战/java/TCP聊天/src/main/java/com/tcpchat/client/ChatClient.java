package com.tcpchat.client;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

/**
 * TCP聊天 - 客户端 (Swing GUI)
 */
public class ChatClient extends JFrame {

    private Socket socket;
    private PrintWriter writer;
    private String username;

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel statusLabel;

    public ChatClient() {
        initLogin();
    }

    /** 登录对话框 */
    private void initLogin() {
        JTextField nameField = new JTextField(15);
        JTextField hostField = new JTextField("localhost", 15);
        JTextField portField = new JTextField("8888", 6);

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("用户名:"));
        panel.add(nameField);
        panel.add(new JLabel("服务器地址:"));
        panel.add(hostField);
        panel.add(new JLabel("端口:"));
        panel.add(portField);
        panel.add(new JLabel());

        String[] options = {"连接", "取消"};
        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);

        JDialog dialog = pane.createDialog(this, "TCP聊天 - 登录");
        dialog.setVisible(true);

        if (!"连接".equals(pane.getValue())) {
            System.exit(0);
        }

        username = nameField.getText().trim();
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "端口号无效", "错误", JOptionPane.ERROR_MESSAGE);
            initLogin();
            return;
        }

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            initLogin();
            return;
        }

        connect(host, port);
    }

    /** 连接服务器 */
    private void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // 发送用户名
            writer.println(username);

            // 启动消息接收线程
            new Thread(new MessageReceiver()).start();

            // 初始化主界面
            initMainUI();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "连接服务器失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            initLogin();
        }
    }

    /** 主界面 */
    private void initMainUI() {
        setTitle("TCP聊天 - " + username);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 550);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendMessage("/quit");
                close();
            }
        });

        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("聊天消息"));

        // 在线用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        userList.setFixedCellWidth(130);

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("在线用户"));

        // 右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(150, 0));
        rightPanel.add(userScroll, BorderLayout.CENTER);

        // 中间面板
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(chatScroll, BorderLayout.CENTER);

        // 输入面板
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendChatMessage());

        sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendChatMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        // 状态栏
        statusLabel = new JLabel(" 已连接到服务器");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());

        // 布局
        setLayout(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);

        inputField.requestFocus();
        setVisible(true);
    }

    /** 发送聊天消息 */
    private void sendChatMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if ("/users".equals(text)) {
            sendMessage(text);
        } else if ("/quit".equals(text)) {
            sendMessage(text);
            close();
            System.exit(0);
        } else {
            sendMessage(text);
            chatArea.append("我: " + text + "\n");
        }
        inputField.setText("");
    }

    private void sendMessage(String msg) {
        if (writer != null) {
            writer.println(msg);
        }
    }

    private void close() {
        try {
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    /** 接收服务器消息的后台线程 */
    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String msg = line;

                    // 检查是否是在线用户列表消息
                    if (msg.startsWith("系统: 在线用户")) {
                        updateUserList(msg);
                        chatArea.append(msg + "\n");
                    } else {
                        chatArea.append(msg + "\n");
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    chatArea.append("系统: 与服务器断开连接\n");
                    statusLabel.setText(" 连接已断开");
                }
            }
        }

        private void updateUserList(String msg) {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                // 格式: "系统: 在线用户 (N人): user1 user2 ..."
                int colon = msg.lastIndexOf(": ");
                if (colon > 0) {
                    String names = msg.substring(colon + 2).trim();
                    if (!names.isEmpty()) {
                        for (String name : names.split(" ")) {
                            userListModel.addElement(name);
                        }
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
