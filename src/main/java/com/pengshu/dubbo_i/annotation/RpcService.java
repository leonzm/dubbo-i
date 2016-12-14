package com.pengshu.dubbo_i.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.pengshu.dubbo_i.server.RpcServer;

/**
 * 辅助dubbo的Service注解，提供额外的配置
 * @author pengshu
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RpcService {

	RpcServer.RestfulEnable restfulEnable() default RpcServer.RestfulEnable.defaul; // 配置单个服务是否启用restful服务，该配置会覆盖配置文件中的配置
	
}
