package com.udpchat.chat;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.*;

/**
 * UDP聊天 — 点对点通信，每个实例既是发送方也是接收方
 */
public class UdpChatPeer extends JFrame {

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private String username;

    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel statusLabel;

    public UdpChatPeer() {
        initLogin();
    }

    private void initLogin() {
        JTextField nameField = new JTextField("Alice", 15);
        JTextField localPortField = new JTextField("9000", 6);
        JTextField remoteHostField = new JTextField("localhost", 15);
        JTextField remotePortField = new JTextField("9001", 6);

        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel("用户名:"));
        panel.add(nameField);
        panel.add(new JLabel("本地监听端口:"));
        panel.add(localPortField);
        panel.add(new JLabel("对方主机:"));
        panel.add(remoteHostField);
        panel.add(new JLabel("对方端口:"));
        panel.add(remotePortField);
        panel.add(new JLabel());

        String[] options = {"启动", "取消"};
        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
        JDialog dialog = pane.createDialog(this, "UDP聊天 - 配置");
        dialog.setVisible(true);

        if (!"启动".equals(pane.getValue())) {
            System.exit(0);
        }

        username = nameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名不能为空");
            initLogin();
            return;
        }

        try {
            int localPort = Integer.parseInt(localPortField.getText().trim());
            remotePort = Integer.parseInt(remotePortField.getText().trim());
            remoteAddress = InetAddress.getByName(remoteHostField.getText().trim());

            socket = new DatagramSocket(localPort);
            new Thread(new Receiver()).start();
            initMainUI();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "启动失败: " + e.getMessage());
            initLogin();
        }
    }

    private void initMainUI() {
        setTitle("UDP聊天 - " + username + " [本地:" + socket.getLocalPort()
                + " -> " + remoteAddress.getHostName() + ":" + remotePort + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                send("/quit");
                close();
            }
        });

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createTitledBorder("聊天消息"));

        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());

        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        statusLabel = new JLabel(" 监听端口: " + socket.getLocalPort()
                + " | 目标: " + remoteAddress.getHostName() + ":" + remotePort);
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        chatArea.append("系统: UDP聊天已启动，本地端口 " + socket.getLocalPort() + "\n");
        chatArea.append("系统: 正在与 " + remoteAddress.getHostName() + ":" + remotePort + " 通信\n");
        inputField.requestFocus();
        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        send(text);
        chatArea.append("我: " + text + "\n");
        inputField.setText("");
    }

    private void send(String msg) {
        try {
            byte[] data = msg.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
            socket.send(packet);
        } catch (Exception e) {
            chatArea.append("系统: 发送失败 - " + e.getMessage() + "\n");
        }
    }

    private void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /** 后台接收线程 */
    private class Receiver implements Runnable {
        @Override
        public void run() {
            try {
                byte[] buf = new byte[1024];
                while (!socket.isClosed()) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

                    if ("/quit".equals(msg)) {
                        SwingUtilities.invokeLater(() ->
                                chatArea.append("系统: 对方已断开连接\n"));
                    } else {
                        String from = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                        SwingUtilities.invokeLater(() ->
                                chatArea.append(from + ": " + msg + "\n"));
                    }
                }
            } catch (SocketException e) {
                // socket closed, normal shutdown
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        chatArea.append("系统: 接收异常 - " + e.getMessage() + "\n"));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UdpChatPeer::new);
    }
}
