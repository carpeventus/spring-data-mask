package io.github.carpeventus.datamask.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 HTTP 接口方法上，开启该接口响应的脱敏处理。
 *
 * <p>命中该注解后，响应体中所有标注 {@link SensitiveData} 的 String 字段输出脱敏值；
 * 未标注该注解的接口输出不受影响，脱敏只作用于响应，不影响请求。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveApi {
}
