package org.openjava.asm.proxy;

/**
 * @author: brenthuang
 * @date: 2022/04/22
 */
public class DynamicProxyException extends RuntimeException {
    public DynamicProxyException(String message) {
        super(message);
    }

    public DynamicProxyException(Throwable cause) {
        super(cause);
    }

    public DynamicProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
