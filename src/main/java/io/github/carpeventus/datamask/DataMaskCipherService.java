package io.github.carpeventus.datamask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 密文服务 Bean：把明文字段加密为可逆的密文串，并按批解密还原。
 *
 * <p>密文串为 URL-safe Base64（无 padding）编码的 {@code IV ‖ 密文体 ‖ 认证标签(16B)}：
 * 算法 AES-256-GCM，每次加密随机生成 12 字节 IV；明文载荷为 Jackson 序列化的
 * JSON {@code {"p": 原始字段值, "u": 当前用户, "t": epoch 秒}}。
 *
 * <p>解密按顺序还原为等长明文列表：单批最多 {@value #MAX_CIPHERS_PER_BATCH} 条；校验密文归属的
 * 当前用户与请求方一致、密文未超过 {@code data-mask.cipher-expire-time} 有效期，任一条失败整批失败，
 * 错误统一为 {@link DataMaskException}（携带错误码），不做密文与明文的映射。SDK 不注册任何 HTTP 接口
 * （见 ADR-0003）。
 */
public class DataMaskCipherService {

    /** 单批最多接受的密文条数。 */
    public static final int MAX_CIPHERS_PER_BATCH = 50;

    /** 随机 IV 长度（字节）。 */
    static final int IV_LENGTH_BYTES = 12;

    /** GCM 认证标签长度（位）。 */
    static final int TAG_LENGTH_BITS = 128;

    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;
    private static final int KEY_LENGTH_BYTES = 32;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper();

    private final CipherKeyProvider cipherKeyProvider;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final Duration cipherExpireTime;

    public DataMaskCipherService(CipherKeyProvider cipherKeyProvider, CurrentUserProvider currentUserProvider,
            Clock clock, Duration cipherExpireTime) {
        this.cipherKeyProvider = cipherKeyProvider;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.cipherExpireTime = cipherExpireTime;
    }

    /**
     * 加密单个明文，生成新的随机 IV，因此同一明文每次加密得到不同的密文串。
     *
     * @param plaintext 原始字段值
     * @return URL-safe Base64（无 padding）的密文串
     */
    public String encrypt(String plaintext) {
        byte[] key = requireCipherKey();
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        byte[] sealed = doFinal(Cipher.ENCRYPT_MODE, key, iv, payloadBytes(plaintext));
        byte[] combined = new byte[iv.length + sealed.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(sealed, 0, combined, iv.length, sealed.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    }

    /**
     * 按顺序解密密文列表，返回与输入等长的明文列表；任一条失败则整批失败，不返回部分结果。
     *
     * @param ciphertexts 密文串列表，最多 {@value #MAX_CIPHERS_PER_BATCH} 条
     * @return 与输入顺序一致、等长的明文列表
     * @throws DataMaskException 超出条数上限、用户不一致、密文失效或密文损坏
     */
    public List<String> decrypt(List<String> ciphertexts) {
        if (ciphertexts.size() > MAX_CIPHERS_PER_BATCH) {
            throw new DataMaskException(DataMaskException.ErrorCode.TOO_MANY_CIPHERS,
                    "单次最多解密 " + MAX_CIPHERS_PER_BATCH + " 条密文，实际为 " + ciphertexts.size() + " 条。");
        }
        byte[] key = requireCipherKey();
        String currentUser = currentUserProvider.currentUserId();
        long now = Instant.now(clock).getEpochSecond();
        List<String> plaintexts = new ArrayList<>(ciphertexts.size());
        for (String ciphertext : ciphertexts) {
            DecodedCipher decoded = decode(key, ciphertext);
            if (!Objects.equals(decoded.user, currentUser)) {
                throw new DataMaskException(DataMaskException.ErrorCode.USER_MISMATCH,
                        "密文归属的用户与当前用户不一致，密文不能跨用户使用。");
            }
            if (now - decoded.epochSeconds > cipherExpireTime.getSeconds()) {
                throw new DataMaskException(DataMaskException.ErrorCode.CIPHER_EXPIRED,
                        "密文已超过有效期（" + cipherExpireTime + "）。");
            }
            plaintexts.add(decoded.plaintext);
        }
        return plaintexts;
    }

    private DecodedCipher decode(byte[] key, String ciphertext) {
        if (ciphertext == null) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER, "密文为 null。");
        }
        byte[] combined;
        try {
            combined = Base64.getUrlDecoder().decode(ciphertext);
        } catch (IllegalArgumentException ex) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER,
                    "密文不是合法的 URL-safe Base64 编码。", ex);
        }
        if (combined.length < IV_LENGTH_BYTES + TAG_LENGTH_BYTES) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER,
                    "密文长度不足，无法包含 12 字节 IV 与 16 字节认证标签。");
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
        byte[] sealed = new byte[combined.length - IV_LENGTH_BYTES];
        System.arraycopy(combined, IV_LENGTH_BYTES, sealed, 0, sealed.length);
        byte[] payload;
        try {
            payload = doFinal(Cipher.DECRYPT_MODE, key, iv, sealed);
        } catch (IllegalStateException ex) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER,
                    "密文损坏或密钥不匹配。", ex);
        }
        return parsePayload(payload);
    }

    private static DecodedCipher parsePayload(byte[] payload) {
        JsonNode node;
        try {
            node = PAYLOAD_MAPPER.readTree(payload);
        } catch (IOException ex) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER,
                    "密文载荷解析失败。", ex);
        }
        JsonNode plaintext = node == null ? null : node.get("p");
        JsonNode user = node == null ? null : node.get("u");
        JsonNode time = node == null ? null : node.get("t");
        if (plaintext == null || !plaintext.isTextual() || user == null || !user.isTextual() || time == null
                || !time.canConvertToLong()) {
            throw new DataMaskException(DataMaskException.ErrorCode.MALFORMED_CIPHER,
                    "密文载荷缺少 p/u/t 字段。");
        }
        return new DecodedCipher(plaintext.asText(), user.asText(), time.asLong());
    }

    private byte[] payloadBytes(String plaintext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("p", plaintext);
        payload.put("u", currentUserProvider.currentUserId());
        payload.put("t", Instant.now(clock).getEpochSecond());
        try {
            return PAYLOAD_MAPPER.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("data-mask 密文载荷序列化失败。", ex);
        }
    }

    private byte[] requireCipherKey() {
        byte[] key = cipherKeyProvider.cipherKey();
        if (key == null || key.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException("CipherKeyProvider#cipherKey() 必须提供 32 字节 AES-256 密钥，实际为 "
                    + (key == null ? "null" : key.length + " 字节") + "。");
        }
        return key;
    }

    private static byte[] doFinal(int mode, byte[] key, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(key, KEY_ALGORITHM), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("data-mask 加解密失败。", ex);
        }
    }

    /**
     * 解出的载荷：明文、归属用户与加密时刻（epoch 秒）。
     */
    private static final class DecodedCipher {

        private final String plaintext;
        private final String user;
        private final long epochSeconds;

        private DecodedCipher(String plaintext, String user, long epochSeconds) {
            this.plaintext = plaintext;
            this.user = user;
            this.epochSeconds = epochSeconds;
        }
    }
}
