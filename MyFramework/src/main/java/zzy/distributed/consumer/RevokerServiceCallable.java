package zzy.distributed.consumer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import zzy.distributed.service.MyRequest;
import zzy.distributed.service.MyResponse;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RevokerServiceCallable implements Callable<MyResponse> {

    private Channel channel;
    private InetSocketAddress inetSocketAddress;
    private MyRequest request;

    public static RevokerServiceCallable of(InetSocketAddress inetSocketAddress, MyRequest request) {
        return new RevokerServiceCallable(inetSocketAddress, request);
    }

    public RevokerServiceCallable(InetSocketAddress inetSocketAddress, MyRequest request) {
        this.inetSocketAddress = inetSocketAddress;
        this.request = request;
    }

    @Override
    public MyResponse call() throws Exception {
        //初始化返回结果容器，将本次调用的唯一标识作为Key存入返回结果的map
        RevokerResponseHolder.initResponseData(request.getUniqueKey());
        //根据调用的服务提供者地址获取对应的channel队列
        ArrayBlockingQueue<Channel> blockingQueue = ChannelPoolFactory.getSingletonInstance()
                .acquire(inetSocketAddress);
        try {
            if (Objects.isNull(channel)) {
                channel = blockingQueue.poll(request.getConsumeTimeout(), TimeUnit.MILLISECONDS);
            }
            //若获取的channel不可用，则重新获取一个
            while (!channel.isOpen() || !channel.isActive() || !channel.isWritable()) {
                channel = blockingQueue.poll(request.getConsumeTimeout(), TimeUnit.MILLISECONDS);
                if (Objects.isNull(channel)) {
                    channel = ChannelPoolFactory.getSingletonInstance().registerChannel(inetSocketAddress);
                }
            }

            //将本次调用的信息写入通道，发起异步调用
            ChannelFuture channelFuture = channel.writeAndFlush(request);
            channelFuture.syncUninterruptibly();

            //从返回结果容器中获取返回结果
            long invokeTimeout = request.getConsumeTimeout();
            return RevokerResponseHolder.getValue(request.getUniqueKey(), invokeTimeout);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ChannelPoolFactory.getSingletonInstance().release(blockingQueue, channel, inetSocketAddress);
        }
        return null;
    }
}
