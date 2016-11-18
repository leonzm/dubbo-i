package com.pengshu.dubbo_i.exception;

/**
 * Created by pengshu on 2016/11/17.
 */
public class ServiceNotFoundException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5274029107440176459L;

	public ServiceNotFoundException(String message) {
        super(message);
    }

}
