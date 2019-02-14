import lzhy.client.RpcClient;


public class ClientTest {
    public static void main(String[] args)  throws InterruptedException{
        RpcClient client1 = new RpcClient("192.168.123.62", 9000);
        RpcClient client2 = new RpcClient("192.168.123.62", 9001);
        client1.register("fib_res", long.class);
        client2.register("exp_res", ExpResponse.class);
        for (int i = 0; i <= 30; i++) {
            try {
                System.out.printf("fib(%d) = %d\n", i, (Long)client1.send("fib",i));
                Thread.sleep(100);
            } catch (Exception e) {
                i--; // retry
            }
        }
        Thread.sleep(3000);
        for (int i = 0; i <= 30; i++) {
            try {
                ExpResponse res = client2.send("exp",new ExpRequest(2,i));
                Thread.sleep(100);
                System.out.printf("exp2(%d) = %d cost=%dns\n", i, res.getValue(), res.getCostInNanos());
            } catch (Exception e) {
                i--; // retry
            }
        }

        client1.close();
        client2.close();
    }
}
