package org.openjava.asm.core;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;

public class TimeClassTransformer extends ClassVisitor implements Opcodes {
    private boolean allowForClass;
    public TimeClassTransformer(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        allowForClass = allowForClass(access, name, signature);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (allowForClass && mv != null && !"<init>".equals(name) && Modifier.isPublic(access)) {
            // TimeMethodVisitor不支改写持构造函数, TimeMethodVisitor1支持改写构造函数
//            mv = new TimeMethodVisitor1(Opcodes.ASM9, mv, access, name, desc);
            mv = new TimeMethodVisitor(Opcodes.ASM9, mv, access, name, desc);
        }
        return mv;
    }

    protected boolean allowForClass(final int access, final String name, final String signature) {
        // 不是接口和枚举类才允许改变字节码
        return !Modifier.isInterface(access) && (access & ACC_ENUM) == 0;
    }

    public static void main(String[] args) throws Exception {
        InputStream is = new FileInputStream("/Users/brenthuang/Work/projects/openjava-asm/build/classes/java/test/org/openjava/asm/service/HelloService.class");
        ClassReader reader = new ClassReader(is);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        TimeClassTransformer cv = new TimeClassTransformer(Opcodes.ASM9, cw);
        // 如果MethodVisitor存在LocalVariablesSorter则，需使用EXPAND_FRAMES参数
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        byte[] packet = cw.toByteArray();
        OutputStream os = new FileOutputStream("/Users/brenthuang/Desktop/org/openjava/asm/service/HelloService.class");
        os.write(packet);
        os.flush();
        os.close();
    }
}
