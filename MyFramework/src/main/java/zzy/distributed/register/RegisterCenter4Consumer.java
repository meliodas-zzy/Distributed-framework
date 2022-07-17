package zzy.distributed.register;

import zzy.distributed.service.ConsumerService;
import zzy.distributed.service.ProviderService;

import java.util.List;
import java.util.Map;

/**
 * @author 郑占余
 */
public interface RegisterCenter4Consumer {

    /**
     * 消费端初始化服务端服务信息的本地缓存
     *
     * @param remoteServiceKey
     * @param groupName
     */
    public void initProviderMap(String remoteServiceKey, String groupName);

    /**
     * 消费端获得服务提供者信息
     *
     * @return
     */
    public Map<String, List<ProviderService>> getServiceMetaData4Consumer();

    /**
     * 消费端将消费端信息注册到zookeeper
     *
     * @param consumerService
     */
    public void registerConsumer(final ConsumerService consumerService);

}
