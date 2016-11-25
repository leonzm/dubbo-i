package com.pengshu.dubbo_i.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.google.common.base.Strings;
import com.pengshu.dubbo_i.conf.DubboI_Configuration;
import com.pengshu.dubbo_i.exception.RpcServiceException;

/**
 * 对GenericService的封装，提供泛化的服务引用
 * @author pengshu
 *
 */
public class RpcGenericService {
	
	private static Map<String, ReferenceConfig<GenericService>> references = new ConcurrentHashMap<String, ReferenceConfig<GenericService>>(); // <serviceName:version:group, reference>
	
	private GenericService genericService;
	
	private RpcGenericService() {}
	
	private RpcGenericService(GenericService genericService) {
		this.genericService = genericService;
	}
	
	// ///////////////////////////////////// Create //////////////////////////////////////////
	public static RpcGenericService Create(String serviceName, String version) throws RpcServiceException {
		return Create(serviceName, version, "", DubboI_Configuration.instance.getLoadbalance(), DubboI_Configuration.instance.getConnections(), DubboI_Configuration.instance.getAccepts(), DubboI_Configuration.instance.getRetries(), DubboI_Configuration.instance.getTimeout());
	}
	
	public static RpcGenericService Create(String serviceName, String version, String group) throws RpcServiceException {
		return Create(serviceName, version, group, DubboI_Configuration.instance.getLoadbalance(), DubboI_Configuration.instance.getConnections(), DubboI_Configuration.instance.getAccepts(), DubboI_Configuration.instance.getRetries(), DubboI_Configuration.instance.getTimeout());
	}
	
	public static RpcGenericService Create(String serviceName, String version, String group, String loadbalance, int connections, int actives, int retries, int timeout) throws RpcServiceException {
		if (DubboI_Configuration.instance == null) {
			throw new RpcServiceException("还未初始化Dubbo配置");
		}
		if (Strings.isNullOrEmpty(serviceName) || Strings.isNullOrEmpty(version)) {
			return null;
		}
		ReferenceConfig<GenericService> reference;
		group = !Strings.isNullOrEmpty(group) ? group : "";
		String k = serviceName.concat(":").concat(version).concat(group);
		if (references.containsKey(k)) {
			reference = references.get(k);
		} else {
			reference = new ReferenceConfig<GenericService>(); // 该实例很重量，里面封装了所有与注册中心及服务提供方连接，请缓存
			reference.setApplication(DubboI_Configuration.instance.application);
			reference.setOwner(DubboI_Configuration.instance.getOwner());
			reference.setRegistry(DubboI_Configuration.instance.registry);
			reference.setProtocol(DubboI_Configuration.PROTOCOL_DUBBO); // 只调用指定协议的服务提供方，其它协议忽略
			reference.setInterface(serviceName); // 弱类型接口名 
			reference.setVersion(version);
			reference.setGroup(group);
			if (loadbalance != null && !loadbalance.trim().isEmpty() && DubboI_Configuration.getLoadbalance(loadbalance) != null) { // 负载均衡，服务消费方
				reference.setLoadbalance(loadbalance);
			} else {
				reference.setLoadbalance(DubboI_Configuration.instance.getLoadbalance());
			}
			if (connections >= 0) { // 对每个提供者的最大连接数
				reference.setConnections(connections);
			}
			if (actives >= 0) { // 每服务消费者每服务每方法最大并发调用数
				reference.setActives(actives);
			}
			if (retries >= 0) { // 远程服务调用重试次数，不包括第一次调用，不需要重试请设为0
				reference.setRetries(retries);
			}
			if (timeout > 0) { // 服务方法调用超时时间(毫秒)
				reference.setTimeout(timeout);
			}
			reference.setLazy(true); // 延迟连接，用于减少长连接数，当有调用发起时，再创建长连接
			reference.setGeneric(true); // 声明为泛化接口
			
			references.put(k, reference);
		}
		 
		GenericService genericService = reference.get(); // 用com.alibaba.dubbo.rpc.service.GenericService可以替代所有接口引用
		return new RpcGenericService(genericService);
	}
	
	// ///////////////////////////////////// invoke //////////////////////////////////////////
    public Object invoke(String method) throws RpcServiceException {
		return invoke(method, new String[] {}, new Object[] {});
	}
	
	public Object invoke(String method, String[] parameterTypes, Object[] args) throws RpcServiceException {
		if (parameterTypes == null) {
			throw new RpcServiceException("参数类型不能为空");
		}
		if (args  == null) {
			throw new RpcServiceException("参数值不能为空");
		}
		if (parameterTypes.length != args.length) {
			throw new RpcServiceException("参数类型与参数值不一致");
		}
		return genericService.$invoke(method, parameterTypes, args);
	}
	
	public Object invoke(String method, Object...parameters) throws RpcServiceException {
		if (parameters == null || parameters.length == 0) {
			invoke(method);
		}
		String[] parameterTypes = new String[parameters.length];
		for (int i = 0; i < parameters.length; i ++) {
			parameterTypes[i] = parameters[i].getClass().getTypeName();
		}
		return invoke(method, parameterTypes, parameters);
	}
	
	public Object invoke(String method, List<Object> parameters) throws RpcServiceException {
		String[] parameterTypes;
		Object[] args;
		if (parameters == null || parameters.size() == 0) {
			parameterTypes = new String[] {};
			args = new Object[] {};
		} else {
			parameterTypes = new String[parameters.size()];
			args = new Object[parameters.size()];
			
			for (int i = 0; i < parameters.size(); i ++) {
				Object param = parameters.get(i);
				parameterTypes[i] = param.getClass().getTypeName();
				args[i] = param;
			}
		}
		
		return genericService.$invoke(method, parameterTypes, args);
	}

}
