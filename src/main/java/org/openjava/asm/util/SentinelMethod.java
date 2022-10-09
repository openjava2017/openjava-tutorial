package org.openjava.asm.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法哨兵注解，标注的方法将监控方法执行时间，当执行时间超过threshold设定的阀值时预警
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SentinelMethod {
    /**
     * 方法执行时间阀值, 默认值10秒
     */
    long threshold() default 10000;
}
