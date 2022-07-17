package zzy.distributed.service;

import java.io.Serializable;

public class MyResponse implements Serializable {
    //UUID,唯一标识一次返回值
    private String uniqueKey;
    //客户端指定的服务超时时间
    private long consumeTimeout;
    //接口调用返回的结果对象
    private Object result;

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public long getInvokeTimeout() {
        return consumeTimeout;
    }

    public void setInvokeTimeout(long invokeTimeout) {
        this.consumeTimeout = invokeTimeout;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
