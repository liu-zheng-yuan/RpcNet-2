package lzhy.client;

//定义客户端错误,用于统一抛出
public class RpcException extends RuntimeException {
    private static final long serialVersionID = 1L;

    public RpcException(String message,Throwable cause) {
        super(message,cause);
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(Throwable cause) {
        super(cause);

    }

}
