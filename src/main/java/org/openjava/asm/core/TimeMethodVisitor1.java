package org.openjava.asm.core;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class TimeMethodVisitor1 extends AdviceAdapter {
    private int identifier;
    private String name;
    protected TimeMethodVisitor1(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
        this.name = name;
    }

    protected void onMethodEnter() {
        visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        identifier = newLocal(Type.LONG_TYPE);
        visitVarInsn(LSTORE, identifier);
    }

    protected void onMethodExit(int opcode) {
        visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        visitLdcInsn(name + " consumes %d milliseconds......");
        visitInsn(ICONST_1);
        visitTypeInsn(ANEWARRAY, "java/lang/Object");
        visitInsn(DUP);
        visitInsn(ICONST_0);

        visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        visitVarInsn(LLOAD, identifier);
        visitInsn(LSUB);
        visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        visitInsn(AASTORE);
        visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
        visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

//    public void visitMaxs(int maxStack, int maxLocals) {
//        //使用ClassWriter.COMPUTE_FRAMES选项可以不用调用visitMaxs方法
//        System.out.println(String.format("maxStack:%d, maxLocals:%d", maxStack, maxLocals));
//        visitMaxs(maxStack + 4, maxLocals);
//    }
}
