package com.pengshu.dubbo_i.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.google.common.base.Strings;
import com.pengshu.dubbo_i.conf.DubboI_Configuration;
import com.pengshu.dubbo_i.conf.DubboI_Configuration.Loadbalance;
import com.pengshu.dubbo_i.util.class_scan.DefaultClassScanner;

/**
 * Rpc服务加载、注册
 * @author pengshu
 *
 */
@Component
public class RpcServer implements BeanPostProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);
	
	public static final Map<String, String> rpcServiceVersionMap = new HashMap<String, String>(); // <serviceName, version>

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		exportService(bean);
        return bean;
	}

	/**
	 * 加载指定包下的服务并暴露及注册服务
	 * @param packageName
	 */
	public static void scanService(String packageName) {
		if (!Strings.isNullOrEmpty(packageName)) {
			 List<Class<?>> clazzs = DefaultClassScanner.getInstance().getClassListByAnnotation(packageName, Service.class);
			 clazzs.stream().forEach(clazz -> {
				 try {
					Object bean = clazz.newInstance();
					exportService(bean);
				} catch (InstantiationException | IllegalAccessException e) {
					LOGGER.warn(clazz.getName() + "实例化异常", e);
				}
			 });
		}
	}
	
	/**
	 * 暴露及注册服务
	 * @param bean
	 * @return
	 */
	public static boolean exportService(Object bean) {
		if (bean != null) {
			Service service = bean.getClass().getAnnotation(Service.class);
	        if (DubboI_Configuration.instance != null && service != null) {
	        	 ServiceBean<Object> serviceConfig = new ServiceBean<Object>(service);
	             if (void.class.equals(service.interfaceClass()) && "".equals(service.interfaceName())) { // 检查接口
	                 if (bean.getClass().getInterfaces().length > 0) {
	                     serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
	                 } else {
	                     throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName() + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
	                 }
	             }
	             
	             serviceConfig.setApplication(DubboI_Configuration.instance.application);
	             serviceConfig.setRegistry(DubboI_Configuration.instance.registry);
	             if (DubboI_Configuration.instance.isRestfulEnable()) {
	            	 serviceConfig.setProtocols(Arrays.asList(DubboI_Configuration.instance.protocolDubbo, DubboI_Configuration.instance.protocolRestful)); // 开启dubbo服务、restful服务
	             } else {
	            	 serviceConfig.setProtocol(DubboI_Configuration.instance.protocolDubbo); // 开启dubbo服务
	             }
	             Loadbalance loadbalance = DubboI_Configuration.getLoadbalance(service.loadbalance());
	             if (loadbalance != null) { // 均衡负载，服务提供方
	            	 serviceConfig.setLoadbalance(loadbalance.toString());
	             } else {
	            	 serviceConfig.setLoadbalance(DubboI_Configuration.instance.loadbalance);
	             }
	             String version = service.version();
	             if (version != null && !version.trim().isEmpty()) { // 服务版本，注解中的版本可覆盖properties文件中的版本
	             } else {
	            	 version = DubboI_Configuration.instance.getVersion();
	             }
	             serviceConfig.setVersion(version);
	             rpcServiceVersionMap.put(bean.getClass().getInterfaces()[0].getName(), version);
	             serviceConfig.setRef(bean);
	             
	             serviceConfig.export(); // 暴露及注册服务
	             LOGGER.info("Dubbo-i 注册服务: " + bean.getClass().getName());
	             return true;
	        }
		}
		return false;
	}

}
