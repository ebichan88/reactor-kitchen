package reactor.demo;

import reactor.core.publisher.Flux;

public class App {
    /**
     * ReactorでHello Worldをするデモ。
     * 
     * @param args
     */
    public static void main(String[] args) {
        // List から要素を順に発行する「cold」なFluxを作成する
        Flux<String> flux = Flux.just("HELLO ", "WORLD ", "DEMO!");
        // Operator: 要素を順に小文字に変換する新しいFluxを作成する
        Flux<String> lowerCaseFlux = flux.map(String::toLowerCase);
        // subscribe: 購読開始して、Flux が値を発行すれば println が呼ばれる
        lowerCaseFlux.subscribe(System.out::print); // 出力: "hello world demo!"
    }
}
