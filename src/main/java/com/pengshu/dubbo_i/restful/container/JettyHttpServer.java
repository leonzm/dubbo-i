package com.pengshu.dubbo_i.restful.container;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;
import com.pengshu.dubbo_i.restful.protocol.JettyRpcHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pengshu on 2016/11/17.
 */
public class JettyHttpServer extends AbstractHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyHttpServer.class);

    private Server server;

    public JettyHttpServer(URL url, HttpHandler handler) {
        super(url, handler);

        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon(true);
        threadPool.setMaxThreads(threads);
        threadPool.setMinThreads(threads);

        server = new Server(threadPool);
        ServerConnector serverConnector = new ServerConnector(server);
        LOGGER.info("DubboI初始化Jetty容器线程池大小：{}", threads);

        int port = url.getPort(9090);
        serverConnector.setPort(port);
        LOGGER.info("DubboI初始化Jetty绑定端口：{}", port);

        server.setConnectors(new Connector[] {serverConnector});

        HandlerList handlers = new HandlerList();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new Slf4jRequestLog());
        handlers.setHandlers(new Handler[] {
                requestLogHandler,
                new JettyRpcHandler()
        });
        server.setHandler(handlers);

        try {
            server.start();
            LOGGER.info("DubboI 启动Jetty Rest服务成功");
        } catch (Exception e) {
            throw new IllegalStateException("DubboI 启动Jetty Rest服务失败，原因：" + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.warn("DubboI 停止Jetty Rest服务失败", e);
        }
    }

    @Override
    public boolean isClosed() {
        return server.isStopped();
    }

}
