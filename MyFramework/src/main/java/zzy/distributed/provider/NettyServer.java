package zzy.distributed.provider;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import sun.nio.ch.Net;
import zzy.distributed.serialize.common.SerializeType;
import zzy.distributed.serialize.serializer.NettyDecoderHandler;
import zzy.distributed.serialize.serializer.NettyEncoderHandler;
import zzy.distributed.service.MyRequest;
import zzy.distributed.utils.PropertyUtil;

import java.util.Objects;

public class NettyServer {
    private static NettyServer nettyServer = new NettyServer();

    private Channel channel;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private SerializeType serializeType = PropertyUtil.getSerializeType();

    private NettyServer() {
    }

    public static NettyServer getSingletonInstance() {
        return nettyServer;
    }

    public void start(final int port) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new NettyDecoderHandler(MyRequest.class, serializeType));
                        ch.pipeline().addLast(new NettyEncoderHandler(serializeType));
                        ch.pipeline().addLast(new NettyServerInvokeHandler());
                    }
                });
        try {
            channel = serverBootstrap.bind(port).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (Objects.isNull(channel)) {
            throw new RuntimeException("server stoped");
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channel.closeFuture().syncUninterruptibly();
    }
}
