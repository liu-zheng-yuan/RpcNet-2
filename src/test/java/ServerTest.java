import io.netty.channel.ChannelHandlerContext;
import lzhy.common.AbstractMessageHandler;
import lzhy.common.OutputMessage;
import lzhy.server.RpcServer;

import java.util.ArrayList;
import java.util.List;

public class ServerTest {
    public static void main(String[] args) {
        RpcServer server1 = new RpcServer("localhost",9000, 10, 5);
        server1.registerService("fib", Integer.class, new FibRequestHandler());
        RpcServer server2 = new RpcServer("localhost",9001, 10, 5);
        server2.registerService("exp", ExpRequest.class, new ExpRequestHandler());
        new Thread(new Runnable() {
            @Override
            public void run() {
                server2.start();
            }
        }).start();
        server1.start();
    }

}

//斐波那契和指数计算处理
class FibRequestHandler implements AbstractMessageHandler<Integer> {

    private List<Long> fibs = new ArrayList<>();

    {
        fibs.add(1L); // fib(0) = 1
        fibs.add(1L); // fib(1) = 1
    }

    @Override
    public void handle(ChannelHandlerContext ctx, String requestId, Integer n) {
        for (int i = fibs.size(); i < n + 1; i++) {
            long value = fibs.get(i - 2) + fibs.get(i - 1);
            fibs.add(value);
        }
        //响应输出
        ctx.writeAndFlush(new OutputMessage(requestId, "fib_res", fibs.get(n)));
    }

}

class ExpRequestHandler implements AbstractMessageHandler<ExpRequest> {

    @Override
    public void handle(ChannelHandlerContext ctx, String requestId, ExpRequest message) {
        int base = message.getBase();
        int exp = message.getExp();
        long start = System.nanoTime();
        long res = 1;
        for (int i = 0; i < exp; i++) {
            res *= base;
        }
        long cost = System.nanoTime() - start;
        //响应输出
        ctx.writeAndFlush(new OutputMessage(requestId, "exp_res", new ExpResponse(res, cost)));
    }

}