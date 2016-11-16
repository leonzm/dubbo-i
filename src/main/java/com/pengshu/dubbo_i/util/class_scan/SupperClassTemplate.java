package com.pengshu.dubbo_i.util.class_scan;

/**
 * 用于获取子类的模板类
 *
 */
public abstract class SupperClassTemplate extends ClassTemplate {

    protected final Class<?> superClass;

    protected SupperClassTemplate(String packageName, Class<?> superClass) {
        super(packageName);
        this.superClass = superClass;
    }
}
