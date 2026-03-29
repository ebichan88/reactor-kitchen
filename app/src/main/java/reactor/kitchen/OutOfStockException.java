package reactor.kitchen;

/**
 * 在庫切れを表す例外。
 * Reactor パイプライン内で Mono.error() に渡して onError シグナルとして伝播させる。
 */
public class OutOfStockException extends RuntimeException {
    final Dish dish;

    public OutOfStockException(Dish dish) {
        super(dish.name + " は売り切れです！");
        this.dish = dish;
    }
}
