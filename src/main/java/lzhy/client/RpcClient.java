package lzhy.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lzhy.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//客户端连接管理,读写消息,连接重连
public class RpcClient {

    private final static Logger LOG = LoggerFactory.getLogger(RpcClient.class);
    private String serverIp; //服务器Ip
    private int serverPort;//服务器端口

    private Bootstrap bootstrap;
    private EventLoopGroup group;//客户端无bossGroup
    private ClientTaskCollector taskCollector;

    private volatile boolean started;
    private volatile boolean stopped;

    public RpcClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        this.init();

    }

    public void init() {
        //下面是boostrap启动
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup(1);//客户端只要一个线程负责发送就可以
        //下面配置
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        this.taskCollector = new ClientTaskCollector(this);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipe = channel.pipeline();
                //入栈Handler
                pipe.addLast(new ReadTimeoutHandler(60));
                pipe.addLast(MarshallingCodeCFactory.buildDecoder());
                //出站Handler
                //todo 好像netty自己支持MarshallEncoder 不用自己写工厂方法
                pipe.addLast(MarshallingCodeCFactory.buildEncoder());
                pipe.addLast(taskCollector);

            }
        });
        bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);
    }

    //连接到远程服务器
    public void connect() {
        bootstrap.connect(serverIp, serverPort).syncUninterruptibly();
    }

    //重连
    public void reconnect() {
        if (stopped) {
            return;
        }
        //获得异步连接之后的future对象
        ChannelFuture future = bootstrap.connect(serverIp, serverPort);
        //注册监听器,回调异步执行结果
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    return;
                }
                if (!stopped) {
                    group.schedule(() -> {
                        //由IO线程来每隔一段时间就重启
                        reconnect();
                    }, 1, TimeUnit.SECONDS);
                }
                LOG.error("重新连接失败 @ {}:{}", serverIp, serverPort, future.cause());
            }
        });
    }



//    //发送RPC请求,异步地
//    public <T> RpcFuture<T> sendAsync(String type, Object object) {
//        if (!started) {
//            connect();
//            started = true;
//        }
//        String requestId = UUID.randomUUID().toString();
//        OutputMessage output = new OutputMessage(requestId, type, object);
//        RpcFuture<T> future = taskCollector.send(output);
//        return future;
//    }
//
//    //同步地发送RPC请求,一定要等到收到结果,才能返回
//    public <T> T send(String type, Object object) {
//        //返回的直接是结果
//        RpcFuture<T> future = sendAsync(type, object);
//        try {
//            return future.get();
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RpcException(e);
//        }
//    }

    //产生代理对象，实际调用的是taskCollector的refer方法
    public <T> T refer(final Class<T> interfaceClass) {
        try {
            if (interfaceClass == null)
                throw new IllegalArgumentException("interface class 为 null");
            if (!interfaceClass.isInterface())
                throw new IllegalArgumentException(interfaceClass.getName() + " 应该是 interface class!");
            if (!started) {
                connect();
                started = true;
            }
            return taskCollector.refer(interfaceClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
    //关闭连接 主要是EventLoopGroup
    public void close() {
        stopped = true;
        taskCollector.close();
        group.shutdownGracefully().syncUninterruptibly();

    }

}
