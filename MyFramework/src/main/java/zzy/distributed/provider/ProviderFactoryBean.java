package zzy.distributed.provider;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import zzy.distributed.register.RegisterCenter;
import zzy.distributed.register.RegisterCenter4Provider;
import zzy.distributed.service.ProviderService;
import zzy.distributed.utils.IpUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ProviderFactoryBean implements FactoryBean, InitializingBean {

    //服务接口
    private Class<?> serviceItf;
    //服务实现
    private Object serviceObject;
    //服务端口
    private String serverPort;
    //服务超时时间
    private long timeout;
    //服务代理对象,暂时没有用到
    private Object serviceProxyObject;
    //服务提供者唯一标识
    private String appKey;
    //服务分组组名
    private String groupName = "default";
    //服务提供者权重,默认为1 ,范围为[1-100]
    private int weight = 1;
    //服务端线程数,默认10个线程
    private int workerThreads = 10;

    @Override
    public Object getObject() throws Exception {
        return serviceProxyObject;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceItf;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * InitializingBean中定义的方法，会在Spring Bean初始化前自动执行一次，这里的实现主要做了两件事
     * 1、启动Netty服务端，将服务对外发布出去
     * 2、将服务注册到ZK
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        NettyServer.getSingletonInstance().start(Integer.parseInt(serverPort));

        //注册到ZK
        List<ProviderService> providerServiceList = buildProviderServiceInfos();
        RegisterCenter4Provider registerCenter4Provider = RegisterCenter.getSingletonInstance();
        registerCenter4Provider.registerProvider(providerServiceList);
    }

    /**
     * 以方法为粒度获得providerServiceList
     *
     * @return
     */
    private List<ProviderService> buildProviderServiceInfos() {
        List<ProviderService> providerServiceList = new ArrayList<>();
        Method[] methods = serviceObject.getClass().getDeclaredMethods();
        for (Method method : methods) {
            ProviderService providerService = new ProviderService();
            providerService.setServiceItf(serviceItf);
            providerService.setServiceObject(serviceObject);
            providerService.setServerIp(IpUtil.localIp());
            providerService.setServerPort(Integer.parseInt(serverPort));
            providerService.setTimeout(timeout);
            providerService.setServiceMethod(method);
            providerService.setWeight(weight);
            providerService.setWorkerThreads(workerThreads);
            providerService.setAppKey(appKey);
            providerService.setGroupName(groupName);
            providerServiceList.add(providerService);
        }
        return providerServiceList;
    }

    public Class<?> getServiceItf() {
        return serviceItf;
    }

    public void setServiceItf(Class<?> serviceItf) {
        this.serviceItf = serviceItf;
    }

    public Object getServiceObject() {
        return serviceObject;
    }

    public void setServiceObject(Object serviceObject) {
        this.serviceObject = serviceObject;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Object getServiceProxyObject() {
        return serviceProxyObject;
    }

    public void setServiceProxyObject(Object serviceProxyObject) {
        this.serviceProxyObject = serviceProxyObject;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
