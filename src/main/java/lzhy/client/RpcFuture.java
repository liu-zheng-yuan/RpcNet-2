package lzhy.client;

import java.util.concurrent.*;

public class RpcFuture<T> implements Future<T> {
    private T result;
    private Throwable error;
    //保证了要么成功收到结果,要么失败每收到结果,才会从get方法返回
    private CountDownLatch latch = new CountDownLatch(1);


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    //成功得到返回值
    public void success(T result) {
        //todo 考虑泛型的情况
        this.result = result;
        latch.countDown();
    }

    //失败了
    public void fail(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    @Override
    public boolean isDone() {
        //成功得到返回值或者失败了都认为是完成了
        return (result != null) || (error !=null);
    }



    @Override
    public T get() throws InterruptedException, ExecutionException,RpcException {
        //这句的意思是直到Rpcfuture对象被调用过success或者fail方法后才能从get返回
        latch.await(10, TimeUnit.SECONDS);
        //如果等待了10秒之后，result和error都没有接收到对象，说明出bug了
        if (error == null && result == null) {
            throw new RpcException("超时获取不到结果和error");
        }

        if (error != null) {
            throw new RpcException(error);
        }
        return result;

    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, unit);
        if (error != null) {
            throw new ExecutionException(error);
        }
        return result;
    }
}
