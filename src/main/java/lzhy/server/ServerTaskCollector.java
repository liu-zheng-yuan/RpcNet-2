package lzhy.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lzhy.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//服务器端的事件回调类。也是一个InBoundHandler。由它分配线程去处理接收到的请求，而不阻塞Netty的EventLoop线程。
@ChannelHandler.Sharable
public class ServerTaskCollector extends ChannelInboundHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(ServerTaskCollector.class);
    //线程池
    private ThreadPoolExecutor executor;
    //服务注册中心
    private ServiceRegistry registry;

    //构造函数
    public ServerTaskCollector(ServiceRegistry registry, int workThreads) {
        LOG.info("ServerTaskCollector 构造");
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
        this.registry = registry;
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
        LOG.info("新的连接已接入:"+ctx.channel().toString());
    }

    //断开一个连接时
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("连接已断开:"+ctx.channel().toString());
    }

    //收到客户端传来的请求时
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ClientOutputMessage) {
            //收到客户端请求之后，交给线程池里线程执行
            this.executor.execute(()->{
                handleMessage(ctx,(ClientOutputMessage) msg);
            });
        } else {
           LOG.error("收到不能解析的客户端请求" + msg);
        }
    }

    //将msg交给线程池处理的具体逻辑
    private void handleMessage(ChannelHandlerContext ctx, ClientOutputMessage msg) {
        try {
            //从注册中心找到接口名对应的实现类
            Object impl = this.registry.get(msg.getInterfacesName());
            Method method = impl.getClass().getMethod(msg.getMethodName(), msg.getParameterTypes());
            Object result = method.invoke(impl, msg.getArguements());
            ServerOutputMessage out = new ServerOutputMessage(msg.getRequestId(), result);
            ctx.writeAndFlush(out);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            //如果找不到，就说明RPCServer没有提供这个服务，就实用默认Handler处理
            ServerOutputMessage out = new ServerOutputMessage(msg.getRequestId(), e);
            ctx.writeAndFlush(out);
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
