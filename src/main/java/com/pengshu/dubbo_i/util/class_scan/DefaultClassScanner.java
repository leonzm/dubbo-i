package com.pengshu.dubbo_i.util.class_scan;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 默认类扫描器
 *
 */
public class DefaultClassScanner implements ClassScanner {
	
	private DefaultClassScanner() {}
	
	private static class InnerInstance {
		private static final DefaultClassScanner instance = new DefaultClassScanner();
	}
	
	public static final DefaultClassScanner getInstance() {
		return InnerInstance.instance;
	}

    @Override
    public List<Class<?>> getClassList(String packageName) {
        return new ClassTemplate(packageName) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(packageName);
            }
        }.getClassList();
    }

    @Override
    public List<Class<?>> getClassListByAnnotation(String packageName, Class<? extends Annotation> annotationClass) {
        return new AnnotationClassTemplate(packageName, annotationClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.isAnnotationPresent(annotationClass);
            }
        }.getClassList();
    }

    @Override
    public List<Class<?>> getClassListBySuper(String packageName, Class<?> superClass) {
        return new SupperClassTemplate(packageName, superClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
            }
        }.getClassList();
    }
}
