package org.openjava.asm.proxy;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * @author: brenthuang
 * @date: 2022/04/21
 */
public class ReflectUtils {
    private static Method DEFINE_CLASS;

    static {
        try {
            Class loader = Class.forName("java.lang.ClassLoader"); // JVM crash w/o this
            DEFINE_CLASS = loader.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class});
            DEFINE_CLASS.setAccessible(true);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("Proxy not supported by jvm");
        }
    }

    public static Class defineClass(String className, byte[] bytes, ClassLoader loader) throws Exception {
        Object[] args = new Object[]{className, bytes, Integer.valueOf(0), Integer.valueOf(bytes.length), null};
        //TODO: WARNING: An illegal reflective access operation has occurred
        Class c = (Class)DEFINE_CLASS.invoke(loader, args);
        // Force static initializers to run.
        Class.forName(className, true, loader);
        return c;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Type.getInternalName(ReflectUtils.class));
        System.out.println(ReflectUtils.class.getName());

        String descriptor = Type.getMethodType(Type.getObjectType("java/lang/Integer"), new Type[] {Type.INT_TYPE}).getDescriptor();
        System.out.println(descriptor);

    }
}
