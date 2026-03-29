package reactor.kitchen;

import java.util.concurrent.atomic.AtomicInteger;

public class Order {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);
    final int id;
    final Dish dish;

    Order(Dish dish) {
        this.id = ID_GEN.getAndIncrement();
        this.dish = dish;
    }

    @Override
    public String toString() {
        return "#" + id + "(" + dish.name + ")";
    }
}
