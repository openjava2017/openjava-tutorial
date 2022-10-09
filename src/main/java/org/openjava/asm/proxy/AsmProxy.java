package org.openjava.asm.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: brenthuang
 * @date: 2022/04/20
 */
public class AsmProxy {
    private Class<?> superClass;
    private InvocationHandler handler;
    private ProxyClassGenerator.ProxyBuilder builder;

    public AsmProxy() {
        builder = ProxyClassGenerator.builder();
    }

    public Object newInstance() {
        if (superClass == null) {
            throw new IllegalArgumentException("superClass needed");
        }

        Class<?> c = builder.argumentTypes(null).build();
        try {
            bindHandler(c, handler);
            Constructor<?> constructor = c.getConstructor(new Class[0]);
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException iex) {
            throw new DynamicProxyException(iex);
        } catch (InvocationTargetException tex) {
            throw new DynamicProxyException(tex.getTargetException());
        } finally {
            bindHandler(c, null);
        }
    }

    public Object newInstance(Class[] argumentTypes, Object[] arguments) {
        if (superClass == null) {
            throw new IllegalArgumentException("superClass needed");
        }

        Class<?> c = builder.argumentTypes(argumentTypes).build();
        try {
            bindHandler(c, handler);
            Constructor<?> constructor = c.getConstructor(argumentTypes);
            return constructor.newInstance(arguments);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException iex) {
            throw new DynamicProxyException(iex);
        } catch (InvocationTargetException tex) {
            throw new DynamicProxyException(tex.getTargetException());
        } finally {
            bindHandler(c, null);
        }
    }

    private void bindHandler(Class<?> c, InvocationHandler h) {
        try {
            Method method = c.getMethod("bindHandler", InvocationHandler.class);
            method.invoke(null, h);
        } catch (NoSuchMethodException nex) {
            throw new IllegalArgumentException(c + " is not an enhanced class");
        } catch (IllegalAccessException iex) {
            throw new DynamicProxyException(iex);
        } catch (InvocationTargetException ex) {
            throw new DynamicProxyException(ex.getTargetException());
        }
    }

    public void setSuperClass(Class<?> superClass) {
        builder.superClass(superClass);
        this.superClass = superClass;
    }

    public void setClassLoader(ClassLoader classLoader) {
        builder.classLoader(classLoader);
    }

    public void setHandler(InvocationHandler handler) {
        this.handler = handler;
    }

    public void setMethodFilter(MethodFilter methodFilter) {
        builder.methodFilter(methodFilter);
    }
}
