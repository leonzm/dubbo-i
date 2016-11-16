package com.pengshu.dubbo_i.conf;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;

public class Configuration {

public static final String PROTOCOL_DUBBO = "dubbo";
	
	// 应用提供的配置
	public static String applicationName = "dubbo-i";
	public static String registerAddress = "zookeeper://127.0.0.1:2181";
	public static int dubboPort = 20880;
	
	public static ApplicationConfig application; // 当前应用配置
	public static RegistryConfig registry; // 当前应用配置
	public static ProtocolConfig protocol; // 服务提供者协议配置
	
	static {
		application = new ApplicationConfig();
		application.setName(applicationName);
		
		registry = new RegistryConfig();
		registry.setAddress(registerAddress);
		
		protocol = new ProtocolConfig();
		protocol.setName(PROTOCOL_DUBBO);
		protocol.setPort(dubboPort);
	}
	
}
