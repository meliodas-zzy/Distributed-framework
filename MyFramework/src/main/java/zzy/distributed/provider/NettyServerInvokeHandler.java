package zzy.distributed.provider;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;
import zzy.distributed.register.RegisterCenter;
import zzy.distributed.register.RegisterCenter4Provider;
import zzy.distributed.service.MyRequest;
import zzy.distributed.service.MyResponse;
import zzy.distributed.service.ProviderService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class NettyServerInvokeHandler extends SimpleChannelInboundHandler<MyRequest> {

    private static final Map<String, Semaphore> serviceKeySemaphoreMap = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MyRequest request) throws Exception {
        if (ctx.channel().isWritable()) {
            ProviderService metaDataModel = request.getProviderService();
            long consumeTimeOut = request.getConsumeTimeout();
            final String methodName = request.getInvokedMethodName();

            //根据方法名称定位到服务提供者
            String serviceKey = metaDataModel.getServiceItf().getName();
            int workerThread = metaDataModel.getWorkerThreads();
            Semaphore semaphore = serviceKeySemaphoreMap.get(serviceKey);
            if (Objects.isNull(semaphore)) {
                synchronized (serviceKeySemaphoreMap) {
                    semaphore = serviceKeySemaphoreMap.get(serviceKey);
                    if (Objects.isNull(semaphore)) {
                        semaphore = new Semaphore(workerThread);
                        serviceKeySemaphoreMap.put(serviceKey, semaphore);
                    }
                }
            }

            //获取注册中心服务
            RegisterCenter4Provider registerCenter4Provider = RegisterCenter.getSingletonInstance();
            List<ProviderService> localProviderCaches = registerCenter4Provider.getProviderServiceMap().get(serviceKey);

            Object result = null;
            boolean acquire = false;

            try {
                ProviderService localProviderCache = Collections2.filter(localProviderCaches, new Predicate<ProviderService>() {
                    @Override
                    public boolean apply(ProviderService input) {
                        return StringUtils.equals(input.getServiceMethod().getName(), methodName);
                    }
                }).iterator().next();
                Object serviceObject = localProviderCache.getServiceObject();
                Method method = localProviderCache.getServiceMethod();
                acquire = semaphore.tryAcquire(consumeTimeOut, TimeUnit.MILLISECONDS);
                if (acquire) {
                    result = method.invoke(serviceObject, request.getArgs());
                }
            } catch (Exception e) {
                result = e;
            } finally {
                if(acquire) {
                    semaphore.release();
                }
            }

            //根据服务调用结果result组装返回对象
            MyResponse response = new MyResponse();
            response.setInvokeTimeout(consumeTimeOut);
            response.setUniqueKey(request.getUniqueKey());
            response.setResult(result);
            //返回给消费端
            ctx.writeAndFlush(response);
        }
    }
}
