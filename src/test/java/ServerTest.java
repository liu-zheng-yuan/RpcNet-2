import lzhy.server.RpcServer;

public class ServerTest {
    public static void main(String[] args) {
        RpcServer server = new RpcServer("localhost",9000,10,3);
        //1.实现了多个接口的实现类
        server.registerService(new DoubleServiceImpl());
        //2.只实现了一个接口的实现类，不能和上面的通用
        server.registerService(new HelloServiceImpl());
        server.start();
    }

}

