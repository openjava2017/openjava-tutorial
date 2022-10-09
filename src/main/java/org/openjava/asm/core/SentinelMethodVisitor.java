package org.openjava.asm.core;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * 生成代理类方法，代理类继承自被代理类
 *  public int add(int i, int j) {
 *      long _start = System.currentTimeMillis();
 *      try {
 *          return super.add(i, j);
 *      } catch (Exception ex) {
 *          System.out.println("Do something while exception");
 *          throw ex;
 *      } finally {
 *          System.out.println(System.currentTimeMillis() - _start);
 *      }
 *  }
 *
 *  public void add(int i, int j) {
 *      long _start = System.currentTimeMillis();
 *      try {
 *          super.add(i, j);
 *      } catch (Exception ex) {
 *          System.out.println("Do something while exception");
 *          throw ex;
 *      } finally {
 *          System.out.println(System.currentTimeMillis() - _start);
 *      }
 *  }
 */
public class SentinelMethodVisitor extends MethodVisitor implements Opcodes {
    private boolean allowForMethod; // 是否允许方法被代理
    private LocalVariablesSorter lvs;
    private String name; //方法名
    private String desc; //方法类型描述
    private String superName; //超类名称

    public SentinelMethodVisitor(final int api, final MethodVisitor mv, final int access, final String name,
                                 final String desc, final String superName) {
        // 重写成员方法，MethodVisitor传入null，否则就是改写构造方法
        super(api, null);
        // 使用LocalVariablesSorter简化变量slot index计算
        this.lvs = new LocalVariablesSorter(access, desc, mv);
        this.name = name;
        this.desc = desc;
        this.superName = superName;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if ("Lorg/openjava/service/adapter/SentinelMethod;".equals(descriptor)) {
            allowForMethod = true;
        }
        return visitAnnotation(descriptor, visible); // 代理类将忽略annotation
    }

    @Override
    public void visitCode() {
//        int varIndex = 0; // 下一个本地变量的slot index
//        Type[] arguments = Type.getArgumentTypes(desc);
//        lvs.visitVarInsn(ALOAD, varIndex ++); // 只适用成员方法, index=0的本地变量为this
//
//        for (int i = 0; i < arguments.length; i++) {
//            lvs.visitVarInsn(arguments[i].getOpcode(ILOAD), varIndex);
//            varIndex += arguments[i].getSize();
//        }
//        lvs.visitMethodInsn(INVOKESPECIAL, superName, name, desc, false);
//
//        Type returnType = Type.getReturnType(desc);
//        lvs.visitInsn(returnType.getOpcode(IRETURN));
        Label L0 = new Label();
        Label L1 = new Label();
        Label L2 = new Label();
        Label L3 = new Label();
        Label L4 = new Label();

        lvs.visitTryCatchBlock(L0, L1, L2, "java/lang/Exception");
        lvs.visitTryCatchBlock(L0, L1, L3, null);
        lvs.visitTryCatchBlock(L2, L4, L3, null); // 一直没搞懂为啥 L2-L4之间的异常(为啥不是L2-L3呢，换成L3也能正常生成代码)，Handler是L3

        int _start = lvs.newLocal(Type.LONG_TYPE);
        lvs.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        lvs.visitVarInsn(LSTORE, _start);
        lvs.visitLabel(L0);

        int varIndex = 0; // 下一个本地变量的slot index
        Type[] arguments = Type.getArgumentTypes(desc);
        lvs.visitVarInsn(ALOAD, varIndex ++); // 只适用成员方法, index=0的本地变量为this
        for (int i = 0; i < arguments.length; i++) {
            lvs.visitVarInsn(arguments[i].getOpcode(ILOAD), varIndex);
            varIndex += arguments[i].getSize();
        }
        lvs.visitMethodInsn(INVOKESPECIAL, superName, name, desc, false);
        Type returnType = Type.getReturnType(desc);
        int _returnValue = -1;
        if (!returnType.equals(Type.VOID_TYPE)) {
            _returnValue = lvs.newLocal(returnType);
            lvs.visitVarInsn(returnType.getOpcode(ISTORE), _returnValue);
        }

        lvs.visitLabel(L1);
        lvs.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        lvs.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        lvs.visitVarInsn(LLOAD, _start);
        lvs.visitInsn(LSUB);
        lvs.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);
        if (_returnValue != -1) {
            lvs.visitVarInsn(returnType.getOpcode(ILOAD), _returnValue);
        }
        lvs.visitInsn(returnType.getOpcode(IRETURN));

        lvs.visitLabel(L2);
        int _exception = lvs.newLocal(Type.getType(Exception.class));
        lvs.visitVarInsn(ASTORE, _exception);
        lvs.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        lvs.visitLdcInsn("Do something while exception");
        lvs.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        lvs.visitVarInsn(ALOAD, _exception);
        lvs.visitInsn(ATHROW);

        lvs.visitLabel(L3);
        int _throwable = lvs.newLocal(Type.getType(Throwable.class));
        lvs.visitVarInsn(ASTORE, _throwable);

        lvs.visitLabel(L4);
        lvs.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        lvs.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        lvs.visitVarInsn(LLOAD, _start);
        lvs.visitInsn(LSUB);
        lvs.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);
        lvs.visitVarInsn(ALOAD, _throwable);
        lvs.visitInsn(ATHROW);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        // 使用ClassWriter.COMPUTE_FRAMES参数则自动计算(不会使用父类的maxStack maxLocals)，人工触发调用
        lvs.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        lvs.visitEnd();
    }
}
