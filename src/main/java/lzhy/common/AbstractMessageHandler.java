package lzhy.common;

import io.netty.channel.ChannelHandlerContext;

public interface AbstractMessageHandler<T> {
    public void handle(ChannelHandlerContext ctx, String requestId, T message);
}
