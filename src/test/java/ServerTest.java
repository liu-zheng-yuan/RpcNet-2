import lzhy.server.RpcServer;

public class ServerTest {
    public static void main(String[] args) {
        RpcServer server = new RpcServer("localhost",9000,10,3);
        server.registerService(new FibServiceImpl());
        server.start();
    }

}

