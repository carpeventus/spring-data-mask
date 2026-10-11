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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 密文服务 Bean：把明文字段加密为可逆的密文串，并按批解密还原。
 *
 * <p>密文串为 URL-safe Base64（无 padding）编码的 {@code IV ‖ 密文体 ‖ 认证标签(16B)}：
 * 算法 AES-256-GCM，每次加密随机生成 12 字节 IV；明文载荷为 Jackson 序列化的
 * JSON {@code {"p": 原始字段值, "u": 当前用户, "t": epoch 秒}}。
 *
 * <p>SDK 不注册任何 HTTP 接口，解密只通过本 Bean 提供（见 ADR-0003）。
 */
public class DataMaskCipherService {

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

    public DataMaskCipherService(CipherKeyProvider cipherKeyProvider, CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.cipherKeyProvider = cipherKeyProvider;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
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
     * 按顺序解密密文列表，返回与输入等长的明文列表；不做密文与明文的映射。
     *
     * @param ciphertexts 密文串列表
     * @return 与输入顺序一致、等长的明文列表
     */
    public List<String> decrypt(List<String> ciphertexts) {
        byte[] key = requireCipherKey();
        List<String> plaintexts = new ArrayList<>(ciphertexts.size());
        for (String ciphertext : ciphertexts) {
            plaintexts.add(decryptOne(key, ciphertext));
        }
        return plaintexts;
    }

    private String decryptOne(byte[] key, String ciphertext) {
        byte[] combined = Base64.getUrlDecoder().decode(ciphertext);
        if (combined.length < IV_LENGTH_BYTES + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("密文长度不足，无法包含 12 字节 IV 与 16 字节认证标签。");
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
        byte[] sealed = new byte[combined.length - IV_LENGTH_BYTES];
        System.arraycopy(combined, IV_LENGTH_BYTES, sealed, 0, sealed.length);
        byte[] payload;
        try {
            payload = doFinal(Cipher.DECRYPT_MODE, key, iv, sealed);
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException("密文损坏或密钥不匹配。", ex);
        }
        return payloadPlaintext(payload);
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

    private static String payloadPlaintext(byte[] payload) {
        JsonNode node;
        try {
            node = PAYLOAD_MAPPER.readTree(payload);
        } catch (IOException ex) {
            throw new IllegalArgumentException("密文载荷解析失败。", ex);
        }
        JsonNode plaintext = node.get("p");
        if (plaintext == null || !plaintext.isTextual()) {
            throw new IllegalArgumentException("密文载荷缺少明文字段 p。");
        }
        return plaintext.asText();
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
}
