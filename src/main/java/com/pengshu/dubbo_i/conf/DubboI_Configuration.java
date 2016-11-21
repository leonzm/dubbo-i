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
	
	// 均衡负载，可选值：random,roundrobin,leastactive，分别表示：随机，轮循，最少活跃调用
	public static enum Loadbalance {   
		random, roundrobin, leastactive
	}
	
	public ApplicationConfig application; // 当前应用配置
	public RegistryConfig registry; // 当前应用配置
	public ProtocolConfig protocolDubbo; // 服务提供者协议配置（dubbo）
	public ProtocolConfig protocolRestful; // 服务提供者协议配置（restful）
	public String loadbalance = Loadbalance.random.toString(); // 组在均衡，默认使用random
	
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
		return DubboI_Configuration.newInstance(dubboi_path, restfulEnable, Loadbalance.random);
	}
	
	/**
	 * 初始化DubboI配置
	 * @param dubboi_path dubboi配置文件路径
	 * @param loadbalance 服务端的均衡负载
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static DubboI_Configuration newInstance(String dubboi_path, Loadbalance loadbalance) throws IOException, URISyntaxException {
		return DubboI_Configuration.newInstance(dubboi_path, PROTOCOL_RESTFUL_DEFAULT_ENABLE, loadbalance);
	}
	
	/**
	 * 初始化DubboI配置
	 * @param dubboi_path dubboi配置文件路径
	 * @param restfulEnable 是否开启restful服务，默认为开启
	 * @param loadbalance 服务端的均衡负载
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static DubboI_Configuration newInstance(String dubboi_path, boolean restfulEnable, Loadbalance loadbalance) throws IOException, URISyntaxException {
		synchronized(DubboI_Configuration.class) {
			if (instance == null) {
				instance = new DubboI_Configuration();
				instance.restfulEnable = restfulEnable; // restful
				if (loadbalance != null) { // 均衡负载
					instance.loadbalance = loadbalance.toString();
				}
				instance.initialization(dubboi_path); // 加载配置文件
				instance.initDubbo(); // 初始化dubbo
			}
		}
		return instance;
	}
	
	private DubboI_Configuration() {}
	
	private String applicationName;
	private String zookeeper;
	//private String kafka;
	private Integer dubboPort;
	private Integer restfulPort = 9090;
	private boolean restfulEnable;
	private String version;
	
	// ///////////////////////////////////// get //////////////////////////////////////////
	public String getApplicationName() {
		return applicationName;
	}

	public String getZookeeper() {
		return zookeeper;
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

		applicationName = dubboi_properties.getProperty("dubboi.application");
		if (applicationName == null || applicationName.trim().isEmpty()) {
			LOGGER.error("application配置不能为空:dubboi.application");
			System.exit(-1);
		}
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
		
		return this;
	}
	
	private DubboI_Configuration initDubbo() {
		application = new ApplicationConfig();
		application.setName(applicationName);
		
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
	 * 根据字符串获取负载均衡的枚举对象
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
