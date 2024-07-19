package exec;

import util.RequestData;
import util.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Execute implements Runnable {

    public static List<Long> list = Collections.synchronizedList(new ArrayList<>());
    public static AtomicLong totalTime = new AtomicLong(0);

    private SelectionKey key;

    public Execute(SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run() {
        int serviceId = (int) key.attachment();
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int num = 0;
        ByteBuffer buffer = ByteBuffer.allocate(Util.BUFFER_LEN);
        buffer.clear();
        RequestData data = null;

        int jsonLen = -1;
        byte[] jsonBytes = null;
        int index = 0;

        try {
            while((num = socketChannel.read(buffer)) > 0 || index < jsonLen) {
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
            buffer.clear();
        } catch (IOException e) {
            ExecServer.logger.info("接收数据错误");
            ExecServer.logger.info(e);
//            e.printStackTrace();
        }
        long time1 = System.currentTimeMillis();
        switch (serviceId) {    // 这三个方法，整点复杂度不太一样的，体现workload不同？为了方便，搞成一样的
            case 0: processService0(jsonBytes); break;
            case 1: processService1(jsonBytes); break;
//            case 2: processService2(jsonBytes); break;
            default: ExecServer.logger.info("错误的服务ID");
        }
        long time2 = System.currentTimeMillis();

        try {
            System.out.println("开始返回数据给:"+socketChannel.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] returnData = jsonBytes;
        index = 0;
        buffer.clear();
        buffer.putInt(returnData.length);
        while (index < returnData.length) {
            int len = Math.min(buffer.limit() - buffer.position(), returnData.length - index);
            buffer.put(returnData, index, len);
            buffer.flip();
            try {
                socketChannel.write(buffer);
                while (buffer.remaining() > 0) {
                    socketChannel.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer.clear();
            index += len;
        }
        try {
            socketChannel.shutdownOutput();         // ！！！！！ 这里必须加，不然可能导致阻塞
//            System.out.println("处理完成"+ socketChannel.getRemoteAddress());
            ExecServer.logger.info("处理完成"+ socketChannel.getRemoteAddress());
        } catch (IOException e) {
            ExecServer.logger.info("返回数据错误");
            ExecServer.logger.info(e);
//            e.printStackTrace();
        }
        long t = time2 - time1;
        list.add(t);
        long total = totalTime.addAndGet(t);
        System.out.println("平均处理时间 " + (double)total / list.size());
    }

    public static void processService0(byte[] json) {
        service();
    }
    public static void processService1(byte[] json) {
        service();
    }
    public static void processService2(byte[] json) {

    }

    public static void service() {  // 0.08s
        long cnt = 0;
        long start = System.currentTimeMillis();
        for (int i=1;i<=100;i++) {      // 1000000
            for (int j=1;j<=1000;j++) {
                cnt += Util.random.nextInt(1000);
            }
        }
//        System.out.println(cnt);
        long end = System.currentTimeMillis();
        double totalTime = (end - start)*1.0/1000.0;
//        System.out.println(totalTime);
    }
}
