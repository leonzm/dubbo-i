package com.pengshu.dubbo_i.restful.container;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;

/**
 * Created by pengshu on 2016/11/17.
 */
public class JettyBinder implements HttpBinder {

    @Override
    public HttpServer bind(URL url, HttpHandler handler) {
        return new JettyHttpServer(url, handler);
    }

}
