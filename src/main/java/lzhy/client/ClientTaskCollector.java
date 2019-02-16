package lzhy.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lzhy.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//客户端的任务搜集器(也是Netty中的handler)
//客户端每次send发送消息,都是经过这个handler,调用eventloop线程的executor方法
//1.先把任务的id字段和rpcfuture对象保存在一个map中 2.真正发送出去writeandflush
//**因为channel是双向的,可以直接向ctx里写入,就能直接发出去
@ChannelHandler.Sharable
public class ClientTaskCollector extends ChannelInboundHandlerAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(ClientTaskCollector.class);

    //存发出消息OutputMessage的requestID和异步结果Rpcture对象的引用
    private ConcurrentHashMap<String, RpcFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    //为了实现重连,客户端对象(它与collector互相引用)
    private RpcClient client;
    //保存一个Netty连接中ChannelHandlerContext对象的引用
    private ChannelHandlerContext ctx;
    //连接关闭的错误类型
    private Throwable ConnectionClosed = new Exception("rpc 连接已被关闭,任务失败");

    public ClientTaskCollector(RpcClient rpcClient) {
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

    //客户端收到传来的结果,是ServerOutputMessage类型。从中读出返回结果
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ServerOutputMessage)) {
            LOG.error("客户端收到的返回信息不能被正确地解析成ServerOutputMessage类型");
            return;
        }
        ServerOutputMessage input = (ServerOutputMessage) msg;
        //todo 没有考虑异步的情况
        Object o = input.getResult();
        @SuppressWarnings("unchecked")
        RpcFuture<Object> future = (RpcFuture<Object>) pendingTasks.remove(input.getRequestId());
        if (future == null) {
            LOG.error("任务" + input.getRequestId() + "没有对应的future对象");
            return;
        }
        //判断如果返回的是异常的情况
        if (o instanceof Throwable) {
            future.fail((Throwable) o);
        } else {
            future.success(o);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("客户端连接出现异常");
        cause.printStackTrace();
        ctx.close();
    }

    public <T> RpcFuture<T> send(ClientOutputMessage output) {
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


    //生成代理类.在代理类的invoktionHandler中调用send发送ClientOutputMessage类型的请求
    //clazz 代表在多个接口的情况下，想要强转成的接口类型，利用泛型T表示
    //interfaceClazzs 代表希望获得的代理类实现的接口（必须包括clazz）
    @SuppressWarnings("unchecked")
    public <T> T refer(final Class<T> clazz,final Class[] interfaceClazzs) throws Exception {
        //todo 有可能是实现了多个接口.传入的应该是接口的class数组


        //这里默认把第一个接口的类加载器传进去
        T proxyInstance = (T)Proxy.newProxyInstance(interfaceClazzs[0].getClassLoader(), interfaceClazzs, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //通过ctx发送调用方法名、方法参数类型、参数
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                //有可能是实现了多个接口.interfacesName应该是多个接口的simpleName拼接而成
                ClientOutputMessage output = new ClientOutputMessage(UUID.randomUUID().toString(),InterfacesNameUtil.toNames(interfaceClazzs),methodName, parameterTypes, args);
                //内部类引用外部类的实例使用以下语法
                RpcFuture<Object> future = ClientTaskCollector.this.send(output);
                //同步地获得结果，考虑如果返回的是异常的情况
                //todo 没考虑异步情况
                Object result = null;
                try {
                    //如果返回的是结果，则正常获得；如果返回的是Throwable对象，则捕获
                    result = future.get();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }
        });

        LOG.info("已产生接口:{} 的代理对象", InterfacesNameUtil.toNames(interfaceClazzs));
        return proxyInstance;

    }
    public void close() {
        if (ctx != null) {
            ctx.close();
        }
    }





}
