package lzhy.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.Charset;
import java.util.List;

//把读进来的字节解析成InputMessage类型,注意此时的object是json字符串
public class MessageDecoder extends ReplayingDecoder<InputMessage> {

    //规定:收到的InputMessage中每个String属性之前一定有一个int，表示之后的String的字节数

    private String readString(ByteBuf byteBuf) {
        //所以每次先读一个int，然后把之后int个字节转化成String
        int length = byteBuf.readInt();
        if (length < 0 || length > (1 << 20)) {
            throw new DecoderException("消息所含字符串太长。长度为 " + length);
        }
        byte[] arr = new byte[length];
        byteBuf.readBytes(arr);
        return new String(arr, Charset.forName("utf8"));

    }
    public void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        String requestId = this.readString(byteBuf);
        String type = this.readString(byteBuf);
        String object = this.readString(byteBuf);
        list.add(new InputMessage(requestId, type, object));

    }
}
