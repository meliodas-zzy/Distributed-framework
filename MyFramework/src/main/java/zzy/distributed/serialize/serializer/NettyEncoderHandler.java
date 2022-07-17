package zzy.distributed.serialize.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import zzy.distributed.serialize.common.SerializeType;
import zzy.distributed.serialize.engine.SerializeEngine;

public class NettyEncoderHandler extends MessageToByteEncoder {
    private SerializeType serializeType;

    public NettyEncoderHandler(SerializeType serializeType) {
        this.serializeType = serializeType;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        byte[] data = SerializeEngine.serialize(in, serializeType.getSerializeType());
        //写入长度用于解决拆包/粘包问题
        out.writeInt(data.length);
        out.writeBytes(data);
    }
}
