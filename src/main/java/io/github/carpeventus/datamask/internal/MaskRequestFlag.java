package io.github.carpeventus.datamask.internal;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 请求作用域的脱敏标记：{@code @SensitiveApi} 命中时置位，Jackson 序列化标注字段时读取。
 *
 * <p>没有请求上下文（如直接使用 ObjectMapper）时不标记、不脱敏。
 */
public final class MaskRequestFlag {

    private static final String ATTRIBUTE_NAME = MaskRequestFlag.class.getName();

    /**
     * 在当前请求上置位脱敏标记。
     */
    public static void mark() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTRIBUTE_NAME, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 当前请求是否命中脱敏标记。
     */
    public static boolean isActive() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes != null
                && Boolean.TRUE.equals(attributes.getAttribute(ATTRIBUTE_NAME, RequestAttributes.SCOPE_REQUEST));
    }

    private MaskRequestFlag() {
    }
}
