package org.openjava.asm.proxy;

import java.lang.reflect.Method;

/**
 * @author: brenthuang
 * @date: 2022/04/22
 */
public interface MethodFilter {
    default boolean filter(Class c, Method method) {
        return method.getDeclaringClass() != Object.class;
    }
}
