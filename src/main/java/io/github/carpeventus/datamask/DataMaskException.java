package io.github.carpeventus.datamask;

/**
 * SDK 统一异常：解密等操作失败时抛出，携带可编程处理的错误码。
 *
 * <p>接入方可据此转换为自己的 HTTP 响应——状态码与文案由接入方决定（见 ADR-0003）。
 */
public class DataMaskException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码。
     */
    public enum ErrorCode {

        /** 密文已超过有效期（{@code data-mask.cipher-expire-time}）。 */
        CIPHER_EXPIRED,

        /** 密文归属的用户与当前用户不一致。 */
        USER_MISMATCH,

        /** 密文损坏或非法编码。 */
        MALFORMED_CIPHER,

        /** 单批解密超出条数上限。 */
        TOO_MANY_CIPHERS
    }

    private final ErrorCode errorCode;

    public DataMaskException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DataMaskException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * @return 可编程处理的错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
