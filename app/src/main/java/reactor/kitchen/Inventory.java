package reactor.kitchen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 料理ごとの在庫を管理する。
 * スレッドセーフに在庫の確保・返却を行う。
 */
public class Inventory {
    /** 各料理の初期在庫数 */
    private static final int DEFAULT_STOCK = 5;
    /** 料理ごとの在庫数を保持するマップ */
    private final ConcurrentMap<Dish, AtomicInteger> stock = new ConcurrentHashMap<>();

    /**
     * コンストラクタ
     * 全ての料理の在庫を初期化する。
     */
    public Inventory() {
        for (Dish dish : Dish.values()) {
            stock.put(dish, new AtomicInteger(DEFAULT_STOCK));
        }
    }

    /**
     * 在庫を 1 つ確保する。
     * 
     * @return 確保後の残数。確保できなかった場合は負の値にはならず例外を投げる。
     * @throws OutOfStockException 在庫がない場合
     */
    public int acquire(Dish dish) {
        AtomicInteger remaining = stock.get(dish);
        while (true) {
            int current = remaining.get();
            if (current <= 0) {
                throw new OutOfStockException(dish);
            }
            if (remaining.compareAndSet(current, current - 1)) {
                return current - 1;
            }
        }
    }

    /**
     * キャンセル時に在庫を 1 つ戻す。
     */
    public void release(Dish dish) {
        stock.get(dish).incrementAndGet();
    }

    /**
     * 指定した料理の残り在庫数を返す。
     */
    public int remaining(Dish dish) {
        return stock.get(dish).get();
    }
}
