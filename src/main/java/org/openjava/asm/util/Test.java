package org.openjava.asm.util;

import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class Test {
    @SentinelMethod(threshold = 15000)
    public int div(int i, int j) {
        long _start = System.currentTimeMillis();
        try {
            return i/j;
        } catch (Exception ex) {
            System.out.println("Do something while exception");
            throw ex;
        } finally {
            System.out.println(String.format("it consumes %d milliseconds", System.currentTimeMillis() - _start));
        }
    }

    public void divEx(int i, int j) {
        long _start = System.currentTimeMillis();
        try {
            System.out.println(i/j);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            System.out.println(System.currentTimeMillis() - _start);
        }
    }

    public static void main(String[] args) throws Exception {
//        try {
//            Class<?> proxy = Class.forName("org.openjava.asm.service.HelloServiceProxy");
//            Method method = proxy.getMethod("add", int.class, int.class);
//            Object r = method.invoke(proxy.newInstance(), 10, 0);
//            System.out.println(r);
//        } catch (Exception ex) {
//
//        }
//        SignatureReader reader = new SignatureReader("<E:Ljava/lang/Object;>Lorg/openjava/asm/service/HelloService<TE;>;");
        SignatureReader reader = new SignatureReader("<E:Ljava/lang/Object;F:Ljava/lang/Object;>Lorg/openjava/asm/service/HelloService<TE;>;");
        SignatureWriter writer = new ClassSignatureVisitor();
        reader.accept(writer);
        System.out.println(writer);
    }

    private static class ClassSignatureVisitor extends SignatureWriter {
        @Override
        public void visitFormalTypeParameter(final String name) {
            System.out.println("visitFormalTypeParameter......" + name);
            super.visitFormalTypeParameter(name);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            System.out.println("visitClassBound......");
            return super.visitClassBound();
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            System.out.println("visitInterfaceBound......");
            return super.visitInterfaceBound();
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            System.out.println("visitSuperclass......");
            return new SignatureVisitor(api) {};
        }

        @Override
        public SignatureVisitor visitInterface() {
            System.out.println("visitInterface......");
            return super.visitInterface();
        }

        @Override
        public SignatureVisitor visitParameterType() {
            System.out.println("visitParameterType......");
            return super.visitParameterType();
        }

        @Override
        public SignatureVisitor visitReturnType() {
            System.out.println("visitReturnType......");
            return super.visitReturnType();
        }

        @Override
        public SignatureVisitor visitExceptionType() {
            System.out.println("visitExceptionType......");
            return super.visitExceptionType();
        }

        @Override
        public void visitBaseType(final char descriptor) {
            System.out.println("visitBaseType......");
            super.visitBaseType(descriptor);
        }

        @Override
        public void visitTypeVariable(final String name) {
            System.out.println("visitTypeVariable......" + name);
            super.visitTypeVariable(name);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            System.out.println("visitArrayType......");
            return super.visitArrayType();
        }

        @Override
        public void visitClassType(final String name) {
            System.out.println("visitClassType......" + name);
            super.visitClassType(name);
        }

        @Override
        public void visitInnerClassType(final String name) {
            System.out.println("visitInnerClassType......" + name);
            super.visitInnerClassType(name);
        }

        @Override
        public void visitTypeArgument() {
            System.out.println("visitTypeArgument......");
            super.visitTypeArgument();
        }

        @Override
        public SignatureVisitor visitTypeArgument(final char wildcard) {
            System.out.println("visitTypeArgument......" + wildcard);
            return super.visitTypeArgument(wildcard);
        }

        @Override
        public void visitEnd() {
            System.out.println("visitEnd......");
            super.visitEnd();
        }
    }
}
