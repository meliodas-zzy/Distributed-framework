package zzy.distributed.serialize.engine;

import zzy.distributed.serialize.common.SerializeType;
import zzy.distributed.serialize.serializer.MySerializer;
import zzy.distributed.serialize.serializer.impl.HessianSerializer;
import zzy.distributed.serialize.serializer.impl.JacksonSerializer;
import zzy.distributed.serialize.serializer.impl.JavaSerializer;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 郑占余
 */
public class SerializeEngine {

    public static final Map<SerializeType, MySerializer> serializerMap = new ConcurrentHashMap<>();

    static {
        serializerMap.put(SerializeType.JavaSerializer, new JavaSerializer());
        serializerMap.put(SerializeType.HessianSerializer, new HessianSerializer());
        serializerMap.put(SerializeType.JacksonSerializer, new JacksonSerializer());
    }

    public static <T> byte[] serialize(T obj, String serializeType) {
        SerializeType serialize = SerializeType.queryByType(serializeType);
        if (Objects.isNull(serialize)) {
            throw new RuntimeException("serialize is null");
        }

        MySerializer serializer = serializerMap.get(serialize);
        if (Objects.isNull(serialize)) {
            throw new RuntimeException("serializer type not exists");
        }

        try {
            return serializer.serialize(obj);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz, String serializeType) {

        SerializeType serialize = SerializeType.queryByType(serializeType);
        if (serialize == null) {
            throw new RuntimeException("serialize is null");
        }
        MySerializer serializer = serializerMap.get(serialize);
        if (serializer == null) {
            throw new RuntimeException("serializer type not exists");
        }

        try {
            return serializer.deserialize(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
