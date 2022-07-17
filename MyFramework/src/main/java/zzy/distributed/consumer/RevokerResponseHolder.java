package zzy.distributed.consumer;

import zzy.distributed.service.MyResponse;
import zzy.distributed.service.MyResponseWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RevokerResponseHolder {

    //服务返回结果map
    private static final Map<String, MyResponseWrapper> responseMap = new ConcurrentHashMap<>();
    //清除过期的返回结果
    private static final ExecutorService removeExpireKeyExecutor = Executors.newSingleThreadExecutor();

    static {
        //删除超时未获取到结果的key，防止内存泄漏
        removeExpireKeyExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        for (Map.Entry<String, MyResponseWrapper> entry : responseMap.entrySet()) {
                            boolean isExpire = entry.getValue().isExpire();
                            if (isExpire) {
                                responseMap.remove(entry.getKey());
                            }
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void initResponseData(String requestUniqueKey) {
        responseMap.put(requestUniqueKey, MyResponseWrapper.of());
    }

    /**
     * Netty异步调用返回结果放入阻塞队列
     * @param response
     */
    public static void putResultValue(MyResponse response) {
        long currentTime = System.currentTimeMillis();
        MyResponseWrapper responseWrapper = responseMap.get(response.getUniqueKey());
        responseWrapper.setResponseTime(currentTime);
        responseWrapper.getResponseQueue().add(response);
        responseMap.put(response.getUniqueKey(), responseWrapper);
    }

    /**
     * 从阻塞队列中获取Netty异步返回的结果值
     * @param requestUniqueKey
     * @param timeout
     * @return
     */
    public static MyResponse getValue(String requestUniqueKey, long timeout) {
        MyResponseWrapper responseWrapper = responseMap.get(requestUniqueKey);
        try {
            return responseWrapper.getResponseQueue().poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            responseMap.remove(requestUniqueKey);
        }
    }
}
