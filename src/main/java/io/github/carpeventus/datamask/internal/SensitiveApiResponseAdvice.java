package io.github.carpeventus.datamask.internal;

import io.github.carpeventus.datamask.annotation.SensitiveApi;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 读取处理器方法上的 {@code @SensitiveApi} 注解，并在 Jackson 写出响应体前置位请求脱敏标记。
 *
 * <p>刻意不实现 servlet 类型的 {@code HandlerInterceptor}：那会把 jar 绑死在
 * {@code javax.servlet} 或 {@code jakarta.servlet} 上，破坏 Boot 2 / Boot 3 共用一个构件
 * （见 ADR-0004）。本类只依赖与 servlet 命名空间无关的类型，Boot 2.7 与 Boot 3 均可装配。
 */
@ControllerAdvice
public class SensitiveApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(SensitiveApi.class) != null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        MaskRequestFlag.mark();
        return body;
    }
}
