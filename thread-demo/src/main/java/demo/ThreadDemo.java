package demo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * スレッド効率の比較デモ。
 *
 * 「並列度」が同じでも、ブロッキングは I/O の間もスレッドを占有し続けるため
 * タスク数と同数のスレッドが必要になる。
 * 一方ノンブロッキングは少数の共有スレッドで多数の I/O 待ちを同時に捌ける。
 *
 * 2 つのバリアントで比較する:
 * 1. BLOCKING（並列） : タスク数と同数のスレッドを生成して並列処理
 * 2. NON-BLOCKING : 少数（CPU コア数程度）のスレッドで全タスクを並列処理
 */
public class ThreadDemo {

    /** 20 件のタスク（各 1 秒の I/O 待ちを想定） */
    static final List<String> TASKS;
    static {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            list.add("タスク-" + String.format("%02d", i));
        }
        TASKS = Collections.unmodifiableList(list);
    }
    /** 各タスクの I/O 待ち時間（ミリ秒） */
    static final int TASK_MILLIS = 1_000;
    /** 両フェーズで共通のスレッド数 */
    static final int POOL_SIZE = 4;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        phase1BlockingConcurrent(); // フェーズ1: ブロッキング・並列のデモ
        phase2NonBlocking(); // フェーズ2: ノンブロッキング（Reactor）のデモ
    }

    // ─────────────────────────────────────────────────────────────
    // フェーズ1: ブロッキング（POOL_SIZE スレッド固定）
    // I/O 待ち中もスレッドを占有するため、実行できるタスクは常に POOL_SIZE 個だけ。
    // 残りはキューで待機 → 合計時間 = ceil(タスク数 / POOL_SIZE) × TASK_MILLIS
    // ─────────────────────────────────────────────────────────────
    static void phase1BlockingConcurrent() throws InterruptedException, ExecutionException {
        header("BLOCKING: スレッド数=" + POOL_SIZE + "、タスク数=" + TASKS.size());
        Set<String> threads = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(TASKS.size());
        Instant start = Instant.now();

        try (ThreadPoolExecutor pool = new ThreadPoolExecutor(
                POOL_SIZE, POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(TASKS.size()))) {

            for (String task : TASKS) {
                int active = pool.getActiveCount(); // 現在実行中のスレッド数
                int queued = pool.getQueue().size(); // キューに待機中のタスク数
                System.out.printf("[main] submit: %s  (現在実行中のスレッド数=%d, キューに待機中のタスク数=%d)%n", task, active, queued);
                // タスクをスレッドプールに投入し、空きスレッドがあれば即時実行、無ければキューに入り後で実行される
                pool.submit(() -> {
                    System.out.printf("[%s] 開始: %s%n", Thread.currentThread().getName(), task);
                    threads.add(Thread.currentThread().getName());
                    try {
                        Thread.sleep(TASK_MILLIS); // I/O 待ち中もスレッドを占有
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.printf("[%s] 完了: %s%n", Thread.currentThread().getName(), task);
                    latch.countDown(); // タスク完了をカウントダウン
                });
            }
            latch.await(); // 全タスクの完了を待機
        }

        System.out.printf("  使用スレッド数 : %d%n", threads.size());
        System.out.printf("  合計時間        : %d ms  ※ タスクは POOL_SIZE=%d ごとに直列処理されます（合計 %d 件）%n%n",
                elapsed(start), POOL_SIZE, TASKS.size());
    }

    // ─────────────────────────────────────────────────────────────
    // フェーズ2: ノンブロッキング（Reactor、スレッド数を POOL_SIZE に固定）
    // delayElement は I/O 待ち中にスレッドを解放するため、
    // POOL_SIZE スレッドで全タスクをほぼ同時に処理できる。
    // ─────────────────────────────────────────────────────────────
    static void phase2NonBlocking() throws InterruptedException {
        header("NON-BLOCKING（Reactor）: スレッド数=" + POOL_SIZE + "、タスク数=" + TASKS.size());
        Set<String> threads = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(TASKS.size());
        Instant start = Instant.now();

        // ブロッキングと同じ POOL_SIZE スレッド数に固定したスケジューラを使用
        Scheduler scheduler = Schedulers.newParallel("reactor-pool", POOL_SIZE);

        Flux.fromIterable(TASKS)
                .flatMap(name -> Mono.just(name)
                        .doOnSubscribe(s -> System.out.printf("[%s] 開始: %s%n", Thread.currentThread().getName(), name))
                        .delayElement(Duration.ofMillis(TASK_MILLIS), scheduler) // 待機中はスレッドを解放
                        .doOnNext(n -> {
                            System.out.printf("[%s] 完了: %s%n", Thread.currentThread().getName(), n);
                            threads.add(Thread.currentThread().getName());
                            latch.countDown(); // タスク完了をカウントダウン
                        }))
                .subscribe();
        latch.await(); // 全タスクの完了を待機
        scheduler.dispose(); // スケジューラをクリーンアップ

        System.out.printf("  使用スレッド数 : %d%n", threads.size());
        System.out.printf("  合計時間        : %d ms  ※ I/O 待ち中にスレッドを解放するため全タスクが並列完了%n", elapsed(start));
    }

    // ─────────────────────────────────────────────────────────────
    // ユーティリティ
    // ─────────────────────────────────────────────────────────────
    static long elapsed(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }

    static void header(String title) {
        System.out.println("═".repeat(60));
        System.out.println("  " + title);
        System.out.println("═".repeat(60));
    }
}
