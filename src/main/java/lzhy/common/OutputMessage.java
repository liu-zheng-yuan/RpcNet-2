package lzhy.common;

//从本机发往远程主机的信息的封装，可以是参数或者返回值
public class OutputMessage {
    private String requestId;//请求的唯一Id
    private String type;//请求的名称，用于从注册表中找到请求对应的类型的class
    private Object object;//具体的内容，可以是方法的输入参数，也可以是方法的返回值

    public OutputMessage(String requestId, String type, Object object) {
        this.requestId = requestId;
        this.type = type;
        this.object = object;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getType() {
        return type;
    }

    public Object getObject() {
        return object;
    }
}
