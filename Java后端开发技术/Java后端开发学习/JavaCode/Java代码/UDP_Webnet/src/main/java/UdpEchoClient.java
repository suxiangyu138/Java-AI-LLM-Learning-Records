import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * UDP 客户端
 * 功能：发送数据报到服务端 9999 端口
 */
public class UdpEchoClient {
    public static void main(String[] args) {
        String serverIp = "127.0.0.1";
        int serverPort = 9999;
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverIp);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("请输入消息（输入 exit 退出）：");
                String userInput = scanner.nextLine();

                // 1. 创建发送数据报
                DatagramPacket sendPacket = new DatagramPacket(
                        userInput.getBytes(), userInput.getBytes().length,
                        serverAddr, serverPort
                );
                // 2. 发送数据报
                socket.send(sendPacket);

                // 3. 接收服务端返回的数据报
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);
                String serverMsg = new String(
                        receivePacket.getData(), 0, receivePacket.getLength()
                );
                System.out.println("服务端返回：" + serverMsg);

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}