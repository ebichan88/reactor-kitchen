package reactor.kitchen;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ANSI カラー付きコンソール出力ユーティリティ。
 * 注文受付・調理開始・提供などを色分けして視認性を高める。
 */
public final class Console {

    // ANSI エスケープコード
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String CYAN = "\033[36m";
    private static final String MAGENTA = "\033[35m";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Console() {
    }

    private static String timestamp() {
        return DIM + "[" + LocalTime.now().format(TIME_FMT) + "]" + RESET + " ";
    }

    /** メニュー表示 (BOLD) */
    public static void menu(String text) {
        System.out.println(BOLD + text + RESET);
    }

    /** 注文受付 (CYAN) */
    public static void orderAccepted(String text) {
        System.out.println(timestamp() + CYAN + "📝 " + text + RESET);
    }

    /** 調理開始 (YELLOW) */
    public static void cookStart(String text) {
        System.out.println(timestamp() + YELLOW + "🍳 " + text + RESET);
    }

    /** 調理完了 (GREEN) */
    public static void cookDone(String text) {
        System.out.println(timestamp() + GREEN + "✅ " + text + RESET);
    }

    /** 提供 (GREEN + BOLD) */
    public static void serve(String text) {
        System.out.println(timestamp() + GREEN + BOLD + "🍽  " + text + RESET);
    }

    /** エラー / バリデーション失敗 (RED) */
    public static void error(String text) {
        System.out.println(timestamp() + RED + "⚠  " + text + RESET);
    }

    /** キャンセル (RED + BOLD) */
    public static void cancel(String text) {
        System.out.println(timestamp() + RED + BOLD + "🚫 " + text + RESET);
    }

    /** 情報メッセージ (MAGENTA) */
    public static void info(String text) {
        System.out.println(timestamp() + MAGENTA + text + RESET);
    }
}
