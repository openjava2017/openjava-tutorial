package org.openjava.asm.agent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PremainAgent {
    public static void premain(String args, Instrumentation inst) {
        //java -javaagent:/Users/brenthuang/Work/projects/openjava-asm/build/libs/openjava-asm-1.0-SNAPSHOT.jar=aa -jar upay-service-1.0.0.jar
        System.out.println(String.format("premain args: %s, retransformClassesSupported: %s, redefineClassesSupported: %s",
            args, inst.isRetransformClassesSupported(), inst.isRedefineClassesSupported()));
        inst.addTransformer(new ClassFileTransformer() {
            // premain是在main函数之前执行(类加载之前执行)，因此参数Class<?> c为null
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> c, ProtectionDomain domain, byte[] classBytes) throws IllegalClassFormatException {
                if ("com/dili/ss/component/JarCleaner".equals(className) || "com/dili/ss/dto/Supreme".equals(className)) {
                    System.out.println("start------>" + className);
                    try {
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
                    System.out.println("end------>" + className);

                }
                return classBytes;
            }
        }, true);
    }
}
