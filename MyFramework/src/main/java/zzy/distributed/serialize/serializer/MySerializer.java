package zzy.distributed.serialize.serializer;

public interface MySerializer {
    /**
     * 序列化
     * @param obj
     * @return
     * @param <T>
     */
    public <T> byte[] serialize(T obj);

    /**
     * 反序列化
     * @param data
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T deserialize(byte[] data, Class<T> clazz);
}
