package zzy.distributed.consumer;

import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.loadBalance.engine.StrategyEngine;
import zzy.distributed.register.RegisterCenter;
import zzy.distributed.register.RegisterCenter4Consumer;
import zzy.distributed.service.MyRequest;
import zzy.distributed.service.MyResponse;
import zzy.distributed.service.ProviderService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConsumerProxyBeanFactory implements InvocationHandler {

    private ExecutorService fixedThreadPool = null;

    //服务接口
    private Class<?> targetInterface;
    //超时时间
    private int consumeTimeout;
    //调用者线程数
    private static int threadWorkerNumber = 10;
    //负载均衡策略
    private String loadBalanceStrategy;

    private static volatile ConsumerProxyBeanFactory singleton;

    public static ConsumerProxyBeanFactory getSingletonInstance(Class<?> targetInterface,
                                                                int consumeTimeout, String loadBalanceStrategy) {
        if (Objects.isNull(singleton)) {
            synchronized (ConsumerProxyBeanFactory.class) {
                if (Objects.isNull(singleton)) {
                    singleton = new ConsumerProxyBeanFactory(targetInterface, consumeTimeout, loadBalanceStrategy);
                }
            }
        }
        return singleton;
    }

    public ConsumerProxyBeanFactory(Class<?> targetInterface, int consumeTimeout, String loadBalanceStrategy) {
        this.targetInterface = targetInterface;
        this.consumeTimeout = consumeTimeout;
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public Object getProxy() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{targetInterface}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //根据接口获得服务提供者列表
        String serviceKey = targetInterface.getName();
        RegisterCenter4Consumer registerCenter4Consumer = RegisterCenter.getSingletonInstance();
        List<ProviderService> providerServices = registerCenter4Consumer.getServiceMetaData4Consumer().get(serviceKey);
        //根据负载均衡策略选择本次服务提供者
        LoadBalanceStrategy strategy = StrategyEngine.queryStrategy(loadBalanceStrategy);
        ProviderService providerService = strategy.select(providerServices);
        //设置本次服务调用的方法和接口
        ProviderService newProvider = providerService.copy();
        newProvider.setServiceMethod(method);
        newProvider.setServiceItf(targetInterface);

        final MyRequest request = new MyRequest();
        request.setUniqueKey(UUID.randomUUID().toString() + "-" + Thread.currentThread().getId());
        //设置本次调用的服务提供者信息
        request.setProviderService(newProvider);
        //设置本次调用的超时时间
        request.setConsumeTimeout(consumeTimeout);
        //设置本次调用的方法名称
        request.setInvokedMethodName(method.getName());
        //设置本次调用的方法参数信息
        request.setArgs(args);

        try {
            if (Objects.isNull(fixedThreadPool)) {
                synchronized (ConsumerProxyBeanFactory.class) {
                    if (Objects.isNull(fixedThreadPool)) {
                        fixedThreadPool = Executors.newFixedThreadPool(threadWorkerNumber);
                    }
                }
            }
            String serverIp = request.getProviderService().getServerIp();
            int serverPort = request.getProviderService().getServerPort();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverIp, serverPort);
            Future<MyResponse> responseFuture = fixedThreadPool.submit(RevokerServiceCallable.
                    of(inetSocketAddress, request));
            MyResponse response = responseFuture.get(request.getConsumeTimeout(), TimeUnit.MILLISECONDS);
            if (!Objects.isNull(response)) {
                return response.getResult();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
