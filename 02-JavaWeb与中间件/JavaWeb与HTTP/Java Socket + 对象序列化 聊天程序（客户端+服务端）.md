Java Socket + 对象序列化 聊天程序（客户端+服务端）
功能：
- 直接发送 Java 对象（不是字符串）
- 自动序列化 / 反序列化
- 界面用 Swing 展示消息
- 一台服务端，一台客户端
    1. 消息对象（可序列化）
    java  
    import java.io.Serializable;
    public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private String from;
    private String content;
    public Message(String from, String content) {
        this.from = from;
        this.content = content;
    }
    public String getFrom() {
        return from;
    }
    public String getContent() {
        return content;
    }
    }
 
2. 服务端（接收对象 + Swing 界面）
    java  
    import javax.swing.*;
    import java.awt.*;
    import java.io.ObjectInputStream;
    import java.net.ServerSocket;
    import java.net.Socket;
    public class ServerUI extends JFrame {
    private final JTextArea ta;
    public ServerUI() {
        setTitle("对象序列化服务端");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ta = new JTextArea();
        ta.setLineWrap(true);
        add(new JScrollPane(ta));
        setVisible(true);
        new Thread(this::startServer).start();
    }
    private void startServer() {
        try (ServerSocket ss = new ServerSocket(8888)) {
            append("服务端已启动，等待客户端连接...");
            Socket socket = ss.accept();
            append("客户端已连接！");
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Message msg = (Message) ois.readObject();
                append("[" + msg.getFrom() + "]：" + msg.getContent());
            }
        } catch (Exception e) {
            append("异常：" + e.getMessage());
        }
    }
    private void append(String text) {
        SwingUtilities.invokeLater(() -> ta.append(text + "\n"));
    }
    public static void main(String[] args) {
        new ServerUI();
    }
    }
 
3. 客户端（发送对象 + Swing 界面）
    java  
    import javax.swing.*;
    import java.awt.*;
    import java.io.ObjectOutputStream;
    import java.net.Socket;
    public class ClientUI extends JFrame {
    private final JTextArea ta;
    private final JTextField tf;
    private ObjectOutputStream oos;
    public ClientUI() {
        setTitle("对象序列化客户端");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        ta = new JTextArea();
        ta.setLineWrap(true);
        tf = new JTextField();
        add(new JScrollPane(ta), BorderLayout.CENTER);
        add(tf, BorderLayout.SOUTH);
        tf.addActionListener(e -> sendMessage());
        setVisible(true);
        connect();
    }
    private void connect() {
        try {
            Socket socket = new Socket("127.0.0.1", 8888);
            oos = new ObjectOutputStream(socket.getOutputStream());
            append("已连接服务端");
        } catch (Exception e) {
            append("连接失败");
        }
    }
    private void sendMessage() {
        String text = tf.getText().trim();
        if (text.isEmpty()) return;
        try {
            Message msg = new Message("客户端", text);
            oos.writeObject(msg); // 直接发送对象
            append("我说：" + text);
            tf.setText("");
        } catch (Exception e) {
            append("发送失败");
        }
    }
    private void append(String s) {
        SwingUtilities.invokeLater(() -> ta.append(s + "\n"));
    }
    public static void main(String[] args) {
        new ClientUI();
    }
    }
 
 
运行方法
1. 先运行 ServerUI
2. 再运行 ClientUI
3. 在客户端输入框按回车发送
4. 服务端收到的是完整 Java 对象！
 
这个程序的核心亮点
- 真正 对象序列化传输
- 不是字符串、不是字节拼接
- 带 Swing 界面，可直接交作业
- 代码规范、注释清晰、老师最爱
