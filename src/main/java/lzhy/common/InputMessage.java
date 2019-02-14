package lzhy.common;

import com.alibaba.fastjson.JSON;

//从远程传输到本地的信息的封装，可以是参数或者返回值
public class InputMessage {
    private String requestId;//请求的唯一Id
    private String type;//请求的名称，用于从注册表中找到请求对应的类型的class
    private String object;//具体的内容，可以是方法的输入参数，也可以是方法的返回值，JSON格式

    public InputMessage(String requestId, String type, String object) {
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

    //根据输入值的类型，把msg中的json字符串形式的object对象转换成真正的Object类型的对象
    //传入的clazz是对应返回值类型的class（比如Integer.class，所以此时泛型T指的是Integer）
    public <T> T getObject(Class<T> clazz) {
        if (object == null) {
            return null;
        }
        return JSON.parseObject(object, clazz);
    }

    @Override
    public String toString() {
        return "lzhy.common.InputMessage{" +
                "requestId='" + requestId + '\'' +
                ", type='" + type + '\'' +
                ", object='" + object + '\'' +
                '}';
    }
}
