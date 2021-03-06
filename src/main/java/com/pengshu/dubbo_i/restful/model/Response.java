package com.pengshu.dubbo_i.restful.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by pengshu on 2016/11/17.
 */
public class Response {

    public interface Code {
        int SUCCESS = 200;
        int SERVER_ERROR = 500;
    }

    private Object data;
    /**
     * 标示本次请求是否成功, 如果为false 那么error 和 errorType一定是有值
     */
    private boolean success = true;
    @JsonIgnoreProperties(value = {"cause", "detailMessage", "localizedMessage", "stackTrace", "suppressed"})
    private Object error;
    private String errorType;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

}
