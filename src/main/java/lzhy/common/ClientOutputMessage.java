package lzhy.common;

import java.io.Serializable;
import java.util.Arrays;

public class ClientOutputMessage implements Serializable {
    private static final long serialVersionUID = 2L;

    private String requestId;//请求的唯一Id
    private String interfaceName;//请求的接口名 todo 有可能是实现了多个接口.interfaceName应该是多个接口的simpleName拼接而成 以后实现
    private String methodName;//请求的方法名
    private Class<?>[] parameterTypes;//方法参数类型
    private Object[] arguements;//方法参数

    public ClientOutputMessage(String requestId, String interfaceName, String methodName, Class<?>[] parameterTypes, Object[] arguements) {
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.arguements = arguements;
    }


    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArguements() {
        return arguements;
    }

    public void setArguements(Object[] arguements) {
        this.arguements = arguements;
    }

    @Override
    public String toString() {
        return "ClientOutputMessage{" +
                "requestId='" + requestId + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", arguements=" + Arrays.toString(arguements) +
                '}';
    }
}
