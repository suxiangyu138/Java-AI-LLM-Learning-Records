import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP 服务端
 * 功能：监听 9999 端口，接收客户端数据报并原样返回
 */
public class UdpEchoServer {
    public static void main(String[] args) {
        // 服务端端口
        int port = 9999;
        // 数据缓冲区大小（接收数据的字节数组）
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP 服务端已启动，监听端口 9999...");

            while (true) {
                // 1. 创建 DatagramPacket 接收数据
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                // 2. 阻塞接收数据报（receive() 方法等待客户端数据）
                socket.receive(receivePacket);
                // 3. 解析数据
                String clientMsg = new String(
                        receivePacket.getData(), 0, receivePacket.getLength()
                );
                InetAddress clientIp = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("客户端 [" + clientIp + ":" + clientPort + "] 发送：" + clientMsg);

                // 客户端发送 exit，关闭服务端
                if ("exit".equalsIgnoreCase(clientMsg)) {
                    String exitMsg = "服务端：连接关闭";
                    DatagramPacket sendPacket = new DatagramPacket(
                            exitMsg.getBytes(), exitMsg.getBytes().length,
                            clientIp, clientPort
                    );
                    socket.send(sendPacket);
                    break;
                }

                // 4. 原样返回数据给客户端
                String echoMsg = "服务端 Echo：" + clientMsg;
                DatagramPacket sendPacket = new DatagramPacket(
                        echoMsg.getBytes(), echoMsg.getBytes().length,
                        clientIp, clientPort
                );
                socket.send(sendPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}