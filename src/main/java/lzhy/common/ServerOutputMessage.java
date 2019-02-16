package lzhy.common;

import java.io.Serializable;

public class ServerOutputMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;//请求的唯一ID
    private Object result;//RPC返回的结果

    public ServerOutputMessage(String requestId, Object result) {
        this.requestId = requestId;
        this.result = result;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ServerOutputMessage{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                '}';
    }
}
