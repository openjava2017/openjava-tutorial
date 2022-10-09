package org.openjava.asm.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openjava.asm.core.TimeClassTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * @author: brenthuang
 * @date: 2022/03/29
 */
public class MainAgent {
    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println(String.format("agentmain args: %s, retransformClassesSupported: %s, redefineClassesSupported: %s",
                args, inst.isRetransformClassesSupported(), inst.isRedefineClassesSupported()));
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> c, ProtectionDomain domain, byte[] classBytes) {
                if ("org/apache/ibatis/session/defaults/DefaultSqlSession".equals(className)) {
                    Thread t = Thread.currentThread();
                    ClassLoader cl = t.getContextClassLoader();
                    t.setContextClassLoader(loader);
                    try {
                        ClassReader reader = new ClassReader(classBytes);
                        // 扩展ClassWriter，优先使用线程上下文类加载器，避免加载到项目类出现ClassNotFoundException
                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                            /**
                             * 首先使用线程上下文类加载器，否则使用本类的加载器
                             */
                            protected ClassLoader getClassLoader() {
                                ClassLoader cl = null;

                                try {
                                    cl = Thread.currentThread().getContextClassLoader();
                                } catch (Throwable ex) {
                                }

                                if (cl == null) {
                                    cl = getClass().getClassLoader();
                                    if (cl == null) {
                                        try {
                                            cl = ClassLoader.getSystemClassLoader();
                                        } catch (Throwable ex) {
                                        }
                                    }
                                }
                                return cl;
                            }
                        };
                        TimeClassTransformer cv = new TimeClassTransformer(Opcodes.ASM9, cw);
                        // 如果MethodVisitor存在LocalVariablesSorter则，需使用EXPAND_FRAMES参数
                        reader.accept(cv, ClassReader.EXPAND_FRAMES);
                        return cw.toByteArray();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        t.setContextClassLoader(cl);
                    }
                    return classBytes;
                }
                return classBytes;
            }
        }, true);
        // 如果是项目类文件，可能出现ClassNotFoundException异常，需使用特定的类加载器进行加载
        // 目前没有可行的方案获取到项目默认的类加载器，比如：Springboot项目的LaunchedURLClassLoader
        // 因此对于使用了自定义类加载器的项目，目前不适合使用agentmain模式，无法完成修改项目类的目的
        inst.retransformClasses(MainAgent.class.getClassLoader().loadClass("org.apache.ibatis.session.defaults.DefaultSqlSession"));
    }
}