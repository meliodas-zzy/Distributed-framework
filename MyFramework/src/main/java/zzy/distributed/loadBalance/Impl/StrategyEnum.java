package zzy.distributed.loadBalance.Impl;

import org.apache.commons.lang3.StringUtils;

public enum StrategyEnum {

    Random("Random"),
    WeightRondom("WeightRandom"),
    Polling("Polling"),
    WeightPolling("WeightPolling"),
    Hash("Hash");

    private String code;
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private StrategyEnum(String code) {
        this.code = code;
    }

    public static StrategyEnum queryByCode(String code) {
        if(StringUtils.isBlank(code)) return null;
        for (StrategyEnum strategy : values()) {
            if (StringUtils.equals(code, strategy.getCode())) {
                return strategy;
            }
        }
        return null;
    }
}
