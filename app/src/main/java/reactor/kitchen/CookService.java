package reactor.kitchen;

import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 調理サービス。
 * 注文を受けて料理を作る非同期処理を提供する。
 */
public class CookService {
    /** 在庫管理サービス */
    private final Inventory inventory;

    /**
     * コンストラクタ
     * 
     * @param inventory 在庫管理サービス
     */
    CookService(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * 注文を受けて料理を作る非同期処理を提供するメソッド。
     * 
     * @param order 注文
     * @return 調理完了後に Order を返す Mono。調理中に在庫切れが発生した場合は onError シグナルで
     *         OutOfStockException を伝播させる。
     */
    Mono<Order> asyncCook(Order order) {
        // Mono.fromCallable のポイント（わかりやすくまとめ）
        // - この Mono は「コールド」：購読（subscribe）されたときに Callable が実行される（評価が遅延される）。
        // - Callable 内で投げられた例外は自動的に onError に変換される（checked/unchecked を問わない）。
        // ここでは「注文ごとに別スレッドで料理を行う」ため、コールドな `Mono.fromCallable` を使い、
        // subscribe のたびに調理処理が開始されるようにしている。
        // - 調理開始/完了メッセージは doOnSubscribe / doOnNext で出す。
        // 在庫チェック → 在庫がなければ Mono.error() で onError シグナルを発行。
        // Mono.fromCallable で在庫確保をラップすることで、例外が自動的に onError に変換される。
        // flatMap で後続の調理パイプラインに繋ぐ。
        return Mono.fromCallable(() -> {
            int remaining = inventory.acquire(order.dish);
            Console.info(order.dish.name + " の残り在庫: " + remaining);
            return order;
        }).flatMap(o ->
        // 引数で渡せない情報をパイプラインの途中で扱いたい場合、
        // deferContextual を使って Context を読み取る方法がある。
        // ここではお客様のタイプ（VIP かどうか）を Context に入れておいて、調理時間を変える例を示す。
        Mono.deferContextual(ctx -> {
            Role role = ctx.getOrDefault("role", Role.NORMAL);
            Duration cookTime = o.dish.cookTimeFor(role);

            return Mono.just(o)
                    .doOnSubscribe(s -> Console.cookStart(
                            "調理開始 " + o + " " + role.label
                                    + " (" + cookTime.toMillis() + "ms)"
                                    + " [" + Thread.currentThread().getName() + "]"))
                    .delayElement(cookTime)
                    // doOnEach は onNext / onError / onComplete のシグナルごとに呼ばれる。
                    // signal.getContextView() でパイプラインに紐づく Context を読み取れる。
                    .doOnEach(signal -> {
                        if (!signal.isOnNext())
                            return;
                        ContextView ctxView = signal.getContextView();
                        String traceId = ctxView.getOrDefault("traceId", "N/A");
                        Console.cookDone("[trace:" + traceId + "] 調理完了 " + signal.get()
                                + " " + role.label
                                + " [" + Thread.currentThread().getName() + "]");
                    });
        }));
    }

    /**
     * キャンセル時に在庫を戻す。
     */
    void restoreStock(Dish dish) {
        inventory.release(dish);
    }
}