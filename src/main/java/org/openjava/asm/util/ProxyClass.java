package org.openjava.asm.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

public class ProxyClass implements Opcodes {
    private int version;
    private int access;
    private String name;
    private String signature;
    private String superName;
    private List<ProxyMethod> methods = new ArrayList<>();

    private ProxyClass() {
    }

    public static ProxyClass of(int version, int access, String name, String signature, String superName) {
        ProxyClass proxyClass = new ProxyClass();
        proxyClass.version = version;
        proxyClass.access = access;
        proxyClass.name = name;
        proxyClass.signature = signature;
        proxyClass.superName = superName;
        return proxyClass;
    }

    public void dump(ClassVisitor cv) {
        boolean hasConstructor =  methods.stream().anyMatch(m -> m.isConstructor());
        if (!hasConstructor) {
            throw new UnsupportedOperationException("Cannot generate proxy class: no public or protected constructor");
        }

//        <E:Ljava/lang/Object;>Ljava/lang/Object;
//        <E:Ljava/lang/Object;>Lorg/openjava/asm/service/HelloService<TE;>;
        cv.visit(version, access, name, signature, superName, null);
        for (ProxyMethod method : methods) {
            // 忽略异常
            MethodVisitor mv = cv.visitMethod(method.getAccess(), method.getName(), method.getDesc(), method.getSignature(), null);
            mv.visitCode();
            if (method.isConstructor()) {
                Type[] arguments = Type.getArgumentTypes(method.getDesc());
                int varIndex = 0; // 下一个本地变量的slot index
                mv.visitVarInsn(ALOAD, varIndex ++);
                for (int i = 0; i < arguments.length; i++) {
                    mv.visitVarInsn(arguments[i].getOpcode(ILOAD), varIndex);
                    varIndex += arguments[i].getSize();
                }
                mv.visitMethodInsn(INVOKESPECIAL, superName, method.getName(), method.getDesc(), false);
                mv.visitInsn(RETURN);
            } else {
                LocalVariablesSorter lvs = new LocalVariablesSorter(access, method.getDesc(), mv);
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
                Type[] arguments = Type.getArgumentTypes(method.getDesc());
                lvs.visitVarInsn(ALOAD, varIndex ++); // 只适用成员方法, index=0的本地变量为this
                for (int i = 0; i < arguments.length; i++) {
                    lvs.visitVarInsn(arguments[i].getOpcode(ILOAD), varIndex);
                    varIndex += arguments[i].getSize();
                }
                lvs.visitMethodInsn(INVOKESPECIAL, superName, method.getName(), method.getDesc(), false);
                Type returnType = Type.getReturnType(method.getDesc());
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
            mv.visitEnd();
        }
        cv.visitEnd();
    }

    public int getVersion() {
        return version;
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public String getSuperName() {
        return superName;
    }

    public void addMethod(ProxyMethod method) {
        methods.add(method);
    }

    public List<ProxyMethod> getMethods() {
        return methods;
    }
}
