package zzy.distributed.consumer;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import zzy.distributed.register.RegisterCenter;
import zzy.distributed.register.RegisterCenter4Consumer;
import zzy.distributed.service.ConsumerService;
import zzy.distributed.service.ProviderService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ConsumerFactoryBean implements FactoryBean, InitializingBean {

    //服务接口
    private Class<?> targetInterface;
    //超时时间
    private int timeout;
    //服务bean
    private Object serviceObject;
    //负载均衡策略
    private String LoadBalanceStrategy;
    //服务提供者唯一标识
    private String remoteAppKey;
    //服务分组组名
    private String groupName = "default";

    @Override
    public Object getObject() throws Exception {
        return serviceObject;
    }

    @Override
    public Class<?> getObjectType() {
        return targetInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        RegisterCenter4Consumer registerCenter4Consumer = RegisterCenter.getSingletonInstance();
        //初始化服务提供者列表到本地缓存
        registerCenter4Consumer.initProviderMap(remoteAppKey, groupName);
        //初始化channel
        Map<String, List<ProviderService>> providerMap = registerCenter4Consumer.getServiceMetaData4Consumer();
        if (MapUtils.isEmpty(providerMap)) {
            throw new RuntimeException("service provider list is empty");
        }
        ChannelPoolFactory.getSingletonInstance().initChannelPoolFactory(providerMap);

        //获取服务提供者代理对象
        ConsumerProxyBeanFactory proxyBeanFactory = ConsumerProxyBeanFactory.getSingletonInstance(targetInterface, timeout, LoadBalanceStrategy);
        this.serviceObject = proxyBeanFactory.getProxy();

        //将消费者信息注册到ZK
        ConsumerService consumerService = new ConsumerService();
        consumerService.setServiceItf(targetInterface);
        consumerService.setRemoteAppKey(remoteAppKey);
        consumerService.setGroupName(groupName);
        registerCenter4Consumer.registerConsumer(consumerService);
    }

    public Class<?> getTargetInterface() {
        return targetInterface;
    }

    public void setTargetInterface(Class<?> targetInterface) {
        this.targetInterface = targetInterface;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Object getServiceObject() {
        return serviceObject;
    }

    public void setServiceObject(Object serviceObject) {
        this.serviceObject = serviceObject;
    }

    public String getClusterStrategy() {
        return LoadBalanceStrategy;
    }

    public void setClusterStrategy(String clusterStrategy) {
        this.LoadBalanceStrategy = clusterStrategy;
    }

    public String getRemoteAppKey() {
        return remoteAppKey;
    }

    public void setRemoteAppKey(String remoteAppKey) {
        this.remoteAppKey = remoteAppKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
