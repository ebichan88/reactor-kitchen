package reactor.kitchen;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 提供する料理の種類を表す列挙型。
 * 各料理には ID、名前、調理にかかるおおよその時間を定義している。
 */
public enum Dish {
    RAMEN(1, "ラーメン", Duration.ofMillis(12000)),
    CHAHAN(2, "チャーハン", Duration.ofMillis(8000)),
    GYOZA(3, "餃子", Duration.ofMillis(4000)),
    EDAMAME(4, "枝豆", Duration.ofMillis(1000));

    final long id;
    final String name;
    final Duration approxTime;

    Dish(long id, String name, Duration approxTime) {
        this.id = id;
        this.name = name;
        this.approxTime = approxTime;
    }

    /**
     * 料理の ID から対応する Dish を取得する。
     * 
     * @param id 料理の ID
     * @return 対応する Dish、または empty（無効な ID の場合）
     */
    public static Optional<Dish> fromId(long id) {
        return Arrays.stream(values())
                .filter(d -> d.id == id)
                .findFirst();
    }

    /**
     * お客様タイプに応じた調理時間を返す。
     * VIP の場合は通常の半分の時間になる。
     * 
     * @param role お客様のタイプ
     * @return 調理時間
     */
    public Duration cookTimeFor(Role role) {
        return role.isVip() ? approxTime.dividedBy(2) : approxTime;
    }
}
