package com.pengshu.dubbo_i.util;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Strings;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * Created by pengshu on 2016/11/18.
 * Javassist操作工具类
 */
public class JavassistUtil {

	private static final ClassPool pool = ClassPool.getDefault();
	
	/**
	 * 返回方法的参数名和参数类型
	 * @param clazz
	 * @param methodName
	 * @param parameterTypes
	 * @return <parameterName, parameterClass>
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws NotFoundException 
	 */
	public static Map<String, Class<?>> getParameterInfo(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException, SecurityException, NotFoundException {
		if (clazz == null || Strings.isNullOrEmpty(methodName)) {
			return null;
		}
		CtClass[] ctParameterTypes;
		if (parameterTypes == null) {
			ctParameterTypes = new CtClass[] {};
		} else {
			ctParameterTypes = new CtClass[parameterTypes.length];
			for (int i = 0; i < parameterTypes.length; i ++) {
				ctParameterTypes[i] = pool.get(parameterTypes[i].getName());
			}
		}
		CtClass ctClass = pool.get(clazz.getName());
		CtMethod ctMethod = ctClass.getDeclaredMethod(methodName, ctParameterTypes);
		
		// 使用javaassist的反射方法获取方法的参数名  
        MethodInfo methodInfo = ctMethod.getMethodInfo(); 
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();  
        LocalVariableAttribute attribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		
        Map<String, Class<?>> paramMap = new LinkedHashMap<String, Class<?>>();
        // 非静态方法，第一个参数为this
        int position = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;  
        for (int i = 0; i < parameterTypes.length; i++) {
        	String parameterName = attribute.variableName(i + position); // 非静态方法从第二个开始
        	paramMap.put(parameterName, parameterTypes[i]);
        }
        
		return paramMap;
	}
	
}
