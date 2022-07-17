### java自带的序列化框架使用

* 需要序列化的对象需要实现serializable接口
* 需要借助ByteArrayOutputStream和ByteArrayInputStream
* 将ByteArrayOutputStream和ByteArrayInputStream传入到ObjectOutputStream和ObjectInputStream中
* 调用objectOutputStream.writeObject(obj)进行序列化
* 调用objectInputStream.readObject()进行反序列化

### 注意事项
* 父类实现了serializable后，子类自动实现序列化，不需要显式实现serializable接口
* 序列化时会把对象中引用的对象也一同序列化
* 某个字段被声明为transient后，默认序列化机制会忽略序列化这个字段
* java默认的序列化框架性能欠佳，序列化后码流过大，若引用过深还会导致OOM
