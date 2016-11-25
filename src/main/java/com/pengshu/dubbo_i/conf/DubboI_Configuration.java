package com.pengshu.dubbo_i.conf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.common.base.Strings;
import com.pengshu.dubbo_i.server.RpcServer;
import com.pengshu.dubbo_i.util.NativePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dubbo配置解析及其他全局配置
 * @author pengshu
 *
 */
public class DubboI_Configuration {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(DubboI_Configuration.class.getName());
	
	public final static ZoneId zoneid = ZoneId.of("GMT+08:00");
	public final static Charset charset = StandardCharsets.UTF_8;
	public final static String ip = getLocalhostIp();
	
	// 协议信息
	public static final String PROTOCOL_DUBBO = "dubbo";
	public static final String PROTOCOL_RESTFUL = "restful";
	public static final String PROTOCOL_RESTFUL_SERVER = "restful";
	public static final int PROTOCOL_RESTFUL_DEFAULT_THREADS = 200;
	public static final boolean PROTOCOL_RESTFUL_DEFAULT_ENABLE = true;
	
	public static final String LOANBALANCE_DEFAULT = Loadbalance.random.toString(); // 默认负载均衡为random
	public static final int ACCEPTS_DEFAULT = 0; // 服务提供方最大可接受连接数，0表示无限制
	public static final int CONNECTIONS_DEFAULT = 0; // 对每个提供者的最大连接数，0表示无限制
	public static final int EXECUTES_DEFAULT = 0; // 服务提供者每服务每方法最大可并行执行请求数，0表示无限制
	public static final int ACTIVES_DEFAULT = 0; // 每服务消费者每服务每方法最大并发调用数，0表示无限制
	public static final int RETRIES_DEFAULT = 2; // Failover容错模式中，远程服务调用重试次数，不包括第一次调用，不需要重试请设为0 
	public static final int TIMEOUT_DEFAULT = 0; // 服务方法调用超时时间(毫秒)，0表示无限制
	
	// 均衡负载，可选值：random,roundrobin,leastactive，分别表示：随机，轮循，最少活跃调用
	public static enum Loadbalance {   
		random, roundrobin, leastactive
	}
	
	public ApplicationConfig application; // 当前应用配置
	public RegistryConfig registry; // 当前应用配置
	public ProtocolConfig protocolDubbo; // 服务提供者协议配置（dubbo）
	public ProtocolConfig protocolRestful; // 服务提供者协议配置（restful）
	
	public static DubboI_Configuration instance;
	
	/**
	 * 初始化DubboI配置
	 * @param dubboi_path dubboi配置文件路径
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static DubboI_Configuration newInstance(String dubboi_path) throws IOException, URISyntaxException {
		return newInstance(dubboi_path, PROTOCOL_RESTFUL_DEFAULT_ENABLE);
	}
	
	/**
	 * 初始化DubboI配置
	 * @param dubboi_path dubboi配置文件路径
	 * @param restfulEnable 是否开启restful服务，默认为开启
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static DubboI_Configuration newInstance(String dubboi_path, boolean restfulEnable) throws IOException, URISyntaxException {
		synchronized(DubboI_Configuration.class) {
			if (instance == null) {
				instance = new DubboI_Configuration();
				instance.restfulEnable = restfulEnable; // restful
				instance.initialization(dubboi_path); // 加载配置文件
				instance.initDubbo(); // 初始化dubbo
			}
		}
		return instance;
	}
	
	private DubboI_Configuration() {}
	
	private String applicationName; // 应用程序名
	private String owner; // 应用负责人，用于服务治理，请填写负责人公司邮箱前缀
	private String zookeeper; // zk集群地址
	//private String kafka;
	private Integer dubboPort; // dubbo协议端口
	private Integer restfulPort = 9090; // restful端口
	private boolean restfulEnable; // restful是否启用
	private String version; // 应用程序版本
	private String loadbalance = LOANBALANCE_DEFAULT; // 应用程序均衡负载方式
	private int accepts = ACCEPTS_DEFAULT; //  服务端最大连接数，对整个应用程序而言
	private int connections = CONNECTIONS_DEFAULT; // 对每个提供者的最大连接数
	private int executes = EXECUTES_DEFAULT; // 服务提供者每服务每方法最大可并行执行请求数
	private int actives = ACTIVES_DEFAULT; // 每服务消费者每服务每方法最大并发调用数
	private int retries = RETRIES_DEFAULT; // 失败最大的重试次数，不含第一次调用
	private int timeout = TIMEOUT_DEFAULT; // 远程服务调用超时时间（毫秒）
	
	// ///////////////////////////////////// get //////////////////////////////////////////
	public String getApplicationName() {
		return applicationName;
	}

	public String getZookeeper() {
		return zookeeper;
	}

	public String getOwner() {
		return owner;
	}
	
	/*public String getKafka() {
		return kafka;
	}*/

	public Integer getDubboPort() {
		return dubboPort;
	}

	public Integer getRestfulPort() {
		return restfulPort;
	}

	public boolean isRestfulEnable() {
		return restfulEnable;
	}

	public String getVersion() {
		return version;
	}
	
	public String getLoadbalance() {
		return loadbalance;
	}
	
	public int getAccepts() {
		return accepts;
	}

	public int getConnections() {
		return connections;
	}

	public int getExecutes() {
		return executes;
	}
	
	public int getActives() {
		return actives;
	}

	public int getRetries() {
		return retries;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	// ///////////////////////////////////// initialization //////////////////////////////////////////
	private DubboI_Configuration initialization(String dubboi_path) throws IOException, URISyntaxException {
		Properties dubboi_properties = new Properties();
		try (InputStream resourceAsStream = Files.newInputStream(NativePath.get(dubboi_path), StandardOpenOption.READ);
				InputStreamReader inputstreamreader = new InputStreamReader(resourceAsStream, DubboI_Configuration.charset);) {
			dubboi_properties.load(inputstreamreader);
		}
		return initialization(dubboi_properties);
	}

	private DubboI_Configuration initialization(Properties dubboi_properties) throws IOException, URISyntaxException {
		LOGGER.info("#读取DubboI 配置:");
		dubboi_properties.forEach((k, v) -> {
			LOGGER.info(k + "=" + v);
		});

		// **** 必填 **** //
		applicationName = dubboi_properties.getProperty("dubboi.application");
		if (applicationName == null || applicationName.trim().isEmpty()) {
			LOGGER.error("application配置不能为空:dubboi.application");
			System.exit(-1);
		}
		owner = dubboi_properties.getProperty("dubboi.owner");
		if (owner == null || owner.trim().isEmpty()) {
			LOGGER.error("owner配置不能为空:dubboi.owner");
			System.exit(-1);
		}
		owner = owner.replaceAll("\\s*", "").trim(); // 有空格会报错
		zookeeper = dubboi_properties.getProperty("dubboi.zookeeper");
		if (zookeeper == null || zookeeper.trim().isEmpty()) {
			LOGGER.error("zookeeper配置不能为空:dubboi.zookeeper");
			System.exit(-1);
		}
		if (!zookeeper.matches("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+,?)+") || zookeeper.endsWith(",")) {
			LOGGER.error("zookeeper配置必须是这样的形式:'\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+'，如果有多个，以逗号分隔");
			System.exit(-1);
		}
		/*kafka = dubboi_properties.getProperty("dubboi.kafka");
		if (kafka == null || kafka.trim().isEmpty()) {
			LOGGER.warn("kafka配置为空:dubboi.kafka 调用链/日志等功能将不可用");
		}*/
		String dubbo_port = dubboi_properties.getProperty("dubboi.dubbo.port");
		if (dubbo_port == null || dubbo_port.trim().isEmpty()) {
			LOGGER.error("dubbo port配置不能为空:dubboi.dubbo.port");
			System.exit(-1);
		}
		if (!dubbo_port.matches("\\d+")) {
			LOGGER.error("dubbo port配置必须是整数");
			System.exit(-1);
		}
		dubboPort = Integer.parseInt(dubbo_port);
		String restful_port = dubboi_properties.getProperty("dubboi.restful.port");
		if (!Strings.isNullOrEmpty(restful_port)) {
			if (restful_port.matches("\\d+")) {
				restfulPort = Integer.parseInt(restful_port);
			} else {
				LOGGER.error("restful port配置必须是整数");
				System.exit(-1);
			}
		}
		version = dubboi_properties.getProperty("dubboi.version");
		if (version == null || version.trim().isEmpty()) {
			LOGGER.error("version配置不能为空:dubboi.version");
			System.exit(-1);
		}
		// **** 必填 **** //
		
		// **** 选填 **** //
		loadbalance = dubboi_properties.getProperty("dubboi.loadbalance");
		if (loadbalance != null && !loadbalance.trim().isEmpty() && getLoadbalance(loadbalance) != null) {
		} else {
			loadbalance = LOANBALANCE_DEFAULT;
		}
		String strAccepts = dubboi_properties.getProperty("dubboi.accepts");
		if (strAccepts != null && !strAccepts.trim().isEmpty() && strAccepts.matches("\\d+")) {
			accepts = Integer.parseInt(strAccepts);
		} else {
			accepts = ACCEPTS_DEFAULT;
		}
		String strConnections = dubboi_properties.getProperty("dubboi.connections");
		if (strConnections != null && !strConnections.trim().isEmpty() && strConnections.matches("\\d+")) {
			connections = Integer.parseInt(strConnections);
		} else {
			connections = CONNECTIONS_DEFAULT;
		}
		String strExecutes = dubboi_properties.getProperty("dubboi.executes");
		if (strExecutes != null && !strExecutes.trim().isEmpty() && strExecutes.matches("\\d+")) {
			executes = Integer.parseInt(strExecutes);
		} else {
			executes = EXECUTES_DEFAULT;
		}
		String strActives = dubboi_properties.getProperty("dubboi.actives");
		if (strActives != null && !strActives.trim().isEmpty() && strActives.matches("\\d+")) {
			actives = Integer.parseInt(strActives);
		} else {
			actives = ACTIVES_DEFAULT;
		}
		String strRetries = dubboi_properties.getProperty("dubboi.retries");
		if (strRetries != null && !strRetries.trim().isEmpty() && strRetries.matches("\\d+")) {
			retries = Integer.parseInt(strRetries);
		} else {
			retries = RETRIES_DEFAULT;
		}
		String strTimeout = dubboi_properties.getProperty("dubboi.timeout");
		if (strTimeout != null && !strTimeout.trim().isEmpty() && strTimeout.matches("\\d+") && Integer.parseInt(strTimeout) > 0) {
			timeout = Integer.parseInt(strTimeout);
		} else {
			timeout = TIMEOUT_DEFAULT;
		}
		// **** 选填 **** //
		
		return this;
	}
	
	private DubboI_Configuration initDubbo() {
		application = new ApplicationConfig();
		application.setName(applicationName);
		application.setOwner(owner);
		
		registry = new RegistryConfig();
		// 由于客户端配置zk的形式是：123.456.789.0:2181或123.456.789.0:2181:123.456.789.1:2181,123.456.789.2:2181，
		// 而dubbo需要的是zookeeper://10.20.153.10:2181?backup=10.20.153.11:2181,10.20.153.12:2181，故这里要做相应调整，如果是多个，首选随机
		StringBuffer zkAddress = new StringBuffer("zookeeper://");
		String[] zkSplit = zookeeper.split(",");
		if (zkSplit.length == 1) { // 只有一台，没得选
			zkAddress.append(zkSplit[0]);
		} else { // 首选随机
			String zkMaster = zkSplit[new Random().nextInt(zkSplit.length)];
			zkAddress.append(zkMaster);
			int slaveNum = 0;
			for (String zkSlave : zkSplit) {
				if (!zkMaster.equals(zkSlave)) {
					if (slaveNum == 0) {
						zkAddress.append("?");
					} else {
						zkAddress.append(",");
					}
					zkAddress.append(zkSlave);
					
					slaveNum ++;
				}
			}
		}
		LOGGER.info("#调整后的zookeeper配置：" + zkAddress.toString());
		registry.setAddress(zkAddress.toString());
		
		protocolDubbo = new ProtocolConfig(PROTOCOL_DUBBO, dubboPort);
		protocolDubbo.setAccepts(accepts); // 服务端最大连接数，对整个应用程序而言
		
		protocolRestful = new ProtocolConfig(PROTOCOL_RESTFUL, restfulPort);
		protocolRestful.setServer(PROTOCOL_RESTFUL_SERVER);
		
		return this;
	}
	
	/**
	 * 非spring方式使用dubbo服务，手动注册指定包下的服务
	 * @param packageName
	 */
	public void registerRpcServer(String packageName) {
		RpcServer.scanService(packageName);
	}
	
	/**
	 * 非spring方式使用dubbo服务，手动注册指定包下的服务
	 * @param packageNames
	 */
	public void registerRpcServer(String... packageNames) {
		if (packageNames != null && packageNames.length > 0) {
			for (String packageName : packageNames) {
				registerRpcServer(packageName);
			}
		}
	}
	
	/**
	 * 非spring方式使用dubbo服务，手动注册指定包下的服务
	 * @param packageNames
	 */
	public void registerRpcServer(List<String> packageNames) {
		if (packageNames != null && packageNames.size() > 0) {
			for (String packageName : packageNames) {
				registerRpcServer(packageName);
			}
		}
	}
	
	/**
	 * 根据字符串获取负载均衡的枚举对象，如果字符串不合乎该枚举，返回null
	 * @param name
	 * @return
	 */
	public static Loadbalance getLoadbalance(String name) {
		if (!Strings.isNullOrEmpty(name)) {
			try {
				return Loadbalance.valueOf(name);
			} catch(IllegalArgumentException e) {}
		}
		return null;
	}
	
	/**
	 * 获取本地ip
	 * @return
	 */
    private static String getLocalhostIp() {
        String ip = "未知";
        try{
            Enumeration<?> enumeration = NetworkInterface.getNetworkInterfaces();
            while(enumeration.hasMoreElements()) {
                NetworkInterface network = (NetworkInterface) enumeration.nextElement();
                Enumeration<?> nextNnumeration = network.getInetAddresses();
                while (nextNnumeration.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) nextNnumeration.nextElement();
                    if(inetAddress.isSiteLocalAddress()){
                        return inetAddress.getHostAddress();
                    }
                }
            }
            return ip;
        } catch(SocketException e) {
            LOGGER.warn("取得hostip失败", e);
            return ip;
        }
    }
    
}
