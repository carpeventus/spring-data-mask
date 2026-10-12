package io.github.carpeventus.datamask.annotation;

/**
 * 脱敏类型，决定字段值的遮蔽规则。
 */
public enum SensitiveDataType {

    /** 邮箱：保留用户名首字符或前 1–2 位，{@code @} 后域名原样保留。 */
    EMAIL,

    /** 电话：手机号保留前 3 位与后 4 位；座机号区号保留、号码只留后 4 位。 */
    PHONE,

    /** 地址：只掩门牌级信息（栋/单元/室/号中的数字），各固定两个星号。 */
    ADDRESS,

    /** 银行卡号：保留前 6 位与后 4 位，中间按 1:1 换星。 */
    BANK_CARD_NO,

    /** 证件号：保留前 6 位与后 4 位，中间按 1:1 换星。 */
    ID_CARD_NO,

    /** 兜底全掩：固定 8 个星号。 */
    ALL
}
