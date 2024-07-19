package access;

import lombok.SneakyThrows;
import util.RequestData;
import util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Routing implements Runnable {
    private SelectionKey key;
    public Routing(SelectionKey key) {
        this.key = key;
    }

    @SneakyThrows
    @Override
    public void run() {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int num = 0;
        ByteBuffer buffer = ByteBuffer.allocate(Util.BUFFER_LEN);
        buffer.clear();
        RequestData data = null;

        // 1. 接收客户端的数据
        int execId = -1;
        int jsonLen = -1;
        byte[] jsonBytes = null;
        int serviceId = -1;
        int index = 0;
        try {
            while ((num = socketChannel.read(buffer)) > 0 || index < jsonLen) {    // 这里可以正常把整个json读取完来处理吗?
                buffer.flip();                                  // 不会，这里按流发过来，每次取多少出来说不好的
                if (jsonLen == -1 && buffer.limit() >= 8) {
                    jsonLen = buffer.getInt();
                    jsonBytes = new byte[jsonLen];
                    serviceId = buffer.getInt();
                }
                int pos = buffer.position();
                buffer.get(jsonBytes, index, buffer.limit() - buffer.position());
                index += buffer.limit() - pos;
                buffer.clear();
                if (index >= jsonLen) break;
            }
//            data = RequestData.toRequestData(jsonBytes);  //不要这里，避免反序列化耗时
            int serverId = AccessServer.id;
            execId = -1;
            double[][][] theta = AccessServer.theta;
            double rand = Util.random.nextDouble();
            double[] serverList = theta[serviceId][serverId];
            for (int i = 0; i < serverList.length; i++) {
                rand -= serverList[i];
                if (rand < 0) {
                    execId = i;
                    break;
                }
            }
            buffer.clear();  // 切换到写模式
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.clear();  // 切换到写模式

        assert execId != -1;
        // 2. 到这里，已经知道service_id、exec_id了，转发数据给执行服务器
        // 获取execId对应服务器的IP地址
        String execIp = Util.EXEC_IP[execId];
//        String execIp = "127.0.0.1";

        int execPort = Util.EXEC_PORT;
        SocketChannel serverAsClientChannel = null;
        try {
            serverAsClientChannel = SocketChannel.open();
            serverAsClientChannel.socket().connect(
                    new InetSocketAddress(execIp, execPort));
//            serverAsClientChannel.configureBlocking(false);

            while (!serverAsClientChannel.finishConnect()) {
            }

            buffer.putInt(serviceId);
            buffer.putInt(jsonLen);
            index = 0;
            while (index < jsonLen) {
                int len = Math.min(buffer.limit() - buffer.position(), jsonLen - index);
                buffer.put(jsonBytes, index, len);
                buffer.flip();
                serverAsClientChannel.write(buffer);
                while (buffer.remaining() > 0) {
                    serverAsClientChannel.write(buffer);
                }
                buffer.clear();
                index += len;
            }
            serverAsClientChannel.shutdownOutput();     // 这里发给了执行服务器

//            Thread.sleep(300);

            // 3. 接收执行服务器返回数据的
            System.out.println("开始等待返回数据");
            buffer.clear();
            jsonLen = -1;
            index = 0;
            while ((num = serverAsClientChannel.read(buffer)) > 0 || index < jsonLen) {
                buffer.flip();
                if (jsonLen == -1 && buffer.limit() >= 4) {
                    jsonLen = buffer.getInt();
                    jsonBytes = new byte[jsonLen];
                }
                int pos = buffer.position();
                buffer.get(jsonBytes, index, buffer.limit() - buffer.position());
                index += buffer.limit() - pos;
                buffer.clear();
                if (index >= jsonLen) break;
            }
            System.out.println("接收数据完成");
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 4. 返回给客户端
        byte[] returnData = jsonBytes;
        index = 0;
        buffer.putInt(returnData.length);
        while (index < returnData.length) {
            int len = Math.min(buffer.limit() - buffer.position(), returnData.length - index);
            buffer.put(returnData, index, len);
            buffer.flip();
            int sent = socketChannel.write(buffer);
            while (buffer.remaining() > 0) {
                //System.out.println("some data is not sent");
                socketChannel.write(buffer);
            }
            buffer.clear();
            index += len;
        }
        socketChannel.shutdownOutput();         // ！！！！！ 这里必须加，不然可能导致阻塞
        System.out.println("处理完成"+ socketChannel.getRemoteAddress());
}
}
