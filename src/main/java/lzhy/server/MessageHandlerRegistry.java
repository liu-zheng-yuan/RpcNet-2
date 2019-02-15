package lzhy.server;

import lzhy.common.AbstractMessageHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//消息处理器注册中心
public class MessageHandlerRegistry {
    //默认的Handler，当找不到合适的，就用这个处理收到的消息
    public DefaultMessageHandler defaultMessageHandler = new DefaultMessageHandler();
    private Map<String, AbstractMessageHandler<?>> handlerMap = new ConcurrentHashMap<>();

    public void register(String type, AbstractMessageHandler<?> handler) {
        if (type == null || handler == null) {
            System.out.println("注册消息处理器出错");
            return;
        }
        handlerMap.put(type, handler);
    }

    public AbstractMessageHandler<?> get(String type) {
        if (type == null || !handlerMap.containsKey(type)) {
            System.out.println("消息处理器未注册：" + type);
            return null;
        }
        return handlerMap.get(type);
    }
}
