package reactor.kitchen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * アプリケーションのエントリーポイント。
 * コンソールからの入力を受け付け、注文を Reactor のパイプラインに投入する。
 */
public class App {
    /** 注文ハンドラ */
    private final OrderHandler orderHandler;
    /** アプリの開始時に表示するメッセージ */
    private static final String message = """
            Reactor Kitchenへようこそ！
              ===== メニュー =====
              No.1 ラーメン (12000ms)
              No.2 チャーハン (8000ms)
              No.3 餃子 (4000ms)
              No.4 枝豆 (1000ms)
              ====================
            【注文】  No.   を入力
            【キャンセル】cancel <注文番号> を入力
            【終了】  quit  を入力
            """;

    /**
     * コンストラクタ。
     * 
     * @param orderHandler 注文ハンドラ
     */
    App(OrderHandler orderHandler) {
        this.orderHandler = orderHandler;
    }

    /**
     * エントリーポイントメソッド
     */
    public static void main(String[] args) {
        new App(new OrderHandler(new CookService(new Inventory()))).run();
    }

    /**
     * アプリケーションのメインループ。
     * コンソールからの入力を処理し、注文を Reactor のパイプラインに投入する。
     */
    public void run() {
        Console.menu(message);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            ConsoleInput input = new ConsoleInput(reader);
            Role role = input.selectRole();
            Console.info(role.label + "のお客様としてご入店されました");
            while (true) {
                // 入力の読み取りと処理
                String line = input.readLine().trim();
                if ("quit".equalsIgnoreCase(line)) {
                    Console.info("ご来店ありがとうございました！");
                    break;
                }
                if (line.toLowerCase().startsWith("cancel")) {
                    orderHandler.cancelOrder(line);
                    continue;
                }
                Optional<Dish> dishOpt = input.validateDishToken(line);
                if (dishOpt.isEmpty()) {
                    continue;
                }
                // 注文をパイプラインに投入
                orderHandler.submitOrder(new Order(dishOpt.get()), role);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
