package client;

import util.RequestData;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Request implements Runnable {
    public int serviceId = -1;

    public Request(int serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public void run() {
//        synchronized (UserClient.lock) {
        try {
            System.out.println("尝试连接");
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(UserClient.serverIp, UserClient.serverPort));
//                socketChannel.configureBlocking(false);
            System.out.println("连接上了");
            RequestData requestData = new RequestData(this.serviceId);
            requestData.id = UserClient.cntRequest.getAndIncrement();

            long startTime = System.currentTimeMillis();
            byte[] outData = requestData.toJsonBytes();

            ByteBuffer buffer = ByteBuffer.allocate(2048);
            buffer.clear();
            buffer.putInt(outData.length);
            buffer.putInt(requestData.serviceId);
            int index = 0;
            int jsonLen = outData.length;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (index < jsonLen) {
                int len = Math.min(buffer.limit() - buffer.position(), jsonLen - index);
                buffer.put(outData, index, len);
                buffer.flip();
                socketChannel.write(buffer);
                while (buffer.remaining() > 0) {
                    socketChannel.write(buffer);
                }
                buffer.clear();
                index += len;
            }
            socketChannel.shutdownOutput();


//            ByteBuffer buffer = ByteBuffer.allocate(outData.length + 8);    // 这里缓冲区开大了，可以整个小的。。 没事，懒得改了
//            buffer.putInt(outData.length);
//            buffer.putInt(requestData.serviceId);   // 这里发送这个请求是哪个服务的
//            buffer.put(outData);
//            buffer.flip();
//            System.out.println("开发");
//            socketChannel.write(buffer);    // 这里发送了json数据
//            System.out.println("发完了");
////            System.out.println("fasong");
//            socketChannel.shutdownOutput();     // ！！！！！ 这里必须加，不然可能导致阻塞
            System.out.println("请求发送完毕");
            // 这里是等回复数据的
            buffer.clear();
            int num = 0;
            jsonLen = -1;
            byte[] recivedData = null;
            index = 0;
            System.out.println("等待返回");

//                socketChannel.s

            while ((num = socketChannel.read(buffer)) > 0 || index < jsonLen) {
                buffer.flip();
                if (jsonLen == -1 && buffer.limit() >= 4) {
                    jsonLen = buffer.getInt();
                    recivedData = new byte[jsonLen];
                }
                int pos = buffer.position();
                buffer.get(recivedData, index, buffer.limit() - buffer.position());
                index += buffer.limit() - pos;
                buffer.clear();
                if (index >= jsonLen) break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            RequestData recivedJson = RequestData.toRequestData(recivedData);
//            System.out.println(recivedJson.id);
            long endTime = System.currentTimeMillis();
//            RequestData recivedData = RequestData.toRequestData(bytes);
            double latency = (endTime - startTime) / 1000.0;
            UserClient.latencyList.add(latency);
            System.out.printf("请求完成，用时 %f s\n", latency);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    }
}
