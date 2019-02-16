import lzhy.client.RpcClient;


public class ClientTest {
    public static void main(String[] args)  {
        RpcClient client = new RpcClient("localhost", 9000);

        TestService testService = null;
        try {
            testService = client.refer(TestService.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(fibService);
        for (int i = 0; i <= 30; i++) {
            try {
                System.out.printf("fib(%d) = %s\n", i, testService.test());
                Thread.sleep(100);
            } catch (Exception e) {
                i--; // retry
            }
        }
//        Thread.sleep(3000);
//        for (int i = 0; i <= 30; i++) {
//            try {
//                ExpResponse res = client2.send("exp",new ExpRequest(2,i));
//                Thread.sleep(100);
//                System.out.printf("exp2(%d) = %d cost=%dns\n", i, res.getValue(), res.getCostInNanos());
//            } catch (Exception e) {
//                i--; // retry
//            }
//        }
        client.close();

    }
}
