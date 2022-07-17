package zzy.distributed.loadBalance.Impl;

import org.apache.commons.lang3.RandomUtils;
import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.service.ProviderService;

import java.util.ArrayList;
import java.util.List;

public class WeightRandomStrategy implements LoadBalanceStrategy {
    @Override
    public ProviderService select(List<ProviderService> providerServiceList) {
        List<ProviderService> list = new ArrayList<>();
        for (ProviderService providerService : providerServiceList) {
            int weight = providerService.getWeight();
            for (int i = 0; i < weight; ++ i) {
                list.add(providerService.copy());
            }
        }

        int MAX_LEN = list.size();
        int index = RandomUtils.nextInt(0, MAX_LEN - 1);
        return list.get(index);
    }
}
