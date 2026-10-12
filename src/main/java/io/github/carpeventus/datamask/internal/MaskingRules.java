package io.github.carpeventus.datamask.internal;

import io.github.carpeventus.datamask.annotation.SensitiveDataType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 各脱敏类型的遮蔽规则：对单个非空值计算脱敏结果。
 *
 * <p>不符合对应模式的值整体输出固定 8 个星号（宁可多掩也不泄漏）；null 与空串由调用方原样处理。
 */
final class MaskingRules {

    /** 不符合模式的值整体输出的固定掩码。 */
    static final String FULLY_MASKED = "********";

    /** 手机：以 1 开头的 11 位纯数字。 */
    private static final Pattern MOBILE = Pattern.compile("^1\\d{10}$");

    /** 含分隔符的座机：2–4 位区号 + 连字符 + 至少 5 位号码部分。 */
    private static final Pattern LANDLINE_WITH_SEPARATOR = Pattern.compile("^(\\d{2,4})-(\\d{5,})$");

    /** 无分隔的纯数字座机：至少 5 位，保证"只保留后 4 位"时仍有字符被遮蔽。 */
    private static final Pattern LANDLINE_DIGITS = Pattern.compile("^\\d{5,}$");

    /** 卡证类：至少 11 位纯数字（前 6 位与后 4 位不重叠）。 */
    private static final Pattern CARD_LIKE_DIGITS = Pattern.compile("^\\d{11,}$");

    /** 门牌级数字：紧邻 号楼/栋/幢/单元/室/号 的数字（"号楼"须先于"号"匹配）。 */
    private static final Pattern DOORPLATE_NUMBER = Pattern.compile("\\d+(?=号楼|栋|幢|单元|室|号)");

    private static final String DOORPLATE_MASK = "**";

    static String mask(SensitiveDataType type, String value) {
        switch (type) {
            case EMAIL:
                return maskEmail(value);
            case PHONE:
                return maskPhone(value);
            case BANK_CARD_NO:
            case ID_CARD_NO:
                return maskCardLike(value);
            case ADDRESS:
                return maskAddress(value);
            case ALL:
            default:
                return FULLY_MASKED;
        }
    }

    /**
     * 邮箱：用户名长度为 3 及以下只保留首字符、4 及以上保留前 2 位，其余 1:1 换星；{@code @} 后域名原样保留。
     */
    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1 || value.indexOf('@', at + 1) >= 0) {
            return FULLY_MASKED;
        }
        int keep = at >= 4 ? 2 : 1;
        return value.substring(0, keep) + stars(at - keep) + value.substring(at);
    }

    /**
     * 手机：保留前 3 位与后 4 位，中间 4 位换星；座机：区号原样保留、号码部分只保留后 4 位。
     */
    private static String maskPhone(String value) {
        if (MOBILE.matcher(value).matches()) {
            return value.substring(0, 3) + stars(value.length() - 7) + value.substring(value.length() - 4);
        }
        Matcher landline = LANDLINE_WITH_SEPARATOR.matcher(value);
        if (landline.matches()) {
            return landline.group(1) + "-" + keepLastFour(landline.group(2));
        }
        if (LANDLINE_DIGITS.matcher(value).matches()) {
            return keepLastFour(value);
        }
        return FULLY_MASKED;
    }

    /**
     * 银行卡号 / 证件号：保留前 6 位与后 4 位，中间 1:1 换星。
     */
    private static String maskCardLike(String value) {
        if (!CARD_LIKE_DIGITS.matcher(value).matches()) {
            return FULLY_MASKED;
        }
        return value.substring(0, 6) + stars(value.length() - 10) + value.substring(value.length() - 4);
    }

    /**
     * 地址：仅门牌级数字（号楼/栋/幢/单元/室/号 前）各替换为固定两个星号；不含门牌级数字时整体全掩。
     */
    private static String maskAddress(String value) {
        if (!DOORPLATE_NUMBER.matcher(value).find()) {
            return FULLY_MASKED;
        }
        return DOORPLATE_NUMBER.matcher(value).replaceAll(DOORPLATE_MASK);
    }

    /** 保留末 4 位、其余 1:1 换星。 */
    private static String keepLastFour(String digits) {
        return stars(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    private static String stars(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append('*');
        }
        return builder.toString();
    }

    private MaskingRules() {
    }
}
