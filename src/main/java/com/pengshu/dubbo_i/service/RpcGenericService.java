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
	
	private static Map<String, ReferenceConfig<GenericService>> references = new ConcurrentHashMap<String, ReferenceConfig<GenericService>>(); // <serviceName:version, reference>
	
	private GenericService genericService;
	
	private RpcGenericService() {}
	
	private RpcGenericService(GenericService genericService) {
		this.genericService = genericService;
	}
	
	public static RpcGenericService Create(String serviceName, String version) throws RpcServiceException {
		if (DubboI_Configuration.instance == null) {
			throw new RpcServiceException("还未初始化Dubbo配置");
		}
		if (Strings.isNullOrEmpty(serviceName) || Strings.isNullOrEmpty(version)) {
			return null;
		}
		ReferenceConfig<GenericService> reference;
		String k = serviceName.concat(":").concat(version);
		if (references.containsKey(k)) {
			reference = references.get(k);
		} else {
			reference = new ReferenceConfig<GenericService>(); // 该实例很重量，里面封装了所有与注册中心及服务提供方连接，请缓存
			reference.setApplication(DubboI_Configuration.instance.application);
			reference.setRegistry(DubboI_Configuration.instance.registry);
			reference.setProtocol(DubboI_Configuration.PROTOCOL_DUBBO);
			reference.setInterface(serviceName); // 弱类型接口名 
			reference.setVersion(version); 
			reference.setGeneric(true); // 声明为泛化接口
			
			references.put(k, reference);
		}
		 
		 
		GenericService genericService = reference.get(); // 用com.alibaba.dubbo.rpc.service.GenericService可以替代所有接口引用
		return new RpcGenericService(genericService);
	}
	
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
