package org.openjava.asm.core;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.openjava.asm.util.ProxyClass;
import org.openjava.asm.util.ProxyMethod;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class SentinelClassTransformer extends ClassVisitor implements Opcodes {

    private ProxyClass proxyClass;

    private ClassVisitor classVisitor;

    public SentinelClassTransformer(int api, ClassVisitor cv) {
        super(api, null);
        this.classVisitor = cv;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        // 抽象类、接口、final类和枚举类不允许生成代理类
        if (allowForClass(access, name, signature)) {
            String newSignature = signature;
            if (newSignature != null) {
                // 转换类的泛型声明, 生成HelloServiceProxy<E> extends HelloService, 其中超类为HelloService<E>
                // <E:Ljava/lang/Object;>Ljava/lang/Object<TE;>;  ===转换成===>
                // <E:Ljava/lang/Object;>Lorg/openjava/asm/service/HelloService;
                SignatureReader sr = new SignatureReader(signature);
                SignatureWriter sw = new SignatureWriter() {
                    private String superName = name;
                    private List<String> typeParams = new ArrayList<>();
                    @Override
                    public void visitFormalTypeParameter(final String name) {
                        typeParams.add(name);
                        super.visitFormalTypeParameter(name);
                    }

                    @Override
                    public SignatureVisitor visitSuperclass() {
                        super.visitSuperclass();
                        super.visitClassType(superName);
                        super.visitEnd();
                        return new SignatureVisitor(api) {};
                    }
                };
                sr.accept(sw);
                newSignature = sw.toString();
            }
            // 代理类不需要实现superClass的接口, interfaces传null
            proxyClass = ProxyClass.of(version, access, name + "Proxy", newSignature, name);
        } else {
            throw new UnsupportedOperationException("Cannot generate proxy class");
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        // 静态块(static方法)，非public/protect方法、static方法、abstract方法和final方法不允许生成代理
        boolean allowForMethod = (Modifier.isPublic(access) || Modifier.isProtected(access))
            && !Modifier.isStatic(access) && !Modifier.isAbstract(access) && !Modifier.isFinal(access);
        if (!allowForMethod) {
            return null;
        }

        if ("<init>".equals(name)) {
            proxyClass.addMethod(ProxyMethod.of(access, name, desc, signature));
            return null;
        }

        return new MethodVisitor(api, null) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if ("Lorg/openjava/asm/util/SentinelMethod;".equals(descriptor)) {
                    proxyClass.addMethod(ProxyMethod.of(access, name, desc, signature));
                }
                return null; // TODO: 获取注解属性
            }
        };
    }

    @Override
    public void visitEnd() {
        proxyClass.dump(classVisitor);
    }

    protected boolean allowForClass(final int access, final String name, final String signature) {
        return !Modifier.isInterface(access) && !Modifier.isAbstract(access) && !Modifier.isFinal(access) && (access & ACC_ENUM) == 0;
    }

    public static void main(String[] args) throws Exception {
        InputStream is = new FileInputStream("/Users/brenthuang/Work/projects/openjava-asm/build/classes/java/test/org/openjava/asm/service/HelloService.class");
        ClassReader reader = new ClassReader(is);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        SentinelClassTransformer cv = new SentinelClassTransformer(Opcodes.ASM9, cw);
        // 如果MethodVisitor存在LocalVariablesSorter则，需使用ClassReader.EXPAND_FRAMES参数
        reader.accept(cv, 0);
        byte[] packet = cw.toByteArray();
        OutputStream os = new FileOutputStream("/Users/brenthuang/Desktop/org/openjava/asm/service/HelloServiceProxy.class");
        os.write(packet);
        os.flush();
        os.close();
    }
}
