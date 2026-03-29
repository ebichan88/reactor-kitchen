package reactor.kitchen;

/**
 * お客様のタイプを表す列挙型。
 */
public enum Role {
    NORMAL("一般"),
    VIP("⭐VIP");

    final String label;

    Role(String label) {
        this.label = label;
    }

    boolean isVip() {
        return this == VIP;
    }
}
