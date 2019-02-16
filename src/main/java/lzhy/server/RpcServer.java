package lzhy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lzhy.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

//Rpc服务器端，负责设置Netty的Bootstrap类、启动、退出
public class RpcServer {
    private final static Logger LOG = LoggerFactory.getLogger(RpcServer.class);

    private String ip;//服务器的ip地址，一般就是本机IP
    private int port;//提供服务的端口号，一个Server实例只提供一个端口上的服务
    private int workThreads;//处理业务的线程池中的线程数
    private int ioThreads;//EventLoopGroup中的负责处理IO的线程数（即Selector线程数）
    private ServiceRegistry registry = new ServiceRegistry();//提供的服务注册中心

    //保存Netty中的一些对象的引用，方便以后关闭
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup ioGroup;
    private ServerTaskCollector serverTaskCollector;
    private Channel serverChannel;


    public RpcServer(String ip,int port, int workThreads, int ioThreads) {

        //todo 自动获取ip只能获取到真实ip,而不会获取到127.0.0.1 如果客户端和服务端都搭载一台主机上,自动获取ip将会有bug 以后可以加入自动识别功能
//        try {
//            //先尝试获取局域网中本地真实ip地址
//            this.ip = InetAddress.getLocalHost().getHostAddress();
//        } catch (UnknownHostException e) {
//            System.out.println("获取Rpc服务器IP地址出错");
//            e.printStackTrace();
//        }
        //现阶段 客户端和服务端ip需要手动填写,要是localhost则都是localhost,要是真实ip则都是真实ip
        this.ip = ip;
        this.port = port;
        this.workThreads = workThreads;
        this.ioThreads = ioThreads;
    }

    //启动RPC服务
    public void start() {
        LOG.info("RPCServer start");
        //bossGroup：Acceptor线程池。负责监听端口的Socket连接，如果只有一个服务端端口需要监听，线程组线程数应该为1。
        bossGroup = new NioEventLoopGroup(1);
        //ioGroup：真正负责I/O读写操作的线程组。
        ioGroup = new NioEventLoopGroup(this.ioThreads);
        //TaskCollector具体执行业务逻辑的线程池也是一个Handler
        serverTaskCollector = new ServerTaskCollector(registry, workThreads);

        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,ioGroup); //注册EventLoopGroup线程组（本质上是个线程池）
            bootstrap.channel(NioServerSocketChannel.class);//指定使用 NIO 的传输 Channel
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipe = ch.pipeline();
                    //先注册入站的Handler
                    //如果客户端60秒没有任何请求,就关闭客户端连接
                    pipe.addLast(new ReadTimeoutHandler(60));
                    //加解码器
                    pipe.addLast(MarshallingCodeCFactory.buildDecoder());
                    //再注册出战的Handler
                    pipe.addLast(MarshallingCodeCFactory.buildEncoder());
                    //业务处理Handler
                    pipe.addLast(serverTaskCollector);
                }
            });
            //设置
            bootstrap.option(ChannelOption.SO_BACKLOG, 100)  //客户端套接字默认接受队列的大小
                    .option(ChannelOption.SO_REUSEADDR, true) //reuse addr 避免端口冲突
                    .option(ChannelOption.TCP_NODELAY, true)  //关闭小流合并,保证消息的及时性
                    .childOption(ChannelOption.SO_KEEPALIVE, true);  //长时间没动静的连接自动关闭
            //绑定端口 开始接受连接
            //bind返回一个ChannelFuture 获取这个future对象里的channel
            LOG.info("服务器启动 @ {}:{}\n", ip, port);
            serverChannel = bootstrap.bind(this.ip,this.port).channel();

        } catch (Exception e) {
            LOG.error("启动RPC服务器出错");
            e.printStackTrace();
        }
    }

    //关闭服务器
    public void stop() {
        // 先关闭服务端套件字
        serverChannel.close();
        // 再关闭消息来源，停止io线程池,shutdownGracefully是异步操作,需要阻塞直到关闭
        bossGroup.shutdownGracefully().syncUninterruptibly();
        ioGroup.shutdownGracefully().syncUninterruptibly();
        // 最后停止业务线程
        serverTaskCollector.closeGracefully();
    }

    //注册要暴露的服务服务
    public void registerService(Object serviceImpl) {
        if (serviceImpl == null) {
            LOG.error("注册服务失败,请确保三个参数不为空");
            return;
        }
        //key是（第一个）接口的名字，而不是实现类的名字
        this.registry.register(serviceImpl.getClass().getInterfaces()[0].getSimpleName(),serviceImpl);
        LOG.info("已注册接口：");
    }
}
