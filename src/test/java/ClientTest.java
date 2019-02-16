import lzhy.client.RpcClient;


public class ClientTest {
    public static void main(String[] args)  {
        RpcClient client = new RpcClient("localhost", 9000);
        //1.返回实现了多个接口的代理类的情况
        Class[] classes = {FibService.class, HelloService.class};
        FibService service1 = client.refer(FibService.class,classes);
        for (int i = 0; i <= 30; i++) {
            try {
                System.out.printf("fib(%d) = %s\n", i, service1.fib(i));
                Thread.sleep(100);
            } catch (Exception e) {
                i--; // retry
            }
        }
        //返回只实现了一个接口的代理类
        HelloService service2 = client.refer(HelloService.class);
        System.out.println(service2.hello());


        client.close();

    }
}
