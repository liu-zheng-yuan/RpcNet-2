package lzhy.common;

public class InterfacesNameUtil {
    public static String toNames(Class<?>[] interfaceClazzs) {
        String interfacesName = "";
        for (Class<?> c : interfaceClazzs) {
            interfacesName += c.getSimpleName();
            interfacesName +="-";
        }
        return interfacesName;
    }
}
