package lzhy.common;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.Charset;
import java.util.List;

@ChannelHandler.Sharable
//这个泛型表示将要被编码的数据的类型，而不是编码后的
//这个类主要将OutputMessage类型编码成InputMessage类型的字节写入ByteBuf中
//注意编码前的object是Object类型,而不是字符串
public class MessageEncoder extends MessageToMessageEncoder<OutputMessage> {

    //规定:发出的OutputMessage中每个String属性之前一定有一个int，表示之后的String的字节数

    @Override
    public void encode(ChannelHandlerContext channelHandlerContext, OutputMessage msg, List<Object> list) throws Exception {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
        //依次将outputmessage的属性写入
        writeStr(buf, msg.getRequestId());
        writeStr(buf, msg.getType());
        //这里用JSON库，将Object转化成json格式的字符串
        writeStr(buf, JSON.toJSONString(msg.getObject()));
        list.add(buf);
    }

    //根据规定，在每个String前写入这个String的字节数
    private void writeStr(ByteBuf buf, String str) {
        buf.writeInt(str.length());
        buf.writeBytes(str.getBytes(Charset.forName("utf8")));
    }


}
