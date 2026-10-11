package io.github.carpeventus.datamask.integration;

import io.github.carpeventus.datamask.annotation.SensitiveData;
import io.github.carpeventus.datamask.annotation.SensitiveDataType;

import java.util.List;

/**
 * 集成测试用的响应/请求结构：覆盖顶层字段、嵌套对象与 List 元素中的标注字段。
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static final class OrderView {

        @SensitiveData(type = SensitiveDataType.ALL)
        private final String phone;

        private final String nickname;

        private final AddressView address;

        private final List<CardView> cards;

        public OrderView(String phone, String nickname, AddressView address, List<CardView> cards) {
            this.phone = phone;
            this.nickname = nickname;
            this.address = address;
            this.cards = cards;
        }

        public String getPhone() {
            return phone;
        }

        public String getNickname() {
            return nickname;
        }

        public AddressView getAddress() {
            return address;
        }

        public List<CardView> getCards() {
            return cards;
        }
    }

    public static final class AddressView {

        @SensitiveData(type = SensitiveDataType.ALL)
        private final String email;

        private final String city;

        public AddressView(String email, String city) {
            this.email = email;
            this.city = city;
        }

        public String getEmail() {
            return email;
        }

        public String getCity() {
            return city;
        }
    }

    public static final class CardView {

        @SensitiveData(type = SensitiveDataType.ALL)
        private final String cardNo;

        private final String bank;

        public CardView(String cardNo, String bank) {
            this.cardNo = cardNo;
            this.bank = bank;
        }

        public String getCardNo() {
            return cardNo;
        }

        public String getBank() {
            return bank;
        }
    }

    public static final class EchoRequest {

        @SensitiveData(type = SensitiveDataType.ALL)
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static final class EchoView {

        private final String echo;

        @SensitiveData(type = SensitiveDataType.ALL)
        private final String masked;

        public EchoView(String echo, String masked) {
            this.echo = echo;
            this.masked = masked;
        }

        public String getEcho() {
            return echo;
        }

        public String getMasked() {
            return masked;
        }
    }

    /**
     * 联系方式视图：EMAIL 与 PHONE（手机/座机）各规则样例，含嵌套对象与 List 元素。
     */
    public static final class ContactView {

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String email;

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String shortEmail;

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String longEmail;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String mobile;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String landline;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String landlineShortArea;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String landlineWithoutSeparator;

        private final NestedRuleView nested;

        private final List<NestedRuleView> items;

        public ContactView(String email, String shortEmail, String longEmail, String mobile, String landline,
                String landlineShortArea, String landlineWithoutSeparator, NestedRuleView nested,
                List<NestedRuleView> items) {
            this.email = email;
            this.shortEmail = shortEmail;
            this.longEmail = longEmail;
            this.mobile = mobile;
            this.landline = landline;
            this.landlineShortArea = landlineShortArea;
            this.landlineWithoutSeparator = landlineWithoutSeparator;
            this.nested = nested;
            this.items = items;
        }

        public String getEmail() {
            return email;
        }

        public String getShortEmail() {
            return shortEmail;
        }

        public String getLongEmail() {
            return longEmail;
        }

        public String getMobile() {
            return mobile;
        }

        public String getLandline() {
            return landline;
        }

        public String getLandlineShortArea() {
            return landlineShortArea;
        }

        public String getLandlineWithoutSeparator() {
            return landlineWithoutSeparator;
        }

        public NestedRuleView getNested() {
            return nested;
        }

        public List<NestedRuleView> getItems() {
            return items;
        }
    }

    /**
     * 嵌套/List 元素中的标注字段样例。
     */
    public static final class NestedRuleView {

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String email;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String phone;

        public NestedRuleView(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }

    /**
     * 证件类视图：BANK_CARD_NO / ID_CARD_NO / ADDRESS 样例。
     */
    public static final class DocumentView {

        @SensitiveData(type = SensitiveDataType.BANK_CARD_NO)
        private final String bankCard;

        @SensitiveData(type = SensitiveDataType.ID_CARD_NO)
        private final String idCard;

        @SensitiveData(type = SensitiveDataType.ADDRESS)
        private final String address;

        @SensitiveData(type = SensitiveDataType.ADDRESS)
        private final String buildingAddress;

        public DocumentView(String bankCard, String idCard, String address, String buildingAddress) {
            this.bankCard = bankCard;
            this.idCard = idCard;
            this.address = address;
            this.buildingAddress = buildingAddress;
        }

        public String getBankCard() {
            return bankCard;
        }

        public String getIdCard() {
            return idCard;
        }

        public String getAddress() {
            return address;
        }

        public String getBuildingAddress() {
            return buildingAddress;
        }
    }

    /**
     * 边界值视图：null、空串与不符合模式的值。
     */
    public static final class EdgeCaseView {

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String nullEmail;

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String emptyEmail;

        @SensitiveData(type = SensitiveDataType.EMAIL)
        private final String notAnEmail;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String tooShortPhone;

        @SensitiveData(type = SensitiveDataType.PHONE)
        private final String notAPhone;

        @SensitiveData(type = SensitiveDataType.BANK_CARD_NO)
        private final String tooShortCard;

        @SensitiveData(type = SensitiveDataType.ADDRESS)
        private final String addressWithoutDoorplate;

        public EdgeCaseView(String nullEmail, String emptyEmail, String notAnEmail, String tooShortPhone,
                String notAPhone, String tooShortCard, String addressWithoutDoorplate) {
            this.nullEmail = nullEmail;
            this.emptyEmail = emptyEmail;
            this.notAnEmail = notAnEmail;
            this.tooShortPhone = tooShortPhone;
            this.notAPhone = notAPhone;
            this.tooShortCard = tooShortCard;
            this.addressWithoutDoorplate = addressWithoutDoorplate;
        }

        public String getNullEmail() {
            return nullEmail;
        }

        public String getEmptyEmail() {
            return emptyEmail;
        }

        public String getNotAnEmail() {
            return notAnEmail;
        }

        public String getTooShortPhone() {
            return tooShortPhone;
        }

        public String getNotAPhone() {
            return notAPhone;
        }

        public String getTooShortCard() {
            return tooShortCard;
        }

        public String getAddressWithoutDoorplate() {
            return addressWithoutDoorplate;
        }
    }

    /**
     * 误标样例：{@code @SensitiveData} 标在非 String 字段上，序列化时应报错而不是静默跳过。
     */
    public static final class BadFieldView {

        @SensitiveData(type = SensitiveDataType.ALL)
        private final Integer count;

        public BadFieldView(Integer count) {
            this.count = count;
        }

        public Integer getCount() {
            return count;
        }
    }

    public static ContactView sampleContacts() {
        return new ContactView(
                "dgs@qq.com",
                "ab@qq.com",
                "hello@qq.com",
                "13812345678",
                "0571-88889999",
                "010-12345678",
                "88889999",
                new NestedRuleView("room@qq.com", "13900001111"),
                java.util.Arrays.asList(
                        new NestedRuleView("in-list@qq.com", "13900002222"),
                        new NestedRuleView("second@qq.com", "010-12345678")));
    }

    public static DocumentView sampleDocuments() {
        return new DocumentView(
                "6222020200112233",
                "330100199001011234",
                "浙江省杭州市西湖区文一西路969号3栋2单元502室",
                "幸福小区8号楼1单元901室");
    }

    public static EdgeCaseView sampleEdgeCases() {
        return new EdgeCaseView(null, "", "未填写", "1234", "1381234567x", "6222022233", "浙江省杭州市西湖区文一西路");
    }
}
