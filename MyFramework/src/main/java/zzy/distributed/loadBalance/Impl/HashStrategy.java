package zzy.distributed.loadBalance.Impl;

import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.service.ProviderService;
import zzy.distributed.utils.IpUtil;

import java.util.List;

public class HashStrategy implements LoadBalanceStrategy {
    @Override
    public ProviderService select(List<ProviderService> providerServiceList) {
        String localIP = IpUtil.localIp();
        int hashCode = localIP.hashCode();
        int size = providerServiceList.size();
        return providerServiceList.get(hashCode % size);
    }
}
