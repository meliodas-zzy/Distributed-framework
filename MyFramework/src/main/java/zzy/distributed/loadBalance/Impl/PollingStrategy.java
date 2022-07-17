package zzy.distributed.loadBalance.Impl;

import zzy.distributed.loadBalance.LoadBalanceStrategy;
import zzy.distributed.service.ProviderService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PollingStrategy implements LoadBalanceStrategy {
    private int index = 0;
    private Lock lock = new ReentrantLock();

    @Override
    public ProviderService select(List<ProviderService> providerServiceList) {
        ProviderService service = null;
        try {
            lock.tryLock(10, TimeUnit.MILLISECONDS);
            if (index >= providerServiceList.size()) {
                index = 0;
            }
            service = providerServiceList.get(index);
            index ++;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        if (Objects.isNull(service)) {
            service = providerServiceList.get(0);
        }
        return service;
    }
}
