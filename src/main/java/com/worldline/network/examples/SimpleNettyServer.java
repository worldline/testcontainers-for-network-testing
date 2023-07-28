package com.worldline.network.examples;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SimpleNettyServer implements AutoCloseable
{
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * binds the server to the port and initializes it with the submitted channel handlers
     *
     * @param port to bind the server to
     * @param handlers for the initChannel callback, added in order to the pipeline
     * @throws InterruptedException
     */
    public ChannelFuture bind(final int port, final ChannelHandler... handlers) throws InterruptedException
    {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(handlers);
            }
        }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

        return b.bind(port);
    }

    @Override
    public void close()
    {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
