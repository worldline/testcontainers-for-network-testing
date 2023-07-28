package com.worldline.network.examples;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SimpleNettyForwardProxy implements AutoCloseable
{
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * creates a new forward proxy that accepts connections on the bindPort and forwards them to targetHost and targetPort
     */
    public ChannelFuture start(final int bindPort, final String targetHost, final int targetPort)
    {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();

        // configure the server
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel chLeft) throws InterruptedException
            {

                // for each new connection, configure a client
                Bootstrap b = new Bootstrap();
                b.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel(SocketChannel chRight)
                    {
                        // wire the two pipelines (left = from the listener and right = to the target)
                        chLeft.pipeline().addLast(new ForwardingHandler(chRight));
                        chRight.pipeline().addLast(new ForwardingHandler(chLeft));
                    }
                });

                // connect to the proxy target
                b.connect(targetHost, targetPort).sync();
            }
        }).option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

        // start listening to the proxy port
        return b.bind(bindPort);
    }

    @Override
    public void close()
    {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    private static class ForwardingHandler extends ChannelDuplexHandler
    {
        private final Channel forwardChannel;

        public ForwardingHandler(Channel forwardChannel)
        {
            this.forwardChannel = forwardChannel;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg)
        {
            forwardChannel.writeAndFlush(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
