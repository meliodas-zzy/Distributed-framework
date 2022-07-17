package zzy.distributed.loadBalance.Impl;

import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.service.ProviderService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WeightPollingStrategy implements LoadBalanceStrategy {
    private int index = 0;
    private Lock lock = new ReentrantLock();

    @Override
    public ProviderService select(List<ProviderService> providerServiceList) {
        ProviderService service = null;
        try {
            lock.tryLock(10, TimeUnit.MILLISECONDS);
            List<ProviderService> list = new ArrayList<>();
            for (ProviderService providerService : providerServiceList) {
                int weight = providerService.getWeight();
                for (int i = 0; i < weight; ++ i) {
                    list.add(providerService.copy());
                }
            }

            if (index >= list.size()) {
                index = 0;
            }
            service = providerServiceList.get(index);
            index ++;
            return service;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return providerServiceList.get(0);
    }
}
