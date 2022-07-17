package zzy.distributed.loadBalance;

import zzy.distributed.service.ProviderService;

import java.util.List;

/**
 * @author 郑占余
 */
public interface LoadBalanceStrategy {
    public ProviderService select(List<ProviderService> providerServiceList);
}
