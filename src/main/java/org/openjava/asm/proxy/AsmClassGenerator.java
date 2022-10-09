package org.openjava.asm.proxy;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @author: brenthuang
 * @date: 2022/04/20
 */
abstract class AsmClassGenerator implements Opcodes {
    public static final String DEBUG_LOCATION_PROPERTY = "asm.debug.proxyClass.location";

    private static String debugProxyClassLocation = System.getProperty(DEBUG_LOCATION_PROPERTY);

    private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

    private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

    private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

    private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

    private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

    private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

    private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

    private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

    private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

    private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

    private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

    private static final Method INT_VALUE = Method.getMethod("int intValue()");

    private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

    private static final Method LONG_VALUE = Method.getMethod("long longValue()");

    private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

    protected Class generateClass() throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        generateClassCode(cw);
        byte[] classBytes = cw.toByteArray();

        if (debugProxyClassLocation != null) {
            String dirs = getFullName().replace('.', File.separatorChar);
            try {
                new File(debugProxyClassLocation + File.separatorChar + dirs).getParentFile().mkdirs();

                File file = new File(new File(debugProxyClassLocation), dirs + ".class");
                OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                try {
                    out.write(classBytes);
                } finally {
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ReflectUtils.defineClass(getFullName(), classBytes, getClassLoader());
    }

    protected abstract void generateClassCode(ClassVisitor cv);

    protected ClassLoader getClassLoader() {
        return AsmClassGenerator.class.getClassLoader();
    }

    protected abstract String getFullName();

    protected int loadArguments(final MethodVisitor mv, final Type[] argumentTypes, final int startIndex) {
        int varIndex = startIndex;
        for (int i = 0; i < argumentTypes.length; i++) {
            mv.visitVarInsn(argumentTypes[i].getOpcode(ILOAD), varIndex);
            varIndex += argumentTypes[i].getSize();
        }
        return varIndex;
    }

    protected void pushInt(final MethodVisitor mv, final int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    protected Type getBoxedType(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
                return BYTE_TYPE;
            case Type.BOOLEAN:
                return BOOLEAN_TYPE;
            case Type.SHORT:
                return SHORT_TYPE;
            case Type.CHAR:
                return CHARACTER_TYPE;
            case Type.INT:
                return INTEGER_TYPE;
            case Type.FLOAT:
                return FLOAT_TYPE;
            case Type.LONG:
                return LONG_TYPE;
            case Type.DOUBLE:
                return DOUBLE_TYPE;
            default:
                return type;
        }
    }

    protected boolean isBoxedType(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
            case Type.BOOLEAN:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * 将栈顶元素cast并在必要的时候进行unbox
     */
    protected void castAndUnbox(MethodVisitor mv, Type type) {
        Type boxedType = NUMBER_TYPE;
        Method unboxMethod = null;
        switch (type.getSort()) {
            case Type.VOID:
                return;
            case Type.CHAR:
                boxedType = CHARACTER_TYPE;
                unboxMethod = CHAR_VALUE;
                break;
            case Type.BOOLEAN:
                boxedType = BOOLEAN_TYPE;
                unboxMethod = BOOLEAN_VALUE;
                break;
            case Type.DOUBLE:
                unboxMethod = DOUBLE_VALUE;
                break;
            case Type.FLOAT:
                unboxMethod = FLOAT_VALUE;
                break;
            case Type.LONG:
                unboxMethod = LONG_VALUE;
                break;
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
                unboxMethod = INT_VALUE;
                break;
            default:
                unboxMethod = null;
                break;
        }
        if (unboxMethod == null) {
            if (!type.equals(OBJECT_TYPE)) {
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            }
        } else {
            mv.visitTypeInsn(CHECKCAST, boxedType.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL, boxedType.getInternalName(), unboxMethod.getName(), unboxMethod.getDescriptor(), false);
        }
    }
}
