package client;

import util.Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UserClient {

    public static final int USER_ID = 0;

    public static AtomicInteger cntRequest = new AtomicInteger(0);  // 记录发起几个请求

    public static List<Double> latencyList = Collections.synchronizedList(new ArrayList<>());   // 记录请求的时延

    public static String serverIp;

    public static int serverPort;

    public static double lambda = 3;

    public static int time = -1;    // 执行多少秒

    public static final Object lock = new Object();

    public static CountDownLatch latch = new CountDownLatch(Util.SERVICE_NUM); // 2个服务，2个延时线程

    public static Random random = new Random();

    public static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 3, 60,
            TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new ThreadPoolExecutor.CallerRunsPolicy());

    public static double nextSleepTime() {
        return -1.0/lambda * Math.log(random.nextDouble());
    }

    static class CalDelayAndRequest implements Runnable {
        private int serviceId = -1;
        public CalDelayAndRequest(int serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public void run() {
            long sleepTime = 0;
            long startTime = System.currentTimeMillis();
            long endTime = startTime + time * 1000;
            long lastTime = startTime;
            while (System.currentTimeMillis() < endTime) {
                sleepTime = (long)(nextSleepTime() * 1000);   // 毫秒
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("发起");
                threadPoolExecutor.execute(new Request(this.serviceId));  // 执行收发请求
//                System.out.println("间隔"+ (System.currentTimeMillis() - lastTime));
                lastTime = System.currentTimeMillis();
            }
            System.out.println("已超出时限");
            latch.countDown();  // 执行完成，门栓减一
        }
    }


    // 程序是可以超出时限后停止的，test_0.txt文件有被更新
    // 自动跑会卡在某个地方，似乎是网络阻塞了，但是在单机又没毛病
    // 点击调试的时候可以一直跑到主程序结束，但是整个程序不退出，而单机上可以退出
    // 在不退出的情况下可以看到有些请求发了一半，但是就停在那里了，整个流程似乎也不推进
    // 发送数据阻塞是个什么情况？
    public static void main(String[] args) throws InterruptedException, IOException {
        serverIp = args[0];     // 服务器IP
        lambda = Double.parseDouble(args[1]);   //泊松lambda
        time = Integer.parseInt(args[2]);   //运行时长
        serverPort = Util.ACCESS_PORT; //服务器端口
        for (int i=0;i < Util.SERVICE_NUM;i++) {
            new Thread(new CalDelayAndRequest(i)).start();
        }
        latch.await();  // 主线程等在这里
        Thread.sleep(5000); // 等待所有请求都结束
        threadPoolExecutor.shutdown();
        // 下面是对数据的统计，
        BufferedWriter out = new BufferedWriter(new FileWriter("test_"+ String.valueOf(USER_ID) +".txt"));
        out.write("cnt:" + String.valueOf(cntRequest)+"\n");
        double sum = 0;
        for (int i=0;i<latencyList.size();i++) {
            double tmp = latencyList.get(i);
            out.write(String.valueOf(tmp) + "\n");
            sum += tmp;
        }
        out.write("\navg:" + sum / latencyList.size());
        out.close();
    }
}
