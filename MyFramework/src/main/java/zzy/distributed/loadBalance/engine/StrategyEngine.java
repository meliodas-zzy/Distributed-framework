package zzy.distributed.loadBalance.engine;

import zzy.distributed.loadBalance.Impl.*;
import zzy.distributed.loadBalance.LoadBalanceStrategy;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyEngine {
    private static final Map<StrategyEnum, LoadBalanceStrategy> strategyMap = new ConcurrentHashMap<>();

    static {
        strategyMap.put(StrategyEnum.Random, new RandomStrategy());
        strategyMap.put(StrategyEnum.WeightRondom, new WeightRandomStrategy());
        strategyMap.put(StrategyEnum.Polling, new PollingStrategy());
        strategyMap.put(StrategyEnum.WeightPolling, new WeightPollingStrategy());
        strategyMap.put(StrategyEnum.Hash, new HashStrategy());
    }

    public static LoadBalanceStrategy queryStrategy(String strategy) {
        StrategyEnum strategyEnum = StrategyEnum.queryByCode(strategy);
        if (Objects.isNull(strategyEnum)) {
            return new RandomStrategy();
        }

        return strategyMap.get(strategy);
    }
}
