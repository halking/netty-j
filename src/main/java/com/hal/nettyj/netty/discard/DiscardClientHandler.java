package com.hal.nettyj.netty.discard;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: Steven HUANG
 * @Date: 2019/5/13
 */
public class DiscardClientHandler extends SimpleChannelInboundHandler<Object> {

  private ByteBuf content;
  private ChannelHandlerContext ctx;
  long counter;
  private int size;

  public DiscardClientHandler(int size) {
    this.size = size;
  }

  private final ChannelFutureListener trafficGenerator = channelFuture -> {
    if (channelFuture.isSuccess()) {
      generateTraffic();
    } else {
      channelFuture.cause().printStackTrace();
      channelFuture.channel().close();
    }
  };

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
    // Server is supposed to send nothing, but if it sends something, discard it.
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;

    //Initialize the message
    content = ctx.alloc().directBuffer(size).writeBytes("Discard test".getBytes());

    //Send the initial messages
    generateTraffic();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    content.release();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    // Close the connection when an exception is raised.
    cause.printStackTrace();
    ctx.close();
  }

  private void generateTraffic() {
    //Flush the outbound buffer to socket.
    //Once flushed, generate the same amount of traffic again;
    ctx.writeAndFlush(content.retainedDuplicate()).addListener(trafficGenerator);
  }
}
