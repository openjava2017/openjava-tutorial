package org.openjava.asm.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openjava.asm.core.TimeClassTransformer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PremainAgent1 {
    public static void premain(String args, Instrumentation inst) {
        //java -javaagent:/Users/brenthuang/Work/projects/openjava-asm/build/libs/openjava-asm-1.0-SNAPSHOT.jar=aa -jar upay-service-1.0.0.jar
        System.out.println(String.format("premain args: %s, retransformClassesSupported: %s, redefineClassesSupported: %s",
            args, inst.isRetransformClassesSupported(), inst.isRedefineClassesSupported()));
        inst.addTransformer(new ClassFileTransformer() {
            // premain是在main函数之前执行(类加载之前执行)，因此参数Class<?> c为null
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> c, ProtectionDomain domain, byte[] classBytes) throws IllegalClassFormatException {
                if ("com/dili/ss/component/JarCleaner".equals(className) || "com/dili/ss/dto/Supreme".equals(className)) {
                    try {
                        System.out.println("------>" + className);
                        String dirs = args.replace('\\', File.separatorChar);
                        new File(dirs + File.separatorChar + className).getParentFile().mkdirs();

                        File file = new File(new File(dirs), className + ".class");
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            out.write(classBytes);
                        } finally {
                            out.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if ("com/diligrp/xtrade/upay/boss/controller/PaymentPlatformController".equals(className)) {
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
                }
                return classBytes;
            }
        }, true);
    }
}
