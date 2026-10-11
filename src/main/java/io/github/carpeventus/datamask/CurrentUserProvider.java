package io.github.carpeventus.datamask;

/**
 * 当前用户扩展点：接入方实现该接口并注册为 Spring Bean，提供发起当前请求的已登录用户标识。
 *
 * <p>该标识会写入密文载荷的 {@code u} 字段，并在解密时校验归属；SDK 每次使用都会调用，
 * 不做缓存（如需缓存请在实现内自行处理）。SDK 启用时缺少该 Bean 会导致应用启动失败。
 */
public interface CurrentUserProvider {

    /**
     * 当前登录用户标识。
     *
     * @return 用户标识，例如用户 ID
     */
    String currentUserId();
}
