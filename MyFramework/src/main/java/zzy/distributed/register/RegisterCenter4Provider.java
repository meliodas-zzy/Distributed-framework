package zzy.distributed.register;

import zzy.distributed.service.ProviderService;

import java.util.List;
import java.util.Map;

/**
 * @author 郑占余
 */
public interface RegisterCenter4Provider {

    /**
     *
     *
     * @param serviceMetaData
     */
    public void registerProvider(final List<ProviderService> serviceMetaData);

    /**
     * 服务端获取服务提供者的信息
     *
     * @return
     */
    public Map<String, List<ProviderService>> getProviderServiceMap();

}
