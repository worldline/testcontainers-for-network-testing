package com.worldline.network.examples;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SimpleNettyClient implements AutoCloseable
{
    private EventLoopGroup workerGroup;

    /**
     * connect the client to host:port and initializes it with the submitted channel handlers
     * @param host to connect the client to
     * @param port to connect the client to
     * @param handlers for the initChannel callback, added in order to the pipeline
     * @throws InterruptedException
     */
    public ChannelFuture connect(final String host, final int port, final ChannelHandler... handlers)
    {
        workerGroup = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch)
            {
                ch.pipeline().addLast(handlers);
            }
        });

        return b.connect(host, port);
    }

    @Override
    public void close()
    {
        workerGroup.shutdownGracefully();
    }
}
