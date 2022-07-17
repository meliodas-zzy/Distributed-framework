package zzy.distributed.serialize.common;

import org.apache.commons.lang3.StringUtils;

/**
 * @author 郑占余
 */
public enum SerializeType {

    JavaSerializer("JavaSerializer"),
    JacksonSerializer("JacksonSerializer"),
    HessianSerializer("HessianSerializer");

    private String serializeType;

    private SerializeType(String serializeType) {
        this.serializeType = serializeType;
    }

    public static SerializeType queryByType(String serializeType) {
        if (StringUtils.isBlank(serializeType)) {
            return null;
        }

        for (SerializeType serialize : SerializeType.values()) {
            if(StringUtils.equals(serializeType, serialize.getSerializeType())) {
                return serialize;
            }
        }
        return null;
    }

    public String getSerializeType() {
        return serializeType;
    }
}
