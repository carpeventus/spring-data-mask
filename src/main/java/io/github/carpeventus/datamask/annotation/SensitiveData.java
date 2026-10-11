package io.github.carpeventus.datamask.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 String 字段上，声明该字段的脱敏类型与是否附加密文字段。
 *
 * <p>字段位于任意深度的嵌套对象或集合中时同样生效；值为 null 或空串时原样输出。
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /**
     * 脱敏类型。
     */
    SensitiveDataType type();

    /**
     * 是否在明文字段同层级生成名为 {@code <字段名>Cipher} 的密文字段，默认关闭。
     */
    boolean addCipherField() default false;
}
