package zzy.distributed.serialize.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import zzy.distributed.serialize.common.SerializeType;
import zzy.distributed.serialize.engine.SerializeEngine;

import java.io.Serializable;
import java.util.List;

public class NettyDecoderHandler extends ByteToMessageDecoder {

    //解码对象类型
    private Class<?> genericClass;
    //解码对象使用的序列化类型
    private SerializeType serializeType;

    public NettyDecoderHandler(Class<?> genericClass, SerializeType serializeType) {
        this.genericClass = genericClass;
        this.serializeType = serializeType;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (dataLength < 0) {
            ctx.close();
        }
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);

        Object obj = SerializeEngine.deserialize(data, genericClass, serializeType.getSerializeType());
        out.add(obj);
    }
}
