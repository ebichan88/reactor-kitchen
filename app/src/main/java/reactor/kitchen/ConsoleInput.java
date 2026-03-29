package reactor.kitchen;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * コンソール入力の読み取り・バリデーションを担当する。
 */
public class ConsoleInput {
    private final BufferedReader reader;

    /**
     * コンストラクタ
     * 
     * @param reader BufferedReader
     */
    ConsoleInput(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * コンソールから1行の入力を読み取る。
     * 
     * @return 入力された文字列
     * @throws IOException 入力の読み取りに失敗した場合
     */
    String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * お客様タイプの選択を促すメソッド。
     * 1 を入力した場合は NORMAL、2 を入力した場合は VIP を返す。
     * 入力が無効な場合は再度入力を促す。
     */
    Role selectRole() {
        Console.menu("""
                ===== お客様タイプを選択 =====
                  1: 一般のお客様
                  2: ⭐ VIP のお客様（調理時間が半分！）
                ==============================
                """);
        try {
            while (true) {
                String input = reader.readLine();
                if (input == null)
                    return Role.NORMAL;
                input = input.trim();
                if ("1".equals(input))
                    return Role.NORMAL;
                if ("2".equals(input))
                    return Role.VIP;
                Console.error("1 または 2 を入力してください");
            }
        } catch (IOException e) {
            return Role.NORMAL;
        }
    }

    /**
     * 入力された文字列が料理の番号として有効かを検証する。
     * 有効な場合は対応する Dish を返し、無効な場合はエラーメッセージを表示して empty を返す。
     * 
     * @param token ユーザーの入力文字列
     * @return 対応する Dish、または empty（無効な入力の場合）
     */
    Optional<Dish> validateDishToken(String token) {
        if (token == null || token.isBlank()) {
            Console.error("入力が空です。番号を入力してください。");
            return Optional.empty();
        }

        long dishId;
        try {
            dishId = Long.parseLong(token);
        } catch (NumberFormatException e) {
            Console.error("無効な入力: " + token);
            return Optional.empty();
        }

        Optional<Dish> dish = Dish.fromId(dishId);
        if (dish.isEmpty()) {
            Console.error("メニューにありません: " + token);
            return Optional.empty();
        }
        return dish;
    }

    /**
     * cancel コマンドから注文番号を解析する。
     * 
     * @param input ユーザーの入力文字列（例: "cancel 3"）
     * @return 注文番号。解析失敗時は empty。
     */
    static Optional<Integer> parseCancelCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            Console.error("使い方: cancel <注文番号>");
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            Console.error("無効な注文番号: " + parts[1]);
            return Optional.empty();
        }
    }
}
