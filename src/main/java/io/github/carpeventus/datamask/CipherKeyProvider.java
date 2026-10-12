package io.github.carpeventus.datamask;

/**
 * 密文密钥扩展点：接入方实现该接口并注册为 Spring Bean，提供 32 字节 AES-256 密钥。
 *
 * <p>密钥来源（配置、KMS、远端服务等）与缓存策略完全由接入方决定；SDK 每次使用都会调用，
 * 不做缓存。密钥长度不是 32 字节时，使用处会直接报错。SDK 启用时缺少该 Bean 会导致应用启动失败。
 */
public interface CipherKeyProvider {

    /**
     * AES-256 密钥，必须为 32 字节。
     *
     * @return 32 字节密钥
     */
    byte[] cipherKey();
}
