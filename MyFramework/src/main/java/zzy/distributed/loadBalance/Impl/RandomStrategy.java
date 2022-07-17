package zzy.distributed.loadBalance.Impl;

import org.apache.commons.lang3.RandomUtils;
import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.service.ProviderService;

import java.util.List;

public class RandomStrategy implements LoadBalanceStrategy {
    @Override
    public ProviderService select(List<ProviderService> providerServiceList) {
        int MAX_LEN = providerServiceList.size();
        int index = RandomUtils.nextInt(0, MAX_LEN - 1);
        return providerServiceList.get(index);
    }
}
