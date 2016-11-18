package com.pengshu.dubbo_i.restful.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pengshu.dubbo_i.exception.ServiceNotFoundException;
import com.pengshu.dubbo_i.restful.container.MetaCache;
import com.pengshu.dubbo_i.restful.model.Response;
import com.pengshu.dubbo_i.util.JsonUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pengshu on 2016/11/17.
 */
public class JettyRpcHandler extends AbstractHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyRpcHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final Map<String, Map<String, MetaCache>> metaCacheMap = new HashMap<>(64);

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        final Response responseData = new Response();
        Object data = null;
        try {
            if (!"application/json".equals(request.getContentType())) {
                throw new IllegalArgumentException("request content type must be application/json,your request content type is [" + request.getContentType() + "]");
            }

            MetaCache metaCache = findMeta(request); // 服务
            JsonNode node = getParameter(request); // 参数Json
            Map<String, Class<?>> arguments = metaCache.getArguments(); // 参数
            if (arguments.size() == 0) { // 无参数
                data = metaCache.invoke();
            } else { // 有参数
                Object[] parseArguments = parseArguments(node, arguments);
                data = metaCache.invoke(parseArguments);
            }

            responseData.setData(data);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Throwable e) {
            LOGGER.error("DubboI Jetty Handler处理异常", e);
            responseData.setErrorType(e.getClass().getName());
            responseData.setError(e);
            responseData.setSuccess(false);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            baseRequest.setHandled(true);
            response.getWriter().write(JsonUtil.toJSON(responseData));
        }
    }

    /**
     * 将Json形式的参数解析为对应的java参数
     * @param node
     * @param arguments
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Object[] parseArguments(JsonNode node, Map<String, Class<?>> arguments) throws ParseException, IOException {
        Object[] args = new Object[arguments.size()];
        int i = 0;
        for (String key : arguments.keySet()) {
            Class<?> type = arguments.get(key);
            JsonNode value = node.path(key);
            if (value.isMissingNode()) {
                throw new IllegalStateException("Parameter [" + key + "] must be exists");
            }
            if (value.isValueNode()) {
                args[i] = getBasicValue(key, value, type);
            } else if(type.isEnum()) {
                if (value.isNull()) {
                    args[i] = null;
                }
                if (value.isIntegralNumber()) {
                    Enum<?>[] enumConstants = ((Class<? extends Enum<?>>)type).getEnumConstants();
                    for (Enum<?> enumConstant : enumConstants) {
                        if (enumConstant.ordinal() == value.intValue()) {
                            args[i] = enumConstant;
                        }
                    }
                } else if (value.isTextual()) {
                    args[i] = Enum.valueOf((Class<? extends Enum>)type, value.textValue());
                }
                throw new IllegalArgumentException("Parameter [" + key + "] must be string or long or null");
            } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
                if (value.isNull()) {
                    args[i] = null;
                }
                if (!value.isArray()) {
                    throw new IllegalArgumentException("Parameter [" + key + "] must be array or null");
                }
                if (type.isArray()) {
                    args[i] = objectMapper.readValue(value.toString(), objectMapper.getTypeFactory().constructArrayType(type.getComponentType()));
                } else {
                    Class<?> aClass = (Class<?>) ((ParameterizedType) type.getGenericSuperclass()).getActualTypeArguments()[0];
                    args[i] = objectMapper.readValue(value.toString(), objectMapper.getTypeFactory().constructCollectionType((Class<? extends Collection>) type, aClass));
                }
            } else if (value.isObject()) {
                if (value.isNull()) {
                    args[i] = null;
                }
                args[i] = JsonUtil.fromJSON(value.toString(), type);
            } else {
                throw new IllegalArgumentException("Parameter [" + key + "] not support");
            }
            i ++;
        }
        return args;
    }

    /**
     * 根据type解析提交的Json参数
     * @param key
     * @param value
     * @param type
     * @return
     * @throws ParseException
     */
    private Object getBasicValue(String key, JsonNode value, Class<?> type) throws ParseException {
        if (int.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                throw new NullPointerException("Parameter [" + key + "] value must not be null");
            } else if (!value.isIntegralNumber()) {
                throw new IllegalStateException("Parameter [" + key + "] value must be int");
            }
            return value.intValue();
        } else if (Integer.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                return null;
            } else if (value.isIntegralNumber()) {
                return value.intValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Integer or null");
            }
        } else if (float.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                throw new NullPointerException("Parameter [" + key + "] value must not be null");
            } else if (!value.isNumber()) {
                throw new IllegalStateException("Parameter [" + key + "] value must be float");
            }
            return value.floatValue();
        } else if (Float.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                return null;
            } else if (value.isNumber()) {
                return value.floatValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Float or null");
            }
        } else if (double.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                throw new NullPointerException("Parameter [" + key + "] value must not be null");
            } else if (!value.isNumber()) {
                throw new IllegalStateException("Parameter [" + key + "] value must be double");
            }
            return value.doubleValue();
        } else if (Double.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                return null;
            } else if (value.isNumber()) {
                return value.doubleValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Double or null");
            }
        } else if (long.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                throw new NullPointerException("Parameter [" + key + "] value must not be null");
            } else if (!value.isNumber()) {
                throw new IllegalStateException("Parameter [" + key + "] value must be long");
            }
            return value.longValue();
        } else if (Long.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                return null;
            } else if (value.isNumber()) {
                return value.longValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Long or null");
            }
        } else if (boolean.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                throw new NullPointerException("Parameter [" + key + "] value must not be null");
            } else if (!value.isBoolean()) {
                throw new IllegalStateException("Parameter [" + key + "] value must be long");
            }
            return value.booleanValue();
        } else if (Long.class.isAssignableFrom(type)) {
            if (value.isNull()) {
                return null;
            } else if (value.isBoolean()) {
                return value.booleanValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Long or null");
            }
        } else if (String.class.isAssignableFrom(type)) {
        	if (value.isNull()) {
                return null;
            } else if (value.isTextual()) {
                return value.textValue();
            } else {
                throw new IllegalStateException("Parameter [" + key + "] value must be Long or null");
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (value.isNull())
                return null;
            else if (value.isTextual()) {
                String text = value.textValue();
                if (text.length() == "yyyy-MM-dd".length()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    return dateFormat.parse(text);
                } else if (text.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return dateFormat.parse(text);
                }
            } else if (value.isNumber()) {
                return new Date(value.longValue());
            }
            throw new IllegalArgumentException("Parameter [" + key + "] must be string or long or null");
        }
        throw new IllegalStateException("Parameter [" + key + "] can't parse");
    }

    /**
     * 查找参数son
     * @param request
     * @return
     * @throws IOException
     */
    private JsonNode getParameter(HttpServletRequest request) throws IOException {
        if (request.getMethod().equals("GET")) { // GET请求的调用参数名为parameters，内容为Json形式
            String body = request.getParameter("parameters");
            if (Strings.isNullOrEmpty(body)) {
                throw new NullPointerException("parameters must not be null");
            }
            return JsonUtil.fromJSON(body, JsonNode.class);
        }
        return JsonUtil.fromJSON(request.getInputStream(), JsonNode.class); // POST请求的参数为application/json
    }

    /**
     * 查找服务
     * @param request
     * @return
     * @throws ServiceNotFoundException
     */
    private MetaCache findMeta(HttpServletRequest request) throws ServiceNotFoundException {
        String service = request.getParameter("service");
        if (Strings.isNullOrEmpty(service)) {
            throw new NullPointerException("service name must not be null");
        }
        String method = request.getParameter("method");
        if (Strings.isNullOrEmpty(method)) {
            throw new NullPointerException("method name must not be null");
        }

        Map<String, MetaCache> stringMetaCacheMap = metaCacheMap.get(service);
        if (stringMetaCacheMap == null || stringMetaCacheMap.size() == 0) {
            throw new ServiceNotFoundException("service : [" + service + "] not found provider");
        }

        MetaCache metaCache = stringMetaCacheMap.get(method);
        if (metaCache == null) {
            throw new ServiceNotFoundException("service : [" + service + "] method name [" + method + "] not found provider");
        }
        return metaCache;
    }

}
