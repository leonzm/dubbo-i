package com.pengshu.dubbo_i.server;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.pengshu.dubbo_i.conf.Configuration;

/**
 * Rpc服务加载、注册
 * @author pengshu
 *
 */
@Component
public class RpcServer implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Service service = bean.getClass().getAnnotation(Service.class);
        if (service != null) {
        	 ServiceBean<Object> serviceConfig = new ServiceBean<Object>(service);
             if (void.class.equals(service.interfaceClass()) && "".equals(service.interfaceName())) { // 检查接口
                 if (bean.getClass().getInterfaces().length > 0) {
                     serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
                 } else {
                     throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName() + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                 }
             }
             serviceConfig.setApplication(Configuration.application);
             serviceConfig.setRegistry(Configuration.registry);
             serviceConfig.setProtocol(Configuration.protocol);
             serviceConfig.setRef(bean);
             
             serviceConfig.export(); // 暴露及注册服务
        }
        return bean;
	}


}
