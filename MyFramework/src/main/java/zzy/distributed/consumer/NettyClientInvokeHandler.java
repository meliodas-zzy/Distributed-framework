package zzy.distributed.consumer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import zzy.distributed.service.MyResponse;

public class NettyClientInvokeHandler extends SimpleChannelInboundHandler<MyResponse> {

    public NettyClientInvokeHandler() {
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyResponse response) throws Exception {
        //将Netty异步返回的结果存入阻塞队列,以便调用端同步获取
        RevokerResponseHolder.putResultValue(response);
    }
}
