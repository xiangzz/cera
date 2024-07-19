package access;

import util.RequestData;
import util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AccessServer {

    public static int id;

    public static int port;

    public static double[][][] theta;

    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) throws IOException {
        id = Integer.parseInt(args[0]);
        port = Util.ACCESS_PORT;

        theta = new double[2][3][3];    // todo 设置theta
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    theta[i][j][k] = 1.0 / 3;
                }
            }
        }

        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();

        serverChannel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(port);

        serverSocket.bind(address);

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("就绪");
        // 6、轮询感兴趣的I/O就绪事件（选择键集合）
        while (selector.select() > 0) {
            if (null == selector.selectedKeys()) continue;
            // 7、获取选择键集合
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                // 8、获取单个的选择键，并处理
                SelectionKey key = it.next();
                if (null == key) continue;
                // 9、判断key是具体的什么事件，是否为新连接事件
                if (key.isAcceptable()) {
                    // 10、若接受的事件是“新连接”事件,就获取客户端新连接
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();
                    if (socketChannel == null) continue;
                    // 11、客户端新连接，切换为非阻塞模式
                    socketChannel.configureBlocking(false);
                    // 12、将客户端新连接通道注册到selector选择器上
                    SelectionKey selectionKey =
                            socketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("接收来自:" + socketChannel.getRemoteAddress() + " 的连接");
                } else if (key.isReadable()) {
                    System.out.println("启动线程，开始处理");
                    threadPoolExecutor.execute(new Routing(key));
                    key.channel().register(selector, 0, null);  // 避免水平触发导致问题，直接关掉监听
                }
                it.remove();
            }
        }
    }
}
