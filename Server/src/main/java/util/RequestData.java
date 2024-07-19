package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;

@Data
public class RequestData {
    public int id;
    public long startTime;
    public long endTime;
    public int clientId;
    public int accessServerId;
    public int execServerId;
    public int serviceId;

    public byte[] data;

    public static ObjectMapper objectMapper = new ObjectMapper();

    public RequestData() {
        id = -1;
        startTime = -1;
        endTime = -1;
        clientId = -1;
        accessServerId = -1;
        execServerId = -1;
        serviceId = -1;
        int x = Util.random.nextDouble() < 0.5 ? -1: 1;
        int len = x * Util.random.nextInt(200000) + 2000000;
        data = new byte[len];
        for (int i=0;i<len;i++) {
            data[i] = (byte)('a' + Util.random.nextInt(26));
        }
    }

    public byte[] toJsonBytes() {
        byte[] xx = null;
        try {
            xx = objectMapper.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return xx;
    }

    public static RequestData toRequestData(byte[] bytes) {
        RequestData data = null;
        try {
            data = objectMapper.readValue(bytes, RequestData.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }


//    public static void main(String[] args) {
//        RequestData data = new RequestData();
//        System.out.println(data.data[0]);
//        byte[] xxx = data.toJsonBytes();
//        long start = System.currentTimeMillis();
//
////        for (int i=0;i<1000;i++)
////            Arrays.sort(data.data); // 循环1000次，耗时3s左右
////        String size = RamUsageEstimator.humanSizeOf(xxx);
////        System.out.println(size);
////        System.out.println(System.currentTimeMillis() - start);
////        System.out.println(data.toJson());
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            data=objectMapper.readValue(xxx, RequestData.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println(data.data[0]);
//
//    }
}
