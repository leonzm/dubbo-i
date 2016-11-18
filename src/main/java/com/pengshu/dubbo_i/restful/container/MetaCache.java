package com.pengshu.dubbo_i.restful.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by pengshu on 2016/11/17.
 */
public class MetaCache {

    private Object service;
    private String serviceName;
    private Method method;
    private String methodName;
    private Map<String, Class<?>> arguments = new LinkedHashMap<>();

    public MetaCache(Object service, String serviceName, Method method, String methodName) {
        this.service = service;
        this.serviceName = serviceName;
        this.method = method;
        this.methodName = methodName;
    }

    public Object getService() {
        return service;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodName() {
        return methodName;
    }

    public Map<String, Class<?>> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Class<?>> arguments) {
        this.arguments = arguments;
    }

    public Object invoke() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(service);
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(service, args);
    }

}
