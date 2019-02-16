package lzhy.server;

import java.util.concurrent.ConcurrentHashMap;

//服务端注册中心，map中储存接口名和之前注册的接口实现类。
public class ServiceRegistry {
    private ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

    public void register(String name, Object serviceImpl) throws IllegalArgumentException {
        if (name == null || serviceImpl == null) {
            throw new IllegalArgumentException("注册服务失败：服务名或实现类为空");
        }
        if (map.containsKey(name) || map.containsValue(serviceImpl)) {
            throw new IllegalArgumentException("存在重复服务：" + name);
        }
        map.put(name, serviceImpl);
    }

    public Object get(String name) {
        if (name == null || !map.containsKey(name)) {
            throw new IllegalArgumentException("找不到服务或服务名为空");
        }
        return map.get(name);
    }
}
