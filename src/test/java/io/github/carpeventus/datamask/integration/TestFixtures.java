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
}
