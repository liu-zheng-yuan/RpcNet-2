package lzhy.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lzhy.common.AbstractMessageHandler;
import lzhy.common.InputMessage;
import lzhy.common.MessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//服务器端的事件回调类。也是一个InBoundHandler。由它分配线程去处理接收到的请求，而不阻塞Netty的EventLoop线程。
@ChannelHandler.Sharable
public class ServerTaskCollector extends ChannelInboundHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(ServerTaskCollector.class);
    //线程池
    private ThreadPoolExecutor executor;
    //Handler注册中心
    private MessageHandlerRegistry handlerRegistry;
    //消息类型注册中心
    private MessageRegistry messageRegistry;

    //构造函数
    public ServerTaskCollector(MessageHandlerRegistry handlerRegistry, MessageRegistry messageRegistry, int workThreads) {
        System.out.println("=========2=============" + "ServerTaskCollector.构造");
        //业务队列最大1000，避免堆积
        //如果子线程处理不过来,io线程也会加入业务逻辑(callerRunsPolicy)Todo ???
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1000);
        //给业务线程命名
        ThreadFactory factory = new ThreadFactory() {
            //用原子类型记录线程数
            AtomicInteger index = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("rpcnet-" + index.getAndIncrement());
                return t;
            }
        };
        //闲置时间超过30秒就自动销毁
        this.executor = new ThreadPoolExecutor(1, workThreads, 30, TimeUnit.SECONDS, queue, factory, new ThreadPoolExecutor.CallerRunsPolicy());
        this.handlerRegistry = handlerRegistry;
        this.messageRegistry = messageRegistry;
    }

    //关闭这个类中所有连接和线程池
    public void closeGracefully() {
        //先通知关闭，再等待线程池中任务完成，再强行关闭
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
        this.executor.shutdownNow();
    }

    //新来一个连接时
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("新的连接已接入");
    }

    //断开一个连接时
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("连接已断开");
    }

    //收到客户端传来的请求时
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof InputMessage) {
            //收到客户端请求之后，交给线程池里线程执行
            this.executor.execute(()->{
                handleMessage(ctx,(InputMessage) msg);
            });
        } else {
            System.out.println("收到不能解析的客户端请求" + msg);
        }
    }
    //将msg交给线程池处理的具体逻辑
    private void handleMessage(ChannelHandlerContext ctx, InputMessage msg) {
        //从注册中心找到type对应的输入类型class
        Class<?> clazz = this.messageRegistry.get(msg.getType());
        //如果找不到，就说明RPCServer没有提供这个服务，就实用默认Handler处理
        if (clazz == null) {
            handlerRegistry.defaultMessageHandler.handle(ctx, msg.getRequestId(), msg);
            return;
        }
        //根据找到的输入值的类型，把msg中的json字符串形式的object对象转换成真正的Object类型的对象
        Object o = msg.getObject(clazz);
        //Todo 比较难看 等待解决 强转要检查
        @SuppressWarnings("unchecked")
        //调用合适的Handler的handle方法来解决输入问题
        AbstractMessageHandler<Object> handler = (AbstractMessageHandler<Object>) handlerRegistry.get(msg.getType());
        if (handler != null) {
            handler.handle(ctx,msg.getRequestId(),o);
        } else {
            handlerRegistry.defaultMessageHandler.handle(ctx, msg.getRequestId(), msg);
        }

    }

    // 此处可能因为客户端机器突发重启也可能是客户端链接闲置时间超时，后面的ReadTimeoutHandler抛出来的异常也可能是消息协议错误，序列化异常
    // 不管它，链接统统关闭，反正客户端具备重连机制
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.warn("连接错误");
        cause.printStackTrace();
        ctx.close();
    }
}
