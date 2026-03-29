package reactor.kitchen;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 注文の投入・キャンセルを Reactor パイプラインで処理するハンドラ。
 */
public class OrderHandler {
    /** 調理サービス */
    private final CookService cookService;
    /** 進行中の注文 */
    private final ConcurrentMap<Integer, Disposable> inFlight = new ConcurrentHashMap<>();
    /** 注文番号と料理の対応 */
    private final ConcurrentMap<Integer, Dish> orderDish = new ConcurrentHashMap<>();

    OrderHandler(CookService cookService) {
        this.cookService = cookService;
    }

    /**
     * 注文をReactorパイプラインに投入する。
     *
     * パイプライン構成:
     * Mono.just(order)
     * → flatMap(asyncCook) : 非同期調理（在庫確保 + delayElement）
     * → doOnCancel : キャンセル時の後始末
     * → doOnNext : 提供完了の通知
     * → onErrorResume : 在庫切れエラーのハンドリング
     * → contextWrite : traceId / role を Context に書き込む
     * → subscribe() : 購読開始（ここでパイプラインが動き出す）
     */
    void submitOrder(Order order, Role role) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        Disposable disposable = Mono.just(order)
                .flatMap(o -> cookService.asyncCook(o))
                .doOnCancel(() -> {
                    // キャンセル時の後始末
                    // dispose() が呼ばれると delayElement のタイマーがキャンセルされ、doOnCancel が発火する。
                    inFlight.remove(order.id);
                    orderDish.remove(order.id);
                    cookService.restoreStock(order.dish);
                    Console.cancel("注文 " + order + " をキャンセルしました（在庫を戻しました）");
                })
                .doOnNext(o -> {
                    // 提供完了の通知
                    inFlight.remove(o.id);
                    orderDish.remove(o.id);
                    Console.serve("提供: " + o);
                })
                .onErrorResume(OutOfStockException.class, e -> {
                    // 在庫切れエラーのハンドリング
                    // onErrorResume: onError シグナルをキャッチして代替の Mono を返す。
                    Console.error(e.getMessage());
                    return Mono.empty();
                })
                // contextWrite: パイプラインに Context（traceId）を書き込む。
                // Reactor の Context は下流（subscribe 側）→ 上流へ伝播する。
                // そのため contextWrite はチェーンの末尾（subscribe の直前）に置く。
                // パイプライン内のどこからでも signal.getContextView() で読み取れる。
                .contextWrite(Context.of("traceId", traceId, "role", role))
                .subscribe(); // subscribe() を呼ぶとパイプラインが動き出す。ここで注文が処理される。
        inFlight.put(order.id, disposable);
        orderDish.put(order.id, order.dish);
        Console.orderAccepted("[trace:" + traceId + "] 注文を受け付けました: " + order);
    }

    void cancelOrder(String input) {
        ConsoleInput.parseCancelCommand(input).ifPresentOrElse(orderId -> {
            Disposable d = inFlight.remove(orderId);
            if (d == null) {
                Console.error("注文 #" + orderId + " は存在しないか、既に完了/キャンセル済みです");
                return;
            }
            // dispose() を呼ぶと Reactor のパイプラインがキャンセルされる。
            // delayElement 内部のタイマーが取り消され、doOnCancel が発火する。
            d.dispose();
        }, () -> {
        });
    }
}
