package lzhy.server;

import io.netty.channel.ChannelHandlerContext;
import lzhy.common.AbstractMessageHandler;
import lzhy.common.InputMessage;

public class DefaultMessageHandler implements AbstractMessageHandler<InputMessage> {

    @Override
    public void handle(ChannelHandlerContext ctx, String requestId, InputMessage message) {
        System.out.println(String.format("找不到合适的MessageHandler，编号为{}的请求信息被抛弃", requestId));
        System.out.println("该信息包含以下内容：" + message.toString());
    }
}
