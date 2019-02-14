package lzhy.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lzhy.common.InputMessage;
import lzhy.common.MessageRegistry;
import lzhy.common.OutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//客户端的任务搜集器(也是Netty中的handler)
//客户端每次send发送消息,都是经过这个handler,调用eventloop线程的executor方法
//1.先把任务的id字段和rpcfuture对象保存在一个map中 2.真正发送出去writeandflush
//**因为channel是双向的,可以直接向ctx里写入,就能直接发出去
@ChannelHandler.Sharable
public class ClientTaskCollector extends ChannelInboundHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(ClientTaskCollector.class);

    private MessageRegistry messageRegistry;
    //存发出消息OutputMessage的requestID和异步结果Rpcture对象的引用
    private ConcurrentHashMap<String, RpcFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    //为了实现重连,客户端对象(它与collector互相引用)
    private RpcClient client;
    //保存一个Netty连接中ChannelHandlerContext对象的引用
    private ChannelHandlerContext ctx;
    //连接关闭的错误类型
    private Throwable ConnectionClosed = new Exception("rpc 连接已被关闭,任务失败");

    public ClientTaskCollector(MessageRegistry messageRegistry, RpcClient rpcClient) {
        this.messageRegistry = messageRegistry;
        this.client = rpcClient;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        //连接一建立,就把ctx保存起来
        this.ctx = ctx;
    }

    //当连接失效时,尝试重连
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //原来的ctx肯定不能再用了,也要清除所有的任务
        this.ctx = null;
        pendingTasks.forEach((id,future)->{
            future.fail(ConnectionClosed);
        });
        pendingTasks.clear();
        //尝试重连,由io线程执行重连
        ctx.channel().eventLoop().schedule(() -> {
            client.reconnect();
        }, 1, TimeUnit.SECONDS);
    }

    //客户端收到传来的结果,是被编码成二进制的InputMessage类型
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof InputMessage)) {
            LOG.error("客户端收到的返回信息不能被正确地解析成InputMessage类型");
            return;
        }
        InputMessage input = (InputMessage) msg;
        //input里就是rpc调用的结果
        Class<?> clazz = messageRegistry.get(input.getType());
        if (clazz == null) {
            LOG.error("找不到合适的Class类型与返回值匹配");
            return;
        }
        Object o = input.getObject(clazz);
        @SuppressWarnings("unchecked")
        RpcFuture<Object> future = (RpcFuture<Object>) pendingTasks.remove(input.getRequestId());
        if (future == null) {
            LOG.error("任务" + input.getType() + "没有对应的future对象");
            return;
        }
        future.success(o);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("客户端连接出现异常");
    }

    public <T> RpcFuture<T> send(OutputMessage output) {
        RpcFuture<T> future = new RpcFuture<T>();
        if (ctx != null) {
            ctx.channel().eventLoop().execute(() -> {
                //发送消息时1.先把任务的id字段和rpcfuture对象保存在一个map中 2.真正发送出去writeandflush
                pendingTasks.put(output.getRequestId(), future);
                ctx.writeAndFlush(output);
            });

        } else {
            //没有ctx 就说明连接还没有建立
            future.fail(ConnectionClosed);
        }
        return future;
    }

    public void close() {
        if (ctx != null) {
            ctx.close();
        }
    }


}
