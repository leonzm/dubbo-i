package com.pengshu.dubbo_i.restful.protocol;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.pengshu.dubbo_i.conf.DubboI_Configuration;
import com.pengshu.dubbo_i.restful.container.JettyHttpServer;
import com.pengshu.dubbo_i.restful.container.MetaCache;

/**
 * Created by freeman on 16/6/14.
 * 
 */
public class RestfulProxyProtocol extends AbstractProxyProtocol {
	
	private HttpBinder httpBinder;
	private JettyHttpServer jettyHttpServer;
	Protocol dubboProtocol;
	Runnable runnable = () -> {
		if (jettyHttpServer.isClosed()) {
			return;
		}
		jettyHttpServer.close();
	};

	/**
	 * spring自动注入
	 *
	 * @param httpBinder
	 */
	public void setHttpBinder(HttpBinder httpBinder) {
		this.httpBinder = httpBinder;
	}

	/**
	 * 容器回回调暴露方法 在此处对外暴露
	 *
	 * @param impl
	 * @param type
	 * @param url
	 * @param <T>
	 * @return
	 * @throws RpcException
	 */
	protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
		//检查方法是否有重载 如果有直接抛出异常ß
		check(type);
		if (jettyHttpServer == null) {
			jettyHttpServer = (JettyHttpServer) httpBinder.bind(url, (req, res) -> {});
		}

		HashMap<String, MetaCache> value = new HashMap<>(64);
		logger.info("export and cache ->  service : " + type.getName());
		Arrays.stream(type.getMethods()).forEach(method -> {
			MetaCache metaCache = new MetaCache(impl, type.getSimpleName(), method, method.getName());
			value.put(method.getName(), metaCache);
			Arrays.stream(method.getParameters()).forEach(parameter -> {
				logger.info(parameter.getName() + " " + parameter.getType());
				metaCache.getArguments().put(parameter.getName(), parameter.getType());
			});
		});
		
		JettyRpcHandler.metaCacheMap.put(type.getName(), value);
		return runnable;
	}

	private <T> void check(Class<T> type) throws RpcException {
		Method[] methods = type.getMethods();
		Set<String> methodSet = new HashSet<>();
		Arrays.stream(methods).forEach(method -> methodSet.add(method.getName()));
		if (!(methods.length == methodSet.size())) {
			throw new RpcException("It does not support overloaded methods");
		}
	}

	// 不支持作为java消费者
	@SuppressWarnings("unchecked")
	protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
		return (T) DubboProtocol.getDubboProtocol().refer(type, url);
	}

	@Override
	public int getDefaultPort() {
		return DubboI_Configuration.instance.getRestfulPort();
	}

}
