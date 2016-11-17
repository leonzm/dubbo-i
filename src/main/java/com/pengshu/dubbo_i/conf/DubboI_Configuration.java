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

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
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
	
	public static final String PROTOCOL_DUBBO = "dubbo";
	
	public ApplicationConfig application; // 当前应用配置
	public RegistryConfig registry; // 当前应用配置
	public ProtocolConfig protocol; // 服务提供者协议配置
	
	public static DubboI_Configuration instance;
	
	public static DubboI_Configuration newInstance(String dubboi_path) throws IOException, URISyntaxException {
		synchronized(DubboI_Configuration.class) {
			if (instance == null) {
				instance = new DubboI_Configuration();
				instance.initialization(dubboi_path);
				instance.initDubbo();
			}
		}
		return instance;
	}
	
	private DubboI_Configuration() {}
	
	private String applicationName;
	private String zookeeper;
	//private String kafka;
	private Integer dubboPort;
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
		registry.setAddress(zookeeper);
		
		protocol = new ProtocolConfig();
		protocol.setName(PROTOCOL_DUBBO);
		protocol.setPort(dubboPort);
		
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
