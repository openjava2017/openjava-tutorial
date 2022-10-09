package org.openjava.asm.util;

public class ProxyMethod {
    private boolean isConstructor;
    private int access;
    private String name;
    private String desc;
    private String signature;

    private ProxyMethod() {
    }

    public static ProxyMethod of(int access, String name, String desc, String signature) {
        ProxyMethod proxyMethod = new ProxyMethod();
        proxyMethod.isConstructor = "<init>".equals(name);
        proxyMethod.access = access;
        proxyMethod.name = name;
        proxyMethod.desc = desc;
        proxyMethod.signature = signature;
        return proxyMethod;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getSignature() {
        return signature;
    }
}
