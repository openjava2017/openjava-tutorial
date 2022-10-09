package org.openjava.asm.core;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InitSentinelMethodVisitor extends MethodVisitor implements Opcodes {

    private MethodVisitor mv;
    private String name; //方法名
    private String desc; //方法类型描述
    private String superName; //超类名称

    public InitSentinelMethodVisitor(final int api, final MethodVisitor mv, final int access, final String name,
                                     final String desc, final String superName) {
        // 重写构造方法，MethodVisitor传入null，否则就是改写构造方法
        super(api, null);
        this.mv = mv;
        this.name = name;
        this.desc = desc;
        this.superName = superName;
    }

    @Override
    public void visitCode() {
        Type[] arguments = Type.getArgumentTypes(desc);
        mv.visitVarInsn(ALOAD, 0);
        for (int i = 0; i < arguments.length; i++) {
            mv.visitVarInsn(arguments[i].getOpcode(ILOAD), i+1);
        }
        mv.visitMethodInsn(INVOKESPECIAL, superName, name, desc, false);
        mv.visitInsn(RETURN);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        // 使用ClassWriter.COMPUTE_FRAMES参数则自动计算(不会使用父类的maxStack maxLocals)，人工触发调用
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        mv.visitEnd();
    }
}
