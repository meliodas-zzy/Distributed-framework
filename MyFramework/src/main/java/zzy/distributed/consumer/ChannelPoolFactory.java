package zzy.distributed.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.collections.CollectionUtils;
import zzy.distributed.serialize.common.SerializeType;
import zzy.distributed.serialize.serializer.NettyDecoderHandler;
import zzy.distributed.serialize.serializer.NettyEncoderHandler;
import zzy.distributed.service.MyResponse;
import zzy.distributed.service.ProviderService;
import zzy.distributed.utils.PropertyUtil;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author 郑占余
 *
 * 为了使channel能够复用，编写channel连接池工厂类
 */
public class ChannelPoolFactory {
    private static final ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory();
    //key为服务提供者地址，value为channel阻塞队列
    private static final Map<InetSocketAddress, ArrayBlockingQueue<Channel>> channelPoolMap = new ConcurrentHashMap<>();
    //channel阻塞队列长度，可配置
    private static final int channelConnectSize = PropertyUtil.getChannelConnectSize();
    //序列化协议，可配置
    private static final SerializeType serializeType = PropertyUtil.getSerializeType();
    //服务提供者列表
    private List<ProviderService> serviceMetaDataList = new ArrayList<>();

    private ChannelPoolFactory() {
    }

    public static ChannelPoolFactory getSingletonInstance() {
        return channelPoolFactory;
    }

    /**
     * 初始化channelPoolMap
     * @param providerMap
     */
    public void initChannelPoolFactory(Map<String, List<ProviderService>> providerMap) {
        //将服务提供者信息存入列表
        Collection<List<ProviderService>> collectionServiceMetaDataList = providerMap.values();
        for (List<ProviderService> serviceMetaDataModels : collectionServiceMetaDataList) {
            if (CollectionUtils.isEmpty(serviceMetaDataModels)) {
                continue;
            }
            serviceMetaDataList.addAll(serviceMetaDataModels);
        }

        //获取服务提供者地址列表
        Set<InetSocketAddress> socketAddressSet = new HashSet<>();
        for (ProviderService serviceMetaData : serviceMetaDataList) {
            String serviceIp = serviceMetaData.getServerIp();
            int servicePort = serviceMetaData.getServerPort();

            InetSocketAddress socketAddress = new InetSocketAddress(serviceIp, servicePort);
            socketAddressSet.add(socketAddress);
        }

        //根据服务提供者地址列表初始化channel阻塞队列，以地址为key，地址对应的channel阻塞队列为value存入channelPoolMap
        for (InetSocketAddress socketAddress : socketAddressSet) {
            try {
                int realChannelConnectSize = 0;
                while (realChannelConnectSize < channelConnectSize) {
                    Channel channel = null;
                    while (Objects.isNull(channel)) {
                        channel = registerChannel(socketAddress);
                    }
                    realChannelConnectSize ++;
                    ArrayBlockingQueue<Channel> channelArrayBlockingQueue = channelPoolMap.get(socketAddress);
                    if (Objects.isNull(channelArrayBlockingQueue)) {
                        channelArrayBlockingQueue = new ArrayBlockingQueue<Channel>(channelConnectSize);
                        channelPoolMap.put(socketAddress, channelArrayBlockingQueue);
                    }
                    channelArrayBlockingQueue.offer(channel);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ArrayBlockingQueue<Channel> acquire(InetSocketAddress socketAddress) {
        return channelPoolMap.get(socketAddress);
    }

    /**
     * channel用完后，回收到阻塞队列
     * @param arrayBlockingQueue
     * @param channel
     * @param inetSocketAddress
     */
    public void release(ArrayBlockingQueue<Channel> arrayBlockingQueue, Channel channel,
                        InetSocketAddress inetSocketAddress) {
        if (Objects.isNull(arrayBlockingQueue)) {
            return;
        }

        //回收前检查channel是否可用，不可用需要重新注册一个
        if (Objects.isNull(channel) || !channel.isActive() || !channel.isOpen() || !channel.isWritable()) {
            if (!Objects.isNull(channel)) {
                channel.deregister().syncUninterruptibly().awaitUninterruptibly();
                channel.closeFuture().syncUninterruptibly().awaitUninterruptibly();
            }
            Channel newChannel = null;
            while (Objects.isNull(newChannel)) {
                newChannel = registerChannel(inetSocketAddress);
            }
            arrayBlockingQueue.offer(newChannel);
            return;
        }
        arrayBlockingQueue.offer(channel);
    }

    public Channel registerChannel(InetSocketAddress socketAddress) {
        try {
            EventLoopGroup group = new NioEventLoopGroup(10);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.remoteAddress(socketAddress);

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new NettyEncoderHandler(serializeType));
                            ch.pipeline().addLast(new NettyDecoderHandler(MyResponse.class, serializeType));
                            ch.pipeline().addLast(new NettyClientInvokeHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect().sync();
            final Channel newChannel = future.channel();
            final CountDownLatch connectedLatch = new CountDownLatch(1);

            final List<Boolean> isSuccessHolder = new ArrayList<>();
            //监听channel是否创建成功
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (future.isSuccess()) {
                        isSuccessHolder.add(Boolean.TRUE);
                    } else {
                        future.cause().printStackTrace();
                        isSuccessHolder.add(Boolean.FALSE);
                    }
                }
            });

            connectedLatch.await();
            //创建成功，则返回创建的channel
            if (isSuccessHolder.get(0)) {
                return newChannel;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
