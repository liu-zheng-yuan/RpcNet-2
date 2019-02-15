package lzhy.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//消息类型注册中心
public class MessageRegistry {
    //String对应的是Message的type字段，Class<?>对应的是消息中object的class对象
    private Map<String, Class<?>> classMap = new ConcurrentHashMap<>();

    //1.客户端将希望收到的返回值的type和返回值的class储存进去
    //2.服务端将能提供的服务的type和输入参数的class储存进去
    public void register(String type, Class<?> clazz) {
        if (type == null || clazz == null) {
            System.out.println("注册返回类型出错");
            return;
        }
        classMap.put(type, clazz);
    }

    //获取type对应的class
    public Class<?> get(String type) {
        if (type == null || !classMap.containsKey(type)) {
            System.out.println("返回类型未注册：" + type);
            return null;
        }
        return classMap.get(type);
    }



}
