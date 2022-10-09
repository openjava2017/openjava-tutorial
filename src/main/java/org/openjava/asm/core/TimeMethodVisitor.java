package org.openjava.asm.core;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * @author: brenthuang
 * @date: 2022/03/17
 */
public class TimeMethodVisitor extends LocalVariablesSorter implements Opcodes {
    private boolean allowForMethod;
    private int identifier;
    private String name;
    public TimeMethodVisitor(final int api, final MethodVisitor mv, final int access, final String name, final String desc) {
        super(api, access, desc, mv);
        this.name = name;
        allowForMethod = !"<init>".equals(name);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (allowForMethod) {
            onMethodEnter();
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        if (allowForMethod) {
            switch (opcode) {
                case RETURN:
                case IRETURN:
                case FRETURN:
                case ARETURN:
                case LRETURN:
                case DRETURN:
                case ATHROW:
                    onMethodExit(opcode);
                    break;
                default:
                    break;
            }
        }
        super.visitInsn(opcode);
    }

    protected void onMethodEnter() {
        super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        identifier = newLocal(Type.LONG_TYPE);
        super.visitVarInsn(LSTORE, identifier);
    }

    protected void onMethodExit(final int opcode) {
        super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        super.visitLdcInsn(name + " consumes %d milliseconds......");
        super.visitInsn(ICONST_1);
        super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        super.visitInsn(DUP);
        super.visitInsn(ICONST_0);

        super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        super.visitVarInsn(LLOAD, identifier);
        super.visitInsn(LSUB);
        super.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        super.visitInsn(AASTORE);
        super.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
}
