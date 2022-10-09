package org.openjava.asm.proxy;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 代理类生成方案：继承被代理类，重写所有非private构造函数，重写非private且methodFilter过滤之后的方法
 *
 * public class HellService$Proxy extends HelloService {
 *
 *     private static final ThreadLocal threadHandler;
 *     private static final Method addSuper;
 *
 *     private InvocationHandler handler = null;
 *
 *     static {
 *         try {
 *             threadHandler = new ThreadLocal();
 *             addSuper = HellService$Proxy.class.getDeclaredMethod("addSuper", int.class, int.class);
 *         } catch (NoSuchMethodException mex) {
 *             throw new NoSuchMethodError(mex.getMessage());
 *         }
 *     }
 *
 *     public HellService$Proxy(String word) {
 *         super(word);
 *         this.handler = (InvocationHandler) threadHandler.get();
 *     }
 *
 *     public static final void bindHandler(InvocationHandler handler) {
 *         threadHandler.set(handler);
 *     }
 *
 *     public static HellService$Proxy newInstance(String word, InvocationHandler handler) {
 *         bindHandler(handler);
 *         try {
 *             return new HellService$Proxy(word);
 *         } finally {
 *             bindHandler(null);
 *         }
 *     }
 *
 *     public final int addSuper(int a, int b) {
 *         return super.add(a, b);
 *     }
 *
 *     @Override
 *     public final int add(int a, int b) {
 *         try {
 *             if (handler != null) {
 *                 return (int)handler.invoke(HellService$Proxy.this, addSuper, new Object[] {a, b});
 *             } else {
 *                 return super.add(a, b);
 *             }
 *         } catch (RuntimeException | Error rex) {
 *             throw rex;
 *         } catch (Throwable ex) {
 *             throw new UndeclaredThrowableException(ex);
 *         }
 *     }
 * }
 *
 * @author: brenthuang
 * @date: 2022/04/20
 */
class ProxyClassGenerator extends AsmClassGenerator {
    private static final AtomicLong nexUniqueNumber = new AtomicLong();

    private String name; // 被代理类的名称: java.lang.Object
    private String newName; // 代理类名称: java.lang.Object$Proxy
    private String internalName; // 被代理类的内部名称: java/lang/Object
    private String newInternalName; // 代理类内部名称: java/lang/Object$Proxy

    private ClassLoader classLoader;

    private Constructor<?>[] constructors = new Constructor<?>[0]; // 所有需

    private Method[] methods = new Method[0];

    private MethodFilter methodFilter = new MethodFilter() {
        @Override
        public boolean filter(Class c, Method method) {
            return method.getDeclaringClass() != Object.class;
        }
    };

    private ProxyClassGenerator() {
    }

    @Override
    protected void generateClassCode(ClassVisitor cv) {
        // 类信息
        cv.visit(V1_8, ACC_PUBLIC + ACC_SUPER, newInternalName, null, internalName, null);

        // 处理所有静态和非静态成员字段
        generateFieldMembers(cv);

        // 静态构造函数
        generateStaticConstructor(cv);

        // 构造函数
        generateConstructors(cv);

        // bindHandler方法
        generateBindHandler(cv);

        // newInstance方法
        generateNewInstance(cv);

        // 所有代理方法
        generateMethods(cv);

        cv.visitEnd();
    }

    @Override
    protected ClassLoader getClassLoader() {
        ClassLoader cl = this.classLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            cl = super.getClassLoader();
        }
        if (cl == null) { // 如果super.getClassLoader()返回bootstrap类加载器
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    @Override
    protected String getFullName() {
        return newName;
    }

    private void generateFieldMembers(ClassVisitor cv) {
        FieldVisitor fv;
        for (Method method : methods) {
            // 静态字段信息: 每个代理方法一个静态字段
            fv = cv.visitField(ACC_PRIVATE + ACC_STATIC + ACC_FINAL, method.getName() + "Super", "Ljava/lang/reflect/Method;", null, null);
            fv.visitEnd();
        }

        fv = cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "threadHandler", "Ljava/lang/ThreadLocal;", null, null);
        fv.visitEnd();

        // InvocationHandler字段信息
        fv = cv.visitField(ACC_PRIVATE, "handler", "Ljava/lang/reflect/InvocationHandler;", null, null);
        fv.visitEnd();
    }

    private void generateStaticConstructor(ClassVisitor cv) {
        // 静态构造函数
        MethodVisitor mv = cv.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/NoSuchMethodException");
        mv.visitLabel(l0);
        mv.visitTypeInsn(NEW, "java/lang/ThreadLocal"); // 初始化ThreadLocal变量
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, newInternalName, "threadHandler", "Ljava/lang/ThreadLocal;");

        for (Method method : methods) {
            // 初始化静态字段
            mv.visitLdcInsn(Type.getObjectType(newInternalName));
            mv.visitLdcInsn(method.getName() + "Super");
            Type[] argumentTypes = Type.getArgumentTypes(method);
            pushInt(mv, argumentTypes.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitInsn(DUP);
                pushInt(mv, i);
                if (isBoxedType(argumentTypes[i])) {
                    Type boxedType = getBoxedType(argumentTypes[i]);
                    mv.visitFieldInsn(GETSTATIC, boxedType.getInternalName(), "TYPE", "Ljava/lang/Class;");
                } else {
                    mv.visitLdcInsn(argumentTypes[i]);
                }
                mv.visitInsn(AASTORE);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
            mv.visitFieldInsn(PUTSTATIC, newInternalName, method.getName() + "Super", "Ljava/lang/reflect/Method;");
        }
        mv.visitLabel(l1);
        Label l3 = new Label();
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NoSuchMethodException", "getMessage", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
        mv.visitEnd();
    }

    private void generateConstructors(ClassVisitor cv) {
        // 构造函数
        for(Constructor<?> constructor : constructors) {
            Class<?>[] exceptionTypes = constructor.getExceptionTypes();
            String[] exceptions = null;
            if (exceptionTypes.length > 0) {
                exceptions = new String[exceptionTypes.length];
                for (int i = 0; i < exceptionTypes.length; i++) {
                    exceptions[i] = Type.getInternalName(exceptionTypes[i]);
                }
            }
            String constructorDesc = Type.getConstructorDescriptor(constructor);
            MethodVisitor mv = cv.visitMethod(constructor.getModifiers(), "<init>", constructorDesc, null, exceptions);
            mv.visitCode();
            Type[] argumentTypes = Type.getArgumentTypes(constructorDesc); // 调用父类构造函数
            mv.visitVarInsn(ALOAD, 0);
            loadArguments(mv, argumentTypes, 1);
            mv.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", constructorDesc, false);
            mv.visitVarInsn(ALOAD, 0); // 初始化成员变量handler
            mv.visitFieldInsn(GETSTATIC, newInternalName, "threadHandler", "Ljava/lang/ThreadLocal;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/reflect/InvocationHandler");
            mv.visitFieldInsn(PUTFIELD, newInternalName, "handler", "Ljava/lang/reflect/InvocationHandler;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
            mv.visitEnd();
        }
    }

    private void generateBindHandler(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, "bindHandler", "(Ljava/lang/reflect/InvocationHandler;)V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, newInternalName, "threadHandler", "Ljava/lang/ThreadLocal;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "set", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
        mv.visitEnd();
    }

    private void generateNewInstance(ClassVisitor cv) {
        for (Constructor<?> constructor : constructors) {
            // 方法变量表：构造函数的参数1 2..., handler, 代理类对象, 异常对象
            // newInstance会调用构造函数，因此构造函数抛出的异常，newInstance同样需要抛出
            String[] exceptions = wrapExceptionTypes(constructor.getExceptionTypes());
            String constructorDesc = Type.getConstructorDescriptor(constructor);
            Type[] constructorArgumentTypes = Type.getArgumentTypes(constructorDesc);

            Type returnType = Type.getObjectType(newInternalName);
            Type[] argumentTypes = new Type[constructorArgumentTypes.length + 1];
            int varIndex = 0;
            for (int i = 0; i < constructorArgumentTypes.length; i++) {
                argumentTypes[i] = constructorArgumentTypes[i];
                varIndex += argumentTypes[i].getSize();
            }
            argumentTypes[constructorArgumentTypes.length] = Type.getType("Ljava/lang/reflect/InvocationHandler;");
            int handlerTypeSize = varIndex + argumentTypes[constructorArgumentTypes.length].getSize();

            // 此时varIndex为handler在变量表的存储位置, varIndex + handlerTypeSize为生成的代理类对象在变量表的存储位置
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "newInstance", Type.getMethodDescriptor(returnType, argumentTypes), null, exceptions);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, null);
            mv.visitVarInsn(ALOAD, varIndex); // 加载handler变量
            mv.visitMethodInsn(INVOKESTATIC, newInternalName, "bindHandler", "(Ljava/lang/reflect/InvocationHandler;)V", false);
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, newInternalName); // 创建代理类对象
            mv.visitInsn(DUP);
            loadArguments(mv, constructorArgumentTypes, 0); // 加载构造函数所有参数
            mv.visitMethodInsn(INVOKESPECIAL, newInternalName, "<init>", constructorDesc, false);
            // 存储代理类对象至本地变量表最后一个index位置, 最后一个参数(handler)后一个位置
            mv.visitVarInsn(ASTORE, varIndex + handlerTypeSize);
            mv.visitLabel(l1);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESTATIC, newInternalName, "bindHandler", "(Ljava/lang/reflect/InvocationHandler;)V", false);
            // 加载代理类对象至操作数栈
            mv.visitVarInsn(ALOAD, varIndex + handlerTypeSize);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l2);
            mv.visitVarInsn(ASTORE, varIndex + handlerTypeSize + returnType.getSize()); // 存储异常类对象
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESTATIC, newInternalName, "bindHandler", "(Ljava/lang/reflect/InvocationHandler;)V", false);
            mv.visitVarInsn(ALOAD, varIndex + handlerTypeSize + returnType.getSize());
            mv.visitInsn(ATHROW);
            mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
            mv.visitEnd();
        }
    }

    private void generateMethods(ClassVisitor cv) {
        for (Method method : methods) {
            // 生成原生方法：原生方法后缀加Super关键字，并且只调用超类方法 super.methodName()，无其他逻辑
            String[] exceptions = wrapExceptionTypes(method.getExceptionTypes());
            String methodDesc = Type.getMethodDescriptor(method);
            MethodVisitor mv = cv.visitMethod(method.getModifiers() + ACC_FINAL, method.getName() + "Super", methodDesc, null, exceptions);
            mv.visitCode();
            Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
            Type returnType = Type.getReturnType(method);
            mv.visitVarInsn(ALOAD, 0);
            loadArguments(mv, argumentTypes, 1);
            mv.visitMethodInsn(INVOKESPECIAL, internalName, method.getName(), methodDesc, false);
            mv.visitInsn(returnType.getOpcode(IRETURN));
            mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
            mv.visitEnd();

            // 生成代理方法：同方法名
            mv = cv.visitMethod(method.getModifiers() + ACC_FINAL, method.getName(), methodDesc, null, exceptions);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/RuntimeException");
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Error");
            Label l3 = new Label();
            Label l4 = new Label();
            mv.visitTryCatchBlock(l3, l4, l2, "java/lang/RuntimeException");
            mv.visitTryCatchBlock(l3, l4, l2, "java/lang/Error");
            Label l5 = new Label();
            mv.visitTryCatchBlock(l0, l1, l5, "java/lang/Throwable");
            mv.visitTryCatchBlock(l3, l4, l5, "java/lang/Throwable");
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, newInternalName, "handler", "Ljava/lang/reflect/InvocationHandler;");
            mv.visitJumpInsn(IFNULL, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, newInternalName, "handler", "Ljava/lang/reflect/InvocationHandler;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETSTATIC, newInternalName, method.getName() + "Super", "Ljava/lang/reflect/Method;");
            pushInt(mv, argumentTypes.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            int varIndex = 1;
            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitInsn(DUP);
                pushInt(mv, i); // 数组索引, 从0开始
                mv.visitVarInsn(argumentTypes[i].getOpcode(ILOAD), varIndex); // 加载变量表中的第2个参数(index=1)，第一个参数是this(index=0)
                varIndex += argumentTypes[i].getSize();
                // Object和数组不需要进行box
                if (argumentTypes[i].getSort() != Type.OBJECT && argumentTypes[i].getSort() != Type.ARRAY) {
                    if (argumentTypes[i] == Type.VOID_TYPE) {
                        mv.visitInsn(Opcodes.ACONST_NULL); // Void 则赋值NULL
                    } else {
                        Type boxedType = getBoxedType(argumentTypes[i]);
                        String descriptor = Type.getMethodType(boxedType, new Type[] {argumentTypes[i]}).getDescriptor();
                        mv.visitMethodInsn(INVOKESTATIC, boxedType.getInternalName(), "valueOf", descriptor, false);
                    }
                }
                mv.visitInsn(AASTORE); // 为数组i的位置赋值
            }
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/reflect/InvocationHandler", "invoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);
            castAndUnbox(mv, returnType);
            mv.visitLabel(l1);
            mv.visitInsn(returnType.getOpcode(IRETURN));
            mv.visitLabel(l3);
            mv.visitVarInsn(ALOAD, 0);
            loadArguments(mv, argumentTypes, 1);
            mv.visitMethodInsn(INVOKESPECIAL, internalName, method.getName(), methodDesc, false);
            mv.visitLabel(l4);
            mv.visitInsn(returnType.getOpcode(IRETURN));
            // 处理异常 RuntimeException | Error rex
            mv.visitLabel(l2);
            mv.visitVarInsn(ASTORE, varIndex); // 异常保存在变量表中最后一个位置
            mv.visitVarInsn(ALOAD, varIndex);
            mv.visitInsn(ATHROW);
            // 处理异常 Throwable ex
            mv.visitLabel(l5);
            mv.visitVarInsn(ASTORE, varIndex);
            // Throwable传换成UndeclaredThrowableException
            mv.visitTypeInsn(NEW, "java/lang/reflect/UndeclaredThrowableException");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, varIndex);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>", "(Ljava/lang/Throwable;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitMaxs(0, 0); // ClassWriter.COMPUTE_FRAMES会忽略visitMax中的参数，但必须人工触发调用
            mv.visitEnd();
        }
    }

    private String[] wrapExceptionTypes(Class<?>[] exceptionTypes) {
        String[] exceptions = null;
        if (exceptionTypes.length > 0) {
            exceptions = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++) {
                exceptions[i] = Type.getInternalName(exceptionTypes[i]);
            }
        }
        return exceptions;
    }

    public static ProxyBuilder builder() {
        return new ProxyClassGenerator().new ProxyBuilder();
    }

    public class ProxyBuilder {
        private Class<?> superClass;
        private WeakReference<Class<?>> cachedProxyClass; // 缓存生成的代理类，严谨的缓存机制参见JDK的Proxy

        public synchronized ProxyBuilder superClass(Class<?> superClass) {
            if (cachedProxyClass != null) { // 一旦生成过代理类，则不允许修改
                throw new UnsupportedOperationException("Cannot change super class");
            }
            if (superClass.isInterface()) {
                throw new DynamicProxyException("Cannot proxy the interface");
            }
            if (Modifier.isFinal(superClass.getModifiers())) { // 不存在private的class
                throw new DynamicProxyException("Cannot subclass final class");
            }
            this.superClass = superClass;
            // name: java.lang.Object  internalName: java/lang/Object
            long uniqueNumber = nexUniqueNumber.incrementAndGet();
            ProxyClassGenerator.this.name = superClass.getName();
            ProxyClassGenerator.this.newName = ProxyClassGenerator.this.name + "$Proxy" + uniqueNumber;
            ProxyClassGenerator.this.internalName = Type.getInternalName(superClass);
            ProxyClassGenerator.this.newInternalName = ProxyClassGenerator.this.internalName + "$Proxy" + uniqueNumber;
            return this;
        }

        public synchronized ProxyBuilder classLoader(ClassLoader classLoader) {
            if (cachedProxyClass != null) { // 一旦生成过代理类，则不允许修改
                throw new UnsupportedOperationException("Cannot change class loader");
            }
            ProxyClassGenerator.this.classLoader = classLoader;
            return this;
        }

        public synchronized ProxyBuilder methodFilter(MethodFilter methodFilter) {
            if (cachedProxyClass != null) { // 一旦生成过代理类，则不允许修改
                throw new UnsupportedOperationException("Cannot change method filter");
            }
            ProxyClassGenerator.this.methodFilter = methodFilter;
            return this;
        }

        public synchronized ProxyBuilder argumentTypes(Class<?>[] argumentTypes) {
            Constructor<?> constructor;
            try {
                // 确保对应的构造函数存在
                Class<?>[] types = argumentTypes == null ? new Class<?>[0] : argumentTypes;
                constructor = superClass.getConstructor(types);
            } catch (NoSuchMethodException nme) {
                throw new DynamicProxyException("no appropriate constructor found");
            }
            if (Modifier.isPrivate(constructor.getModifiers())) {
                throw new DynamicProxyException("private constructor not allowed");
            }
            return this;
        }

        public synchronized Class<?> build() {
            Class<?> proxyClass;
            if (cachedProxyClass != null && (proxyClass = cachedProxyClass.get()) != null) {
                return proxyClass;
            }
            // 构造函数getDeclaredConstructors() == getConstructors(), 父类的构造函数将忽略; 子类构造函数一定会调用父类的构造函数
            Constructor<?>[] constructors = superClass.getDeclaredConstructors();
            List<Constructor<?>> declaredConstructors = new ArrayList<>();
            for (Constructor<?> constructor : constructors) {
                if (Modifier.isPrivate(constructor.getModifiers())) {
                    continue;
                }
                declaredConstructors.add(constructor);
            }
            ProxyClassGenerator.this.constructors = declaredConstructors.toArray(new Constructor<?>[0]);

            Method[] methods = superClass.getDeclaredMethods();
            List<Method> declaredMethods = new ArrayList<>();
            for (Method method : methods) {
                if (Modifier.isFinal(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (methodFilter == null || methodFilter.filter(superClass, method)) {
                    declaredMethods.add(method);
                }
            }
            ProxyClassGenerator.this.methods = declaredMethods.toArray(new Method[0]);

            try {
                proxyClass = ProxyClassGenerator.this.generateClass();
                cachedProxyClass = new WeakReference<>(proxyClass);
                return proxyClass;
            } catch (Exception ex) {
                throw new DynamicProxyException("dynamic proxy failed", ex);
            }
        }
    }
}
