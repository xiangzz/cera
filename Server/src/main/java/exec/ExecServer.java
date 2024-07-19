package exec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

public class ExecServer {
    public static int id;

    public static int port;

    public static ThreadPoolExecutor threadPoolExecutor0 = new ThreadPoolExecutor(4, 4, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.AbortPolicy());

    public static ThreadPoolExecutor threadPoolExecutor1 = new ThreadPoolExecutor(4, 4, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.AbortPolicy());

    public static Logger logger = LogManager.getLogger("asyncLoggerInfo");

    public static void main(String[] args) throws IOException {
        id = Integer.parseInt(args[0]);
//        port = Integer.parseInt(args[1]);
        port = 8899;

        System.setProperty("Log4jContextSelector",
                "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverChannel.socket();

        serverChannel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(port);

        serverSocket.bind(address);

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        logger.info("server "+id+" 就绪");
        assert AsyncLoggerContextSelector.isSelected(): "log4j2 的异步 disruptor启动失败";
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
//                    System.out.println("接收来自:" + socketChannel.getRemoteAddress() + " 的连接");
                    ExecServer.logger.info("接收来自:" + socketChannel.getRemoteAddress() + " 的连接");
                } else if (key.isReadable()) {
//                    System.out.println("启动线程，开始处理");
                    SocketChannel sc = (SocketChannel) key.channel();
                    ExecServer.logger.info("启动线程，开始处理 "+sc.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(4);
                    buffer.clear();
                    sc.read(buffer);
                    buffer.flip();
                    int serviceId = buffer.getInt();
                    key.attach(serviceId);
                    switch (serviceId) {    // 这里先读一个整数出来判断是哪个服务，然后交给对应的线程池来处理
                        case 0: threadPoolExecutor0.execute(new Execute(key)); break;
                        case 1: threadPoolExecutor1.execute(new Execute(key)); break;
                    }
                    key.channel().register(selector, 0, serviceId);  // 避免水平触发导致问题，直接关掉监听
                }
                it.remove();
            }
        }
    }
}
