[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic)

# JSONIC

Simple JSON encoder/decoder written in java

2018/7/1 JSONIC は、リポジトリを GitHub に移動するとともに今後機能強化が行われることがないメンテナンスモードに移行します。機能、パフォーマンス共に優れた [jackson](https://github.com/FasterXML/jackson) への移行をおすすめいたします。

## ビルド

JDK 21 と Maven 3.9 以降を使用し、プロジェクトルートで実行します。
`mise` を使用する場合は `mise install` の後、`mise exec -- mvn clean verify` を実行してください。

```sh
mvn clean verify
```

成果物は `target/jsonic-1.4.0.jar` に生成されます（Java 21 向けにコンパイル）。
ソースは `src/main/java`、リソースは `src/main/resources`、
テストとテストデータは `src/test/java`・`src/test/resources` に配置しています。

テストは JUnit 6.1.3（Jupiter）で実行します。REST・RPCテストでは、
`sample/basic` と `sample/spring` を `target/web-test` に準備し、
ローカルの空きポートで Jetty を起動します。サンプルの準備はテスト内で行います。
既存テストの日付表現に合わせて、テスト JVM のロケールは日本語、タイムゾーンは
`Asia/Tokyo` に固定しています。初回ビルドには Maven Central への接続が必要です。

## Webサンプルの実行

Java 21 と Jakarta Servlet 6.1 対応サーバーを使用します。Web API は
`javax.servlet` から `jakarta.servlet` へ移行しています。

RESTのJSONP対応は廃止しました。`callback`を指定しても通常のJSONを返します。
`_method`によるHTTPメソッド上書きはPOSTのみ有効です。
RESTのPOST・PUT・DELETEは、`Origin`があればスキーム・ホスト・ポートの一致を検証し、
異なるOriginや`null`、不正な値を403で拒否します。`Origin`がない場合は
`Content-Type: application/json`または`X-Requested-With: XMLHttpRequest`が必要です。
`Sec-Fetch-Site: cross-site`も拒否します。フォーム形式を送るAPIクライアントは
`X-Requested-With: XMLHttpRequest`を付けてください。同一Originのブラウザフォームと
同梱のjQueryサンプルは引き続き利用できます。
リバースプロキシ経由では、Servletコンテナが公開URLのスキーム・ホスト・ポートを
認識するよう、信頼するプロキシをコンテナ側で設定してください。

GatewayFilterはServletコンテナが解釈したパスで、forward先でも認可を確認します。
転送先でもフィルタが実行されるよう、`/*`に対して`REQUEST`と`FORWARD`を登録してください。
圧縮・文字コード設定・設定によるforwardは1リクエストにつき1回だけ適用します。
DynaBean対応のオプション依存であるCommons BeanUtilsは1.11.0を使用します。

### Web APIの設定上の注意

GatewayFilterを使用する場合は、`web.xml`の`filter-mapping`を次のように設定してください。
`filter-name`は、使用している`filter`定義の名前に合わせます。
`FORWARD`を省略すると転送先でフィルタが実行されず、転送先の認可チェックが働きません。

```xml
<filter-mapping>
  <filter-name>Gateway Filter</filter-name>
  <url-pattern>/*</url-pattern>
  <dispatcher>REQUEST</dispatcher>
  <dispatcher>FORWARD</dispatcher>
</filter-mapping>
```

`Origin`を送らないクライアントでフォーム形式のPOSTを行う場合は、次のようにヘッダーを追加します。
`_method=PUT`や`_method=DELETE`を使うPOSTも同じ条件です。

```sh
curl -X POST 'http://localhost:8080/basic/rest/memo.json' \
  -H 'X-Requested-With: XMLHttpRequest' \
  --data-urlencode 'title=sample' \
  --data-urlencode 'text=sample memo'
```

このヘッダーを付けても、異なる`Origin`を送るリクエストは許可されません。
リバースプロキシを利用する場合の要件も含め、
[RESTのCSRF対策とクライアント設定](docs/webservice.html#csrf)を参照してください。

### 同梱ライブラリ


`sample/spring/WEB-INF/lib` には次のバージョン付きjarを同梱しています。

| ライブラリ | バージョン |
|---|---|
| Spring Framework（aop / beans / context / core / expression / web） | 7.0.9 |
| Apache Commons Logging | 1.4.0 |
| Micrometer（commons / observation） | 1.17.1 |
| JSpecify | 1.0.1 |

`mvn clean verify` でREST・RPCテストを実行すると、依存jarと
コンパイル済みサービスを含むサンプルが `target/web-test/spring` に作られます。
JSONIC本体のjarを追加すると、Jakarta Servlet 6.1 対応サーバーの
Webアプリケーションとして配置できます。

```sh
cp target/jsonic-1.4.0.jar target/web-test/spring/WEB-INF/lib/
```

Servlet APIのjarはサーバーが提供するため、`WEB-INF/lib` には含めません。
テストでは Jetty 12.1.13 を起動し、同梱jarを使ってREST・RPCを検証します。

同梱依存jarを `pom.xml` のバージョンから再取得する場合は、次を実行します。

```sh
mise exec -- mvn org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy-dependencies \
  -DincludeScope=runtime \
  -DincludeGroupIds=org.springframework,commons-logging,org.jspecify,io.micrometer \
  -DoutputDirectory=sample/spring/WEB-INF/lib
```

バージョン変更時は、旧バージョンのjarを除いてから取得してください。

## 性能の計測

標準の `JSON` と組み込みの `NamingStyle` では、Bean の読み書きに必要な
プロパティ一覧・アノテーション・型情報と、エスケープ済みの出力キーを
クラスと命名規則ごとに再利用します。`JSON.encode/decode` のように毎回
インスタンスを生成する API にも適用されます。設定や処理中の `Context` は
共有しません。独自の `JSON` サブクラスや `NamingStyle` は、呼び出しごとに
プロパティ情報を構築するため、動的な `normalize/ignore` の動作を維持します。
エラーメッセージの検索は必要時まで遅延し、参照型配列への代入は
リフレクションを介さず行います。

型付きの `parse/decode` では、標準の Bean と配列を対象に、トークンを配列へ
保持してから POJO・最終配列へ直接変換します。構文検証が完了するまでは
コンストラクターや setter を呼ばず、重複キーは最後の値を採用し、最初の
出現順で代入します。通常の Bean の引数なしコンストラクター情報も再利用します。
未知のプロパティは、構文検証後に変換せず読み飛ばします。

独自の `JSON` / `NamingStyle`、型指定なしの値、特殊な変換を持つ型は
従来経路を使います。`JSONHint` がある値や別名のキーなどは必要な部分だけ
中間ツリーを作って従来の変換規則を適用します。最大深度を 64 より大きく
設定した場合も従来経路を使い、非常に深い入力を再帰処理へ切り替えません。
`JSONReader.getValue` のストリーミング API の動作は変更していません。

JMH と Jackson の比較は任意の `benchmark` プロファイルで実行します。
比較用依存関係はテスト用で、通常のビルドや配布 JAR には含まれません。

```sh
mise exec -- mvn -Pbenchmark test-compile \
  org.apache.maven.plugins:maven-dependency-plugin:3.11.0:build-classpath \
  -Dmdep.outputFile=target/benchmark-classpath.txt -DincludeScope=test
benchmark_cp="target/test-classes:target/classes:$(cat target/benchmark-classpath.txt)"
mise exec -- java -cp "$benchmark_cp" org.openjdk.jmh.Main BeanBenchmark \
  -prof gc -rf json -rff target/benchmark.json
```

`mise` を使わない場合は JDK 21 を選択して `mise exec --` を省略してください。
標準設定は 2 forks、各 fork でウォームアップ 3 回・測定 5 回（各 1 秒）、
ヒープ 256 MiB、1 スレッドです。`-p text=ascii` で ASCII のみに絞れます。
`-t 8` を追加すると、共有インスタンスを使う 8 スレッドの計測ができます。

対象は 1 件／100 件の POJO 配列、ASCII／日本語とエスケープを含む文字列、
String 入出力です。JSONIC は静的 API とインスタンス再利用を別々に測り、
Jackson 3.2.3 は `JsonMapper` を再利用します（Blackbird なし）。
出力順を揃え、測定前に JSON 文字列とデコード結果の一致を検証します。
結果の `us/op` は配列全体の処理時間、`gc.alloc.rate.norm` は割り当て量
`B/op` です。初回のクラス解析、UTF-8 入出力、型指定なしの Map/List 処理は
このベンチマークの対象外です。

以下は JSONIC の改善履歴です。ライブラリ間の比較は第五段階の Jackson 3.2.3 の測定に統一しています。

第一段階（メタデータ再利用）の 2026-10-01 計測例（AMD Ryzen AI 9 465、WSL2、OpenJDK 21.0.2、
上記の標準設定、`text=ascii`）。改修前は `2e87e6c1` の実装です。
時間は小さいほど高速です。数値はこのデータと環境での平均であり、一般的な性能保証ではありません。

| API | 件数 | 改修前 µs/op | 改修後 µs/op | 割り当て量 B/op（前 → 後） |
|---|---:|---:|---:|---:|
| `format` | 1 | 1.110 | 0.814 | 2408 → 1888 |
| `format` | 100 | 23.970 | 21.659 | 37704 → 37184 |
| `parse` | 1 | 1.618 | 1.322 | 2464 → 1952 |
| `parse` | 100 | 77.649 | 74.576 | 73625 → 68361 |
| `JSON.encode` | 1 | 1.156 | 0.859 | 2592 → 2072 |
| `JSON.encode` | 100 | 23.899 | 21.696 | 37888 → 37368 |
| `JSON.decode` | 1 | 1.636 | 1.369 | 2648 → 2136 |
| `JSON.decode` | 100 | 76.859 | 72.154 | 73809 → 68544 |

第一段階では小さい POJO の処理時間と割り当て量が改善しましたが、100 件の
デコードの時間差は小さい結果でした。続いて上記の直接変換を追加しています。

<a id="introduction"></a>

第二段階（トークンからの直接変換）の計測は、同じ JDK・入力・ヒープ・
2 forks で、ウォームアップを 5 回に増やして行いました。比較基準は一時コピーで
`TypedDecoder.supports` を `false` にし、第一段階のキャッシュ等を残した
従来のツリー変換経路です。下表は ASCII の POJO 配列に対する測定結果です。

| API | 件数 | 従来経路 µs/op | 直接変換 µs/op | 割り当て量 B/op（前 → 後） |
|---|---:|---:|---:|---:|
| `parse` | 1 | 1.307 | 0.728 | 1952 → 1464 |
| `parse` | 100 | 72.186 | 58.280 | 68360 → 45512 |
| `JSON.decode` | 1 | 1.359 | 0.763 | 2136 → 1648 |
| `JSON.decode` | 100 | 71.427 | 58.298 | 68544 → 45696 |

この段階では数値の `BigDecimal` 化と文字単位のトークン解析が残っており、続く段階で改善しています。
測定の JSON と検証ログはローカルの `target/performance-phase2/` に保存しています。

第三段階では、エスケープのない `String` 入力をまとめて走査し、文字列を
一時バッファへコピーせずにキャッシュへ照合する経路を追加しました。
エスケープを含む文書と `Reader`・可変 `CharSequence` は従来の文字列解析を使います。
型付き変換では、ヒントのない文字列・真偽値・整数・倍精度値の変換器検索も省きます。
整数の桁あふれ検査、重複キー、例外の位置情報は維持しています。

2026-10-01、同じ JDK・ヒープ・2 forks、ウォームアップと測定を各 5 × 1 秒、
`-prof gc` で第三段階の変更前後を隔離コピーにて順次測定しました。
変更前は第二段階の直接変換を有効にした実装です。下表は `parse(String, Item[].class)` の平均です。

| 入力 | 件数 | 今回の変更前 µs/op | 変更後 µs/op | 時間短縮 |
|---|---:|---:|---:|---:|
| ascii | 1 | 0.741 | 0.573 | 22.7% |
| ascii | 100 | 59.469 | 44.823 | 24.6% |
| japanese | 1 | 0.927 | 0.810 | 12.7% |
| japanese | 100 | 78.976 | 69.065 | 12.5% |

この測定では ASCII 入力の処理時間を約 23～25% 短縮しました。
割り当て量はほぼ同じで、今回の主な効果は処理時間の短縮です。
最初の候補は日本語 100 件で退行したため、文書単位の経路選択へ変更してから採用しました。

全 80 テスト中 79 成功。`WebSecurityTest.onlyPostCanOverrideMethod` の
HTTP 204 を期待して 403 になる失敗は変更前でも再現しており、今回の最適化とは別の既存失敗です。
追加した文字列テストでは通常文字・制御文字・エスケープ・日本語・長文・キャッシュ衝突を
Reader 経路と比較しています。測定値と検証ログは `target/performance-phase3/` に保存しています。

第四段階では、型付きデコードに短い数値専用のトークンを追加しました。
`String` 入力の符号・小数点を除く 18 桁以内の通常の数値をまとめて走査し、
`int`・`long`・`double` へ変換する際の `BigDecimal` 生成を省きます。
指数表記、大きな数、特殊な区切り、ストリーム入力は従来の処理に戻します。
型指定のない値やヒント付き変換では必要に応じて元の `BigDecimal` を復元し、
小数点以下の桁数も維持します。浮動小数点の直接計算はオペランドを正確に表現できる範囲に限定します。

第三段階を有効にした変更前コピーと比較しました。環境・JMH 設定は第三段階と同じ
（2 forks、ウォームアップ・測定各 5 × 1 秒、GC profiler）です。
以下は最終実装の測定値です。± は JMH の 99.9% 信頼区間の半幅です。

| 入力 | 件数 | 変更前 µs/op | 変更後 µs/op | 割り当て量 B/op（前 → 後） |
|---|---:|---:|---:|---:|
| ascii | 1 | 0.585 ± 0.018 | 0.544 ± 0.014 | 1472 → 1408 |
| ascii | 100 | 43.604 ± 2.189 | 43.597 ± 1.106 | 45520 → 42448 |
| japanese | 1 | 1.041 ± 0.385 | 0.772 ± 0.021 | 1580 → 1584 |
| japanese | 100 | 70.185 ± 2.558 | 68.201 ± 3.278 | 51616 → 48600 |

ASCII 1 件は約 7% 短縮しましたが、100 件の処理時間は誤差を含めるとほぼ横ばいです。
日本語 1 件の変更前測定はばらつきが大きく、大きな高速化率として扱えません。
今回の確実な効果は、100 件での割り当て量を ASCII 約 7%・日本語約 6% 減らしたことです。

境界値、3,000 種類の乱数による小数、浮動小数点のビット一致、整数の範囲外、
不正構文と例外、型なし値・ヒント・公開 Reader の互換性を検証し、全 89 テストと
`mvn verify` が成功しました。結果とログは `target/performance-phase4/` に保存しています。

第五段階では JFR の CPU サンプルを調べ、反射による代入より先に、パーサーの
状態切り替えと文字列処理を改善しました。ASCII では `JSONParser.next` と
`LocalCache.getString`、日本語では `StringBuilder` の容量確認や文字列解析が上位でした。

`StringDecoder` が `String` 入力を既存の型付きトークンバッファへ直接読み込みます。
エスケープ間の文字列をまとめてコピーし、イベントごとのパーサー状態更新を省きます。
重複キー、ヒント、全構文検証前にユーザーのコンストラクタ・setter を呼ばない保証は維持します。
指数表記・大きな数・深さ制限付近・特殊構文など、対応しない入力は従来のパーサーへ戻ります。
不正入力も従来パーサーで診断するため、エラー位置や例外の互換性を維持します。
一部の入力は途中まで走査した後に再解析するため、そのケースの高速化は保証しません。

仕様削除も検討対象としましたが、今回は削除せずに大きな改善が得られました。
文書全体の検証を待たずに代入する方式はさらに処理をまとめられる可能性がある一方、
不正な末尾があってもユーザーコードが実行されるようになります。今回その変更は採用していません。

以下は OpenJDK 21.0.2、JMH 1.37、Jackson **3.2.3**（Blackbird なし）の計測です。
2 forks、各 fork のウォームアップ・測定各 5 × 1 秒、256 MiB ヒープ、1 スレッド、
`-prof gc`。JFR 収集と正式な性能測定は別に実行しました。
JSONIC の変更前後は同じソース一式の隔離コピーで比較し、比較対象は Jackson 3 系のみです。
± は JMH の 99.9% 信頼区間の半幅です。時間は配列全体の µs/op です。

| 入力 | 件数 | JSONIC 変更前 | JSONIC 変更後 | 時間短縮 | Jackson 3.2.3 |
|---|---:|---:|---:|---:|---:|
| ascii | 1 | 0.585 ± 0.044 | 0.383 ± 0.013 | 34.6% | 0.493 ± 0.024 |
| ascii | 100 | 41.369 ± 1.187 | 30.615 ± 0.932 | 26.0% | 24.191 ± 1.730 |
| japanese | 1 | 0.741 ± 0.038 | 0.471 ± 0.014 | 36.4% | 0.537 ± 0.017 |
| japanese | 100 | 67.759 ± 1.699 | 35.717 ± 2.045 | 47.3% | 25.385 ± 1.048 |

この入力では 1 件は Jackson 3.2.3 より短時間で処理できました。100 件では、
同等になるために ASCII でさらに約 1.27 倍、日本語で約 1.41 倍の高速化が必要です。
この結果は上記 POJO 配列の型付きデコードに限られ、一般的な優劣を示すものではありません。

新たにエスケープ・約 3,000 件の構文変異・ネストした値を従来実装と比較するテストを追加し、
全 92 テストと `mvn verify` が成功しました。測定 JSON・JFR・検証ログは
`target/performance-phase5/` に保存しています。

第六段階では、改善後の JFR サンプルを取り直し、プロパティ検索と型付き変換を改善しました。
複数要素の配列では、最初の重複・未知キーのないオブジェクトのキー順とプロパティを記録します。
後続オブジェクトの全キーが同じ順序で一致した場合、名前検索と作業用インデックス配列の作成を省きます。
順序・キーの数・名前が違う場合は従来の変換処理を使います。記録する配置は文書ごとに最大 1 種類で、
単一要素のための準備や入力を共有キャッシュへ保持する処理は追加していません。
ジェネリック型の解決は各変換の対象型に従い、別の型の解決結果を再利用しません。

文字列配列では、要素が文字列または null なら専用の処理で格納します。
数値・オブジェクトなどの文字列への変換やヒント付きの変換は従来の経路を使います。
重複キー、代入順、エラーのパス、文書全体の検証前にユーザーコードを呼ばない保証は維持しています。

OpenJDK 21.0.2、JMH 1.37、Jackson **3.2.3**（Blackbird なし）。
2 forks、ウォームアップ・測定各 5 × 1 秒、256 MiB ヒープ、1 スレッド、GC profiler。
プロファイル・テスト・ベンチマークは同時に実行せず、最終実装、変更前、Jackson を順次測定しました。
± は JMH の 99.9% 信頼区間の半幅、時間は配列全体の µs/op です。

| 入力 | 件数 | JSONIC 変更前 | JSONIC 変更後 | Jackson 3.2.3 | 割り当て量 B/op（JSONIC 前 → 後） |
|---|---:|---:|---:|---:|---:|
| ascii | 1 | 0.379 ± 0.010 | 0.377 ± 0.017 | 0.474 ± 0.014 | 1264 → 1248 |
| ascii | 100 | 30.440 ± 0.621 | 26.867 ± 0.487 | 25.333 ± 1.758 | 42280 → 36832 |
| japanese | 1 | 0.475 ± 0.027 | 0.449 ± 0.043 | 0.527 ± 0.012 | 1384 → 1368 |
| japanese | 100 | 34.898 ± 1.145 | 31.173 ± 0.481 | 26.708 ± 1.566 | 48400 → 42952 |

100 件で処理時間は ASCII 約 12%・日本語約 11%、割り当て量は約 11～13% 減りました。
平均値では Jackson 3.2.3 と同等になるまで ASCII 約 1.06 倍、日本語約 1.17 倍の高速化が必要です。
ASCII 100 件の信頼区間は重なっており、小さな差の評価には測定のばらつきに注意が必要です。
1 件では引き続き今回の Jackson 3.2.3 の測定より短時間でした。これらは同じキー配置の POJO 配列の
型付きデコードについての結果であり、異なる入力・型・キー順で同じ改善率を保証するものではありません。

キー順・省略・未知キー・重複・別名、setter の副作用、異なるジェネリック型、命名設定、
文字列配列の型変換を検証するテストを追加し、全 96 テストと `mvn verify` が成功しました。
測定 JSON・JFR・検証ログは `target/performance-phase6/` に保存しています。

第七段階では、繰り返し現れるキー名の字句解析を省く処理を追加しました。
配列で次のオブジェクトが現れた後、深さ・キーの位置ごとに、検証済みの引用符付き表記の
入力範囲とデコード済みの名前を記録します。表記全体が同じなら名前を再利用し、
文字列の読み取り・生成・一般キャッシュへの照合を省きます。引用符とエスケープも含めて
範囲全体を比較するため、前方一致だけで別のキーを誤認しません。
異なる表記・キー順は通常の字句解析へ戻ります。

情報は文書内に限定し、各深さで最大 16 個までです。単一要素の配列のためにこのキャッシュを
確保せず、異なる型・文書にまたがる名前の保持も行いません。構文検証・重複キー・エラーの
互換性は維持しています。一般の文字列キャッシュのハッシュ方式は採用版では変更していません。

2026-10-02、OpenJDK 21.0.2、JMH 1.37、Jackson **3.2.3**（Blackbird なし）。
2 forks、ウォームアップ・測定各 5 × 1 秒、256 MiB ヒープ、1 スレッド、GC profiler。
1 件と 100 件は同じ設定で分けて測定し、コンパイルやテストとの同時実行は避けています。
± は JMH の 99.9% 信頼区間の半幅、時間は配列全体の µs/op です。

| 入力 | 件数 | JSONIC 変更前 | JSONIC 変更後 | Jackson 3.2.3 |
|---|---:|---:|---:|---:|
| ascii | 1 | 0.399 ± 0.012 | 0.386 ± 0.005 | 0.480 ± 0.039 |
| ascii | 100 | 27.578 ± 0.403 | 25.719 ± 0.526 | 23.726 ± 0.848 |
| japanese | 1 | 0.455 ± 0.016 | 0.459 ± 0.014 | 0.553 ± 0.012 |
| japanese | 100 | 31.528 ± 0.457 | 27.951 ± 0.271 | 30.274 ± 0.501 |

100 件は ASCII 約 7%・日本語約 11% の時間短縮、1 件は誤差を考慮するとほぼ横ばいです。
今回の測定では日本語 100 件も Jackson 3.2.3 より短時間で、ASCII 100 件は同等まで
平均値であと約 1.08 倍の高速化が必要です。Jackson 自体の測定値も過去の測定から変動しており、
これは上記 POJO 配列と測定条件での比較です。入力やキー配置が異なる場合の優劣を示すものではありません。

繰り返すキーの Unicode エスケープ・前方一致・異なる深さ・16 個を超えるキーを検証する
テストを追加し、全 97 テストと `mvn verify` が成功しました。測定 JSON と検証ログは
`target/performance-phase7/` に保存しています。

## JSONICとは

JSONICは、Java用のシンプルかつ高機能なJSONエンコーダー/デコーダーライブラリです。

Java用のJSONライブラリはすでに多数存在しますが、JSONICはRFC 7159に従った正式なJSON形式でのデコード/エンコードを行いながらも、プログラミング言語に依存する情報をJSON内に含めることなくPOJO(Plain Old Java Object)と自然な変換を行える点に特徴があります。

使い方も非常に簡単です。

```java
import net.arnx.jsonic.JSON;

// POJOをJSONに変換します
String text = JSON.encode(new Hoge());

// JSONをPOJOに変換します
Hoge hoge = JSON.decode(text, Hoge.class);
```

JSONICには、JSON操作APIだけでなく、JSONを使ったWebサービスが簡単に構築できるサーブレットも用意されています。詳しくは[WebサービスAPI](docs/webservice.html)のドキュメントを御覧ください。

<a id="download"></a>

## ダウンロード

公開済みのJSONICは [Maven Central](https://central.sonatype.com/artifact/net.arnx/jsonic)
から取得できます。この作業ツリーのJava 21・Jakarta対応版は、上記の手順でビルドしてください。

<a id="maven"></a>

## リポジトリ

Maven Central Repository から取得できます。

```xml
<dependency>
  <groupId>net.arnx</groupId>
  <artifactId>jsonic</artifactId>
  <version>1.3.10</version>
</dependency>
```

<a id="encoder"></a>

## JSONエンコーダー

POJOからJSONに変換する場合は、encodeを使います。デフォルトでは、空白などを含まない可読性の低いJSONが出力されますが、二番目の引数をtrueにすることで可読性の高いJSONが出力されるようになります（Pretty Printモード）。

なお、JSONのフォーマット中に何らかの例外が発生した場合は、JSONExceptionでラップされ通知されます（Beanからの取得時に例外発生など）。

```java
// 変換対象のPOJOを準備
Hoge hoge = new Hoge();
hoge.number = 10;      // public field
hoge.setString("aaa"); // public property
hoge.setArray(new int[] {1, 2, 3});

// POJOをJSONに変換します。戻り値は {"number":10,"string":"aaa","array":[1,2,3]}となります
String text = JSON.encode(hoge);

// POJOを可読性の高いJSONに変換します。戻り値は次のような文字列になります
// {
//     "number": 10,
//     "string": "aaa",
//     "array": [1, 2, 3]
// }
String text = JSON.encode(hoge, true);

// Appendable(StringBuffer, Writerなど)やOutputStreamを出力先にすることもできます[^1]
JSON.encode(hoge, new FileWriter("hoge.txt"));
JSON.encode(hoge, new FileOutputStream("hoge.txt"));
```

[^1]: OutputStreamを指定した場合に出力される文字コードはUTF-8固定となります。 また、close処理は自動では行われませんので必要に応じて別途行う必要があります。

POJOからJSONへの変換ルールは次の通りです。

| 変換元（Java） | 変換先（JSON） |
| --- | --- |
| Map, DynaBean[^2] | object |
| Object[^3] |
| boolean[], short[], int[], long[], float[], double[], Object[] | array |
| Iterable (Collection, Listなど) |
| Iterator, Enumeration |
| java.sql.Array, java.sql.Struct |
| char[], CharSequence | string |
| char, Character |
| TimeZone, Pattern, File, URL, URI, Path, Type, Member, Charset, UUID, java.timeの各クラス |
| byte[] | string (BASE64エンコード) |
| java.sql.RowId | string (シリアル化後、BASE64エンコード) |
| Locale | string (言語コード-国コードあるいは言語コード-国コード-バリアントコード) |
| InetAddress | string (IPアドレス) |
| byte, short, int, long, float, double | number[^4] |
| Number |
| Date, Calendar | number (1970年からのミリ秒) |
| Enum | string (デフォルトは名前で文字列化。setEnumStyle にて動作の変更が可能)<br> number (setEnumStyle に null を指定すると Enum.ordinal により変換) |
| Optional型 | isPresent() が false を返す時 null、その他の時、保持値 |
| boolean, Boolean | true/false |
| null | null |

[^2]: DynaBeanを利用する場合、Commons BeanUtilsのjarファイルをクラスパスに追加する必要があります。リフレクションを利用して処理を行っているため、利用しない場合は特に含める必要はありません。
[^3]: 対象となるインスタンスをパブリック・getterメソッド、パブリック・フィールドの優先順で探索します。staticが付加されたメソッドやフィールド、transientが付加されたフィールドは対象となりません。
[^4]: NaN, Infinity, -Infinityに限りそれぞれ文字列"NaN", "Infinity", "-Infinity"に変換されます。
また、org.w3c.dom.Document/ElementからJSONへの変換もサポートしています。詳しくは「高度な使い方 - XMLからJSONへの変換」の項をご覧ください。

なお、JSONはobjectかarrayで始まる必要があるため、直接、intやStringのインスタンスをencodeメソッドの引数に指定した場合エラーとなります。

<a id="decoder"></a>

## JSONデコーダー

JSONからPOJOに変換する場合は、decodeを使います。デフォルトでは、object, array, string, number, true/false, nullをHashMap, ArrayList, String, BigDecimal, Boolean, nullに変換しますが、二番目の引数に変換先のクラスを指定することでそのクラスのインスタンスにデータをセットして返してくれます。また、この処理はパブリック・フィールドやパブリック・プロパティ、配列やコレクションのデータを再帰的に辿り実行されますので、一般的なJavaBeansであればencodeして作られたJSONからの逆変換も可能です（Generics型にも対応しています）。

なお、JSON文字列が不正であったり、型の変換に失敗した場合はJSONExceptionが投げられます。

```java
// JSONをPOJOに変換します。戻り値としてサイズが4のArrayListが返されます
List list = (List)JSON.decode("[1, \"a\", {}, false]");

// JSONをHogeクラスのインスタンスに変換します（キャストは不要です）
Hoge hoge = JSON.decode("{\"number\": 10, \"array\": [1, 2, 3]}", Hoge.class);

// クラスの配列型への変換も可能です。
Hoge[] data = JSON.decode("[{ \"id\": 1 }, { \"id\": 2 }, { \"id\": 3 }]", Hoge[].class);

// ReaderやInputStreamからJSONを読み込むことも可能です。[^5]
Hoge hoge = JSON.decode(new FileReader("hoge.txt"), Hoge.class);
Hoge hoge = JSON.decode(new FileInputStream("hoge.txt"), Hoge.class);
```

[^5]: InputStreamから読み込む場合の文字コードは、UTF-8/UTF-16BE/UTF-16LE/UTF-32BE/UTF-32LEから自動判別されます。 また、close処理は自動では行われませんので必要に応じて別途行う必要があります。

JSONからPOJOへの変換ルールは次の通りです。

| 変換元（JSON） | 指定された型 | 変換先（Java） |
| --- | --- | --- |
| object | なし, Object, Map | LinkedHashMap |
| SortedMap | TreeMap |
| その他のMap派生型 | 指定された型 |
| その他の型 | 指定された型（パブリック・フィールド／プロパティに値をセット)[^6] |
| array | なし, Object, Collection, List | ArrayList |
| Set | LinkedHashSet |
| SortedSet | TreeSet |
| その他のCollection派生型 | 指定された型 |
| short[], byte[], int[], long[], float[], double[]<br>Object[]派生型 | 指定された型 |
| Locale | Locale（「言語コード」「国コード」「バリアントコード」からなる配列とみなし変換） |
| Map | インデックスの値をキーとするLinkedHashMap |
| SortedMap | インデックスの値をキーとするTreeMap |
| その他のMap派生型 | インデックスの値をキーとする指定された型のMap |
| string | なし, Object, CharSequence, String | String |
| char | char（幅0の時は'\u0000', 2文字以上の時は1文字目） |
| Character | Character（幅0の時はnull, 2文字以上の時は1文字目） |
| Appendable | StringBuilder |
| その他のAppendable派生型 | 指定された型（値をappend） |
| Enum派生型 | 指定された型（値をEnum.valueOfあるいはint型に変換後Enum.ordinal()で変換） |
| Date派生型,<br>Calendar派生型 | 指定された型（文字列をDateFormatで変換） |
| java.time の各クラス | 指定された型 |
| byte, short, int, long, float, double,<br>Byte, Short, Integer, Long, Float, Double,<br>BigInteger, BigDecimal | 指定された型（文字列を数値とみなし変換） |
| byte[] | byte[]（文字列をBASE64とみなし変換） |
| Locale | Locale（文字列を「言語コード」「国コード」「バリアントコード」が何らかの句読文字で区切られているとみなし変換） |
| Pattern | Pattern（文字列をcompileにより変換） |
| Class, Charset | 指定された型（文字列をforNameにより変換） |
| TimeZone | TimeZone（文字列をTimeZone.getTimeZoneを使い変換） |
| UUID | UUID（文字列をUUID.fromStringで変換） |
| File, URI, URL, Path | 指定された型（文字列をコンストラクタの引数に指定し変換） |
| InetAddress | InetAddress（文字列をInetAddress.getByNameで変換） |
| boolean, Boolean | 指定された型（"", "false", "no", "off", "NaN"の時false、その他の時true） |
| number | なし, Object, Number, BigDecimal | BigDecimal |
| byte, short, int, long, float, double,<br>Byte, Short, Integer, Long, Float, Double,<br>BigInteger | 指定された型 |
| Date派生型,<br>Calendar派生型 | 指定された型（数値を1970年からのミリ秒とみなし変換） |
| boolean, Boolean | 指定された型（0以外の時true、0の時false） |
| Enum派生型 | 指定された型（名前あるいは int値をEnum.ordinal()に従い変換） |
| true/false | なし, Object, Boolean | Boolean |
| char, Character | 指定された型（trueの時'1'、falseの時'0'） |
| float, double, Float, Double | 指定された型（trueの時1.0、falseの時NaN） |
| byte, short, int, long,<br>Byte, Short, Integer, Long,<br>BigInteger | 指定された型（trueの時1、falseの時0） |
| boolean | boolean |
| Enum派生型 | 指定された型（trueを1、falseを0とみなしEnum.ordinal()に従い変換） |
| Optional型 | 保持値（nullの場合 empty()の値が設定されます） |
| null | なし, Object | null |
| byte, short, int, long, float, double | 0 |
| boolean | false |
| char | '\u0000' |

[^6]: 対象となるインスタンスに対しパブリックなsetterメソッド、パブリックなフィールドの優先順で探索します。staticやtransientのメソッド/フィールドは対象となりません。なお、プロパティ名は、単純比較が失敗した場合、LowerCamel記法に変換したものと比較します。

<a id="usage_advanced"></a>

## 高度な使い方

JSONICでは、フレームワークなどでの利用を想定していくつかの便利な機能を用意しています。

<a id="extends"></a>

### 継承による機能拡張

JSONICは、フレームワークでの利用を考慮しインスタンスを生成したり、継承して拡張することができるように設計してあります。 なお、インスタンスを生成して利用する場合は、encode/decodeメソッドの代わりにformat/parseメソッドを利用します。

```java
// インスタンスを生成します
JSON json = new JSON();

// POJOをJSONに変換します(encodeと同じ機能)
String text = json.format(new Hoge());

// JSONをPOJOに変換します(decodeと同じ機能)
Map map = (Map)json.parse(text);

// JSONをHogeクラスのインスタンスに変換します(decodeと同じ機能)
Hoge hoge = json.parse(text, Hoge.class);
```

DIコンテナなどを使いインスタンスを生成したり、独自の変換を追加するために次のようなオーバーライド可能なメソッドが用意されています。

```java
JSON json = new JSON() {

  // フォーマット可能なクラスに変換します（formatでのみ有効です）。
  // 例外が発生した場合、JSONExceptionでラップされ呼び出し元に通知されます。
  protected Object preformat(Context context, Object value) throws Exception {
    // java.awt.geom.Point2DをJSON arrayにフォーマットする例です。
    if (value instanceof Point2D) {
      Point2D p = (Point2D)value;
      List<Double> list = new ArrayList<Double>();
      list.add(p.getX());
      list.add(p.getY());
      return list;
    }
    return super.preformat(context, value);
  }

  // 解析されたデータを指定したクラスに変換します（parseでのみ有効です）。
  // 例外が発生した場合、JSONExceptionでラップされ呼び出し元に通知されます。
  // さら下の階層を変換したい場合は、context.convert(キー, 値, 型)を呼び出してください。
  protected <T> T postparse(Context context, Object value,
    Class<? extends T> c, Type t) throws Exception {

    // JSON arrayをjava.awt.geom.Point2Dに変換する例です。
    if (Point2D.class.isAssignableFrom(c) && value instanceof List) {
      List list = (List)value;
      Point2D p = (Point2D)create(context, c);;
      p.setLocation(
        context.convert(0, list.get(0), double.class),
        context.convert(1, list.get(1), double.class)
      );
      return c.cast(p);
    }
    return super.postparse(context, value, c, t);
  }

  // 型cに対するインスタンスを生成します（parseでのみ有効です）。
  protected <T> T create(Context context, Class<? extends T> c) throws Exception {
    if (Point2D.class.isAssignableFrom(c)) {
      return c.cast(new Point2D.Double());
    }
    return super.create(context, c);
  }

  // Class cにおいて、Member mを無視します（parse/formatの両方で有効です）。
  protected boolean ignore(Context context, Class c, Member m) {
    // デフォルトでは、static/transparentのメンバおよびObjectクラスで宣言された
    // メンバの場合、trueを返します。
    return super.ignore(context, c, m);
  }
};
```

また、継承して作成した自作クラスをJSON.prototypeにセットすることで、JSON.encodeやJSON.decodeの動作を置き換えることも可能です。

```java
JSON.prototype = MyJSON.class;
```

<a id="generics"></a>

### 総称型を指定してのdecode/parse

decodeやparseの引数にはJava 5.0で追加された総称型も指定できます。しかし、総称型はコンパイル時に削除されてしまうため、decode/parseメソッドの引数として直接的に指定することができません。総称型を使う場合は TypeReference を使って型を埋めこむか、ルート要素をJSON objectにして対応するクラス定義の中で総称型を使います。

```java
class Config() {
    // TypeReference を使う
    public static List<RowData> load(Reader reader) throws IOException {
        return JSON.decode(reader, new TypeReference<List<RowData>>() {});
    }

    // 型を定義してその中で総称型を利用する
    public static Config load(Reader reader) throws IOException {
        return JSON.decode(reader, Config.class);
    }

    public static class RowData {
        public String id;
        public String name;
    }

    public List<RowData> rows;
}
```

多少トリッキーですが、FieldやMethodや無名クラスからリフレクションで総称型を取得することで間接的に指定する方法もあります。

```java
    private Map<String, Hoge> config;

    // Filedを使って総称型を指定
    public Map<String, Hoge> load(Reader reader) throws IOException {
        return JSON.decode(reader,
            this.getClass().getField("config").getGenericType());
    }

    // 総称型を継承した無名クラスを使って総称型を指定
    public List<RowData> load(Reader reader) throws IOException {
	    return JSON.decode("[ { ... }, { ... } ]",
	        (new ArrayList<RowData>() {}).getClass().getGenericSuperclass());
    }
```

<a id="prettyprinting"></a>

### 可読性の高い出力 - Pretty Print モード

JSONICでは、encode の第二引数に true を渡すか、setPrettyPrint() メソッドを使うことでインデントや改行などが付いた可動性の高いJSONを出力することができます。

```java
    // encode の第二引数に true を設定
    JSON.encode(obj, true);

    // setPrettyPrint に true を設定
    JSON json = new JSON();
    json.setPrettyPrint(true);
    json.format(obj);
```

デフォルトでは、初期インデントがなく、タブを使用してインデントを出力しますが、setInitialIndent() メソッドや setIndentText() メソッドを使うことでインデントの書式を変更することができます（これらの設定は、setPrettyPrint に true を設定した場合のみ有効となります）。

```java
    // 初期インデントを 1、インデントとして空白4文字を使用
    JSON json = new JSON();
    json.setPrettyPrint(true);
    json.setInitialIndent(1);
    json.setIndentText("    ");
    json.format(obj);
```

<a id="liberalparsing"></a>

### 柔軟な読み込み - TRADITIONALモード

JSONICはポステルの法則（送信するものに関しては厳密に、受信するものに関しては寛容に）に従い、デフォルトでは、妥当でないJSONであっても読み込みが可能なTRADITIONALモードで動作するように作成されています。
RFC 4627に規定された内容との相違点は以下の通りです。

- [デコード] Cスタイルの複数行コメント（/**/）、C++スタイルの行コメント（//）をコメントとして認識します。
- [デコード] ルート要素がobjectの場合、一番外側の'{'と'}'を省略することができます（入力文字列が空白文字列やコメントのみの場合も空のobjectとみなされます）。
- [デコード] シングルクォートで囲まれた文字列やJavaリテラルを文字列として認識します。
- [デコード] objectやarrayにおいて各要素が改行で区切られているとき','を省略することができます。
- [デコード] objectにおいてキーに対する値がobjectの場合、':'を省略することができます。
- [デコード] string中で改行やタブなどの制御文字を有効な文字として認識します。
- [デコード] objectやarrayにおいて値が省略された場合、nullとして認識します。

例えば、次のテキストはRFC 4627では無効ですが、JSONICでは読み込むことが可能です。

```json
// database settings
database {
  description: 'ms sql server
	connecter settings'
  user: sa
  password: xxxx // you need to replace your password.
}

/*
  equals to {"database": {
     "description": "ms sql server\n\tconnecter settings",
     "user": "sa", "password": "xxxx"}}
*/
```

この動作はsetMode(Mode.STRICT)を指定することで、RFCに準じた妥当性チェックを行なうよう変更することができます。

<a id="validation"></a>

### JSONの検証 - STRICTモード

JSONICでは、従来柔軟な読み込みができる反面、RFC 4627に厳密に沿ったJSONであるか判定することができませんでした。 JSONIC 1.2.1からはSTRICTモードが用意され、厳密な検証動作が可能となりました。

モードを変更する場合は、JSONインスタンスのコンストラクタに設定するか、setModeメソッドを呼ぶか、JSON.prototypeにModeを変更したクラスを設定します。

```java
  JSON json = new JSON(JSON.Mode.STRICT);
  
  json.setMode(JSON.Mode.STRICT);
  
  JSON.prototype = (new JSON() {
    {
      setMode(JSON.Mode.STRICT);
    }
  }).getClass();
```

また、データのデコードを行わず検証のみを行うvalidateメソッドも用意されています（これは、setDepth(0)、setMode(Mode.STRICT)を指定した時と同じです）。

```java
  JSON.validate(new FileInputStream("test.json"));
```

<a id="jsfriendly"></a>

### JavaScriptに親和的な出力 - SCRIPTモード

JSONは、可搬性あるデータ連携フォーマットとしてだけでなく、HTML内に書かれるJavaScript内にJavaオブジェクトの内容をインライン出力するためにも便利です。 JSONICでは、このような場合に使いやすいようSCRIPTモードを用意しています。
RFC 4627に規定された内容との相違点は以下の通りです。

- [エンコード] HTMLやXML中ではエスケープが必要な「<」「>」が文字列中に見つかった場合、それぞれ「\u003C」「\u003E」と出力します。
- [エンコード] java.util.Date 型を new Date(ミリ秒) で出力します。
- [エンコード] NaN、POSITIVE_INFINITY, NEGATIVE_INFINITY を文字列ではなく、Number.NaN、Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITYとして出力します。
- [デコード] 引数にstring、number、true/false/nullといったJSONの断片を指定し単純型の値を取得することができます。
- [デコード] Cスタイルの複数行コメント（/**/）、C++スタイルの行コメント（//）をコメントとして認識します（TRADITIONALモードと異なり#はコメントとして認識しません）。
- [デコード] シングルクォートで囲まれた文字列をstringとして認識します。
- [デコード] object のキーに限りシングルクォートで囲まれていないリテラルを文字列として認識します。

モードを変更する場合は、JSONインスタンスのコンストラクタに設定するか、setModeメソッドを呼ぶか、JSON.prototypeにModeを変更したクラスを設定します。また、1.2.6からは、JSON.escapeScript を通じて簡単に使うことが可能です。

```java
  JSON json = new JSON(JSON.Mode.SCRIPT);
  
  json.setMode(JSON.Mode.SCRIPT);
  
  JSON.prototype = (new JSON() {
    {
      setMode(JSON.Mode.SCRIPT);
    }
  }).getClass();
  
  JSON.escapeScript(...);
```

<a id="reader"></a>

### JSONストリームの順次出力 - JSONWriter

JSONICのencode/format は、Javaオブジェクトを構築後、JSONに出力するため、大規模な JSON を出力する場合、 メモリを大量に消費してしまう問題がありました。 version 1.3.1 では、この問題に対応するため、JSONの encode とStringBuffer 的な順次出力を組み合わせた JSONWriter クラスを提供します。

```java
    // JSONWriter を取得
    Writer out = new OutputStreamWriter(new FileOutputStream(...));
    JSONWriter writer = new JSON().getWriter(out);
    // JSON object の出力
    writer.beginObject();

    // 名前と値の出力
    writer.name("string").value("value");

    // 値の出力は、decode と同様に任意のオブジェクトを指定可能です。
    Object o = new Object() {
        public DataType type = DataType.ANY;
        public String text = "あああ";
        public int index = 100;
    };
    writer.name("object").value(o);

    // JSON array の出力
    writer.name("array");
    writer.beginArray();
    writer.value(1);
    writer.value(2);
    writer.value(3);
    writer.endArray();

    writer.endObject();

    // JSONの完成前に途中で出力する場合は、flushを呼ぶ必要があります（通常は不要です）。
    writer.flush();

    // クローズは自動では行われません。明示的にクローズ処理を行ってください。
    out.close();
```

### JSONストリームの順次読み込み - JSONReader

JSONICのdecode/parse は、通常 XMLでの DOM(Document Object Model)に当たる API となっており、JSON を読み取りJavaオブジェクトモデルを構築しますが、この方式で大規模なJSONファイルを扱うと、メモリを大量に消費してしまい OutOfMemoryError が発生してしまいます。

version 1.3 では、この問題に対応するために、StAX(Streaming API for XML) に相当する JSONReader クラスを提供しています。JSONReader は、readerメソッドを介して取得します（reader メソッドの ignoreWhitespace を false にすることで、コメントや空白も取得できます）。

```java
    // JSONReader を取得
    JSONReader reader = new JSON().getReader("[1, 2, 3, 4, 5]");

    JSONEventType type;
    // next で次のトークンを読み取り
    while ((type = reader.next()) != null) {
        switch (type) {
        case START_OBJECT:
            System.out.println("{");
            break;
        case END_OBJECT:
            System.out.println("}");
            break;
        case START_ARRAY:
            System.out.println("[");
            break;
        case END_ARRAY:
            System.out.println("]");
            break;
        case NAME:
            System.out.print(reader.getString() + ": ");
            break;
        case STRING:
            System.out.println(reader.getString());
            break;
        case NUMBER:
            System.out.println(reader.getNumber());
            break;
        case BOOLEAN:
            System.out.println(reader.getBoolean());
            break;
        case NULL:
            System.out.println("null");
            break;
        }
    }

    // ignoreWhitespace を false にするとコメントやスペースも取得可能
    JSONReader reader = new JSON().getReader("[1, 2, 3, 4, 5]", false);
    while ((type = reader.next()) != null) {
        switch (type) {
        case WHITESPACE:
            System.out.println(reader.getString());
            break;
        case COMMENT:
            System.out.println(reader.getString());
            break;
        }
    }
```

JSONReader には、単独の値を取得するだけでなく、現在位置以下のツリーをひとかたまりで取得し Java オブジェクトに変換する getValue メソッドも用意されています。

```java
    // オブジェクトの配列を処理する
    JSONReader reader = new JSON().getReader("[{...}, {...}, {...}, {...}, {...}]");

    List<FooBean> list = new ArrayList<FooBean>();

    JSONEventType type;
    while ((type = reader.next()) != null) {
        if (type == JSONEventType.START_OBJECT) {
            // 現在位置のオブジェクトを取得して FooBean に変換
            list.add(reader.getValue(FooBean.class));
        }
    }
```

JSONReader の JSON 解釈は、設定された JSON.Mode に準じます[^7]が、複数の連続した JSON も処理できるようになっていますので、Twitter API で返されるような、改行で区切られた JSON Streaming を扱うことができます。

[^7]: TRADITIONAL モードでは、ルート要素がobjectの場合、一番外側の'{'と'}'を省略することができますが、JSONReader を使用する場合、連続したJSONの解釈と競合するため省略できません。

```java
    // オブジェクトの配列を処理する
    JSONReader reader = new JSON().getReader("{...}\n{...}\n{...}\n{...}\n{...}");

    JSONEventType type;
    while ((type = reader.next()) != null) {
        if (type == JSONEventType.START_OBJECT) {
            // 現在位置のオブジェクトを取得して Tweet に変換
            System.out.println(reader.getValue(Tweet.class));
        }
    }
```

<a id="format"></a>

### 日時/数値書式の指定 - setDateFormat/setNumberFormat

日付型や数値型は、デフォルトではJSON numberとして出力されますが、JSONIC 1.2.8以降ではsetDateFormat/setNumberFormat を指定することでデフォルトの日時/数値書式を設定できます。フォーマットの書式は Number型の場合 java.text.DecimalFormat、Date型の場合 java.text.SimpleDateFormat[^8]、 Java8 Date/Time API の場合 java.time.format.DateTimeFormatter に従ってフォーマットされます。書式は JSONHint を使うことで上書きすることができます。

[^8]: 書式フォーマットは原則SimpleDateFormatと同じですが、ISO8601形式のタイムゾーンを出力するZZもサポートしています。

```java
	JSON json = new JSON();
	// デフォルトの日時書式を指定
	json.setDateFormat("yyyy/MM/dd");

	// デフォルトの数値書式を指定
	json.setNumberFormat("###,##0.00");

	// 戻り値は { "date": "2011/01/01", "number": "1,000.00" ] となります
	json.format(new Object() {
		public Date date = new Date(2011, 0, 1);
		public int number = 1000;
	});
```

<a id="namingstyle"></a>

### プロパティ名/列挙型出力書式の指定 - setPropertyStyle/setEnumStyle

プロパティ名は、デフォルトではプロパティ名をJSON stringとして、列挙型は序数をJSON numberとして出力しますが、JSONIC 1.2.8以降ではsetPropertyStyle/setEnumStyleを使用することで出力書式を設定できます。

```java
    JSON json = new JSON();
    // プロパティ名を、アッパーキャメル記法に変換して出力
    json.setPropertyStyle(NamingStyle.UPPER_UNDERSCORE);
    
    // 列挙値を、小文字アンダースコア区切りに変換して出力
    json.setEnumStyle(NamingStyle.LOWER_CAMEL);
    
    // 戻り値は { "JSON_MODE": "halfEven" } となります
    json.format(new Object() {
        public RoundingMode jsonMode = RoundingMode.HALF_EVEN;
    });
```

<a id="innerclass"></a>

### 内部クラスを利用したエンコード/デコード

JSONの設定ファイルを解析したいような場合は、内部クラスやパッケージ・デフォルトのクラスを利用したいことがあります。

JSONICでは、encode/decode/parse/formatの引数に指定されたクラスと同一パッケージの内部クラスや無名クラスを自動的にアクセス可能に変更します。

ただし、この場合に生成された内部クラスのインスタンスには包含するクラスのインスタンスがセットされていない状態になります。内部クラスから包含するクラスのインスタンスにアクセスしたい場合や引数に指定したクラス以外のコンテキストで実行したい場合は、setContextを利用して明示的に指定してください。

```java
public class EnclosingClass {
  public void decode() {
    JSON json = new JSON();
    InnerClass ic = json.parse("{\"a\": 100}", InnerClass.class); // このクラスのコンテキストで動作

    System.out.println("ic.a = " + ic.a); // ic.a = 100

    ic.accessEnclosingClass(); // 実行時にNullPointerExceptionが発生

    json.setContext(this);  // コンテキストを設定
    ic = json.parse("{\"a\": 100}", InnerClass.class);

    ic.accessEnclosingClass(); // 正常に動作
  }

  class InnerClass {
    public int a = 0;

    public void accessEnclosingClass() {
      decode();
    }
  }
}
```

<a id="maxdepth"></a>

### setMaxDepth - 最大深度の設定

JSONICは、encode/format時に自分自身を戻すようなフィールドやプロパティ、配列を無視することで再帰による無限ループが発生することを防ぎます。 しかし、そのインスタンスにとって孫に当たるクラスが自分のインスタンスを返す場合にも再帰が発生してしまいます。JSONICでは、このような場合へ対処するため 単純に入れ子の深さに制限を設けています。

なお、最大深度の設定はdecode/parse時にも有効ですので深すぎるデータの取得を避けることも可能となります。

この最大深度は、デフォルトでは32に設定されていますが変更することも可能です。

```java
// 5階層以下の情報は取得しない
json.setMaxDepth(5);
```

<a id="suppressnull"></a>

### setSuppressNull - null値の抑制

JSONICでは、format時に値がnullになっているJSON objectのメンバの出力を抑制できます。初期値はfalseです。余計なメンバが大量に出力されてしまう、プロパティの初期値を優先したいなどの場合に有効です。

```java
// null値の出力を抑制します。
json.setSuppressNull(true);
```

なお、Version 1.2 系では、parse 時や Map の format に対しても null 値が抑制されていましたが、不適切な場合が多いため1.3系では抑制しないよう変更されました。

<a id="xmltojson"></a>

### XMLからJSONへの変換

JSONICでは、org.w3c.dom.Document/ElementからJsonMLへの変換をサポートしています。 方法は、通常と同じようにencode/formatの引数にorg.w3c.dom.Document/Elementのインスタンスを設定するだけです。

```java
Document doc = builder.parse(new File("sample.xml"));
String xmljson = JSON.encode(doc);
```

例えば、下記のXMLの場合

```xml
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Feed Title</title>
  <entry>
    <title>Entry Title</title>
  </entry>
</feed>
```

次のようなJSONが生成されます（実際にはタグ間の空白文字もTextNodeとして出力されます。不要な場合は、DOM作成時に取り除く必要があります）。

```json
["feed", {"xmlns": "http://www.w3.org/2005/Atom"},
	["title", "Feed Title"],
	["entry",
		["title", "Entry Title"],
	]
]
```

<a id="jsonhint"></a>

### JSONHintアノテーション - 変換時ヒントの付加

場合によってデフォルトの変換方式では不十分な場合があります。JSONICでは、メソッドやフィールドにJSONHintアノテーションを付加することで、 動作を部分的に制御することが可能です。

設定できる属性は次の通りです。

| 属性名 | 値型 | 説明 |
| --- | --- | --- |
| name | String | 出力/代入するキー名を変更します |
| format | String | 対象の型がNumberあるいはDate型の場合は、指定したフォーマットに従って変換します。<br> フォーマットの書式はそれぞれjava.text.DecimalFormat、java.text.SimpleDateFormatを参照してください[^9]。 |
| type | Class | parse時に指定した型のインスタンスを生成します（対象の型のサブクラスを指定する必要があります）。 |
| ignore | boolean | 出力/代入対象から除外します |
| serialized | boolean | 値がJSONであるものとして扱います。デフォルトはfalseです。 Format時はtoString()の値をそのまま出力[^10] 、Parse時は入力されたJSONをJava Objectに変換し再度formatした文字列が設定されます。 |
| anonym | String | 単純値型からMapや複合型に変換するときに単純値型を設定するプロパティ名を指定します。anonymを指定しない場合、Mapの場合はnullキーの値として設定されますが、複合型を指定した場合はエラーとなります。 |
| ordinal | int | JSON objectへの変換する際のキーの出力順を昇順で指定します。デフォルトはキー値の自然順序順（＝負値指定）です。 |

[^9]: 書式フォーマットは原則SimpleDateFormatと同じですが、ISO8601形式のタイムゾーンを出力するZZもサポートしています。
[^10]: 出力される文字列は検証されないため妥当でないJSONが出力されてしまう可能性があることに注意してください。逆に言えば、この機能を使うことでコメントやfunction呼び出しを出力することも可能です。

```java
public class WithHintBean {
  // format/parse時のキー値を変更
  @JSONHint(name="名前")
  public int keyValue = 100;

  // format/parse時のフォーマットを指定
  @JSONHint(format="yyyy/MM/dd")
  public Date dateValue = new Date();

  // 数値の時は、DecimalForamtとして認識される
  @JSONHint(format="##0.00")
  public int numberValue = 100;

  // 配列やリストでもOK
  @JSONHint(format="yyyy/MM/dd")
  public List<Date> dateArray;

  // メソッドにも付与可能（getter/setterで別のヒントを与えることも可）
  @JSONHint(format="yyyy/MM/dd")
  public int getMethodValue() {
    return 100;
  }

  // ArrayListの代わりにLinkedListのインスタンスを生成
  @JSONHint(type=LinkedList.class)
  public List<String> stringList;

  // format/parse時に無視
  @JSONHint(ignore=true)
  public int ignoreValue = 100;

  // 値はJSON
  @JSONHint(serialized=true)
  public String json = "{\"num\": 100, \"func\": sum(100, 200) /*illegal JSON*/}";
}
```

<a id="tostring"></a>

### JSONHintによるString指定 - データの文字列化

JSONHintアノテーションのtype属性にStringを指定することで、データをtoString()およびString型を引数にとるコンストラクタを取る文字列相当型として扱うことができるようになります。

```java
public class TestBean {
  @JSONHint(type=String.class)
  public StringBean sb;
}

public class StringBean {
  // decode時は、String型を引数に取るコンストラクタが呼ばれます
  public StringBean(String str) {
    ...
  }

  // encode時は、toStringが呼ばれます
  public String toString() {
    ...
  }
}
```

<a id="serializable"></a>

### JSONHintによるSerializable指定の廃止

`@JSONHint(type=Serializable.class)` によるJavaオブジェクトのシリアル化・復元は廃止しました。
この指定があるプロパティを処理するとエラーになります。アノテーションを削除し、
JSONで表現できるBeanなどへ移行してください。既存のBase64形式のJavaシリアル化データは復元できません。
`byte[]`のBase64変換と、JSON文字列を扱う`@JSONHint(serialized=true)`は引き続き利用できます。

<a id="faq"></a>

## FAQ

- Q. RESTServletでHTTP GETを使うと日本語が文字化けします
- A. 入力文字エンコーディングの問題です。特にApache Tomcat5以降は仕様を厳密に解釈した結果、GETがsetCharacterEncodingを無視するという問題がありますのでuseBodyEncodingForURIを設定し回避する必要があります（JSONIC 1.1 ではGETパラメータを独自に解析していたため、この問題は発生していませんでした）。

- Q. 大量データを読み込むとOutOfMemoryErrorで落ちます。
- A. JSON.decode() や JSON.parse() は、JSON文字列をオブジェクトツリーとして生成するため、大量のデータを取り扱うとメモリを大量に消費してしまいます。そのような場合には、JSON.getReader() メソッドを使うことでデータをひとつずつ読み込み処理することができます。

- Q. Resin サーバで RESTServletやRPCServletが動作しません。
- A. Resin サーバでは、web.xml中にある ${...} を変数として扱うため、\${...} と書かないといけないようです。

<a id="license"></a>

## ライセンス

JSONICは、Apache License, Version 2.0下で配布します。

自分のライブラリへの組み込んでいただいたり、その際にパッケージ名の変更や処理の変更など行っていただいて構いません。保障はありませんが、ライセンスの範囲内でご自由にお使いください。

<a id="report"></a>

## バグ・要望の報告先

バグや要望などは[JSONICプロジェクトサイト](http://osdn.jp/projects/jsonic)の[チケット](http://osdn.jp/projects/jsonic/ticket/)に報告ください。

<a id="releasenote"></a>

## リリースノート

### 2015/11/2 version 1.3.10

- [不具合修正] JSONWriter にて配列中のオブジェクトや配列の後ろのカンマが出力されない問題を修正しました。

- [不具合修正] JSON WebService にて例外発生時に Exception のプロパティに JSONHint が適用されない問題を ignore と name についてのみ適用されるよう修正しました。

- [機能追加] JSONReader にて値の読み取りをスキップしてメモリを節約できる skipValue() メソッドを追加しました。

- [機能追加] JSONWriter にて値をそのまま出力できる append(String text) メソッドを追加しました。

### 2015/8/20 version 1.3.9

- [不具合修正] Java8 Date/Time API に JSONHint の format が正しく反映されない問題を修正しました[チケット:#35349]

- [機能追加] Java7 の java.nio.Path 型に対応しました。

### 2015/6/29 version 1.3.8

- [仕様変更] コンパイル可能な環境の構築が難しくなってきたため、Java 5 のサポートを廃止しました。Java 6 以降をご利用ください。

- [不具合修正] パラメータを持つ総称型のプロパティの decode/parse に対応しました[チケット:#35153]

- [機能追加] Java8 の Optional 型（OptionalInt、OptionalLong、OptionalDouble、Optional）に対応しました。

### 2014/12/23 version 1.3.7

- [不具合修正] JSONHint に type を指定しても、type のプロパティに値が設定されない問題を修正しました。

- [不具合修正] JSON object に PrittyPrint モードで encode/format する際、閉じ括弧のインデントがずれる問題を修正しました。

- [機能追加] null に対して preformat は動作しない問題に対応するため preformatNull メソッドを追加しました。

### 2014/10/26 version 1.3.6

- [仕様変更] RFC 7159 の発行に伴い、文字列、数値、true/false/null をルート要素として許容するよう変更しました。

- [機能追加] Java8 Date/Time API(JSR 310) に対応しました。

- [改善] JSONICをリパッケージした際、メッセージの取得に失敗する問題を修正しました。

### 2014/5/25 version 1.3.5

- [不具合修正] Android で JSONIC を起動するとき Commons BeanUtils がクラスパスにないとエラーが発生して起動できない問題を修正しました（1.3.1以降）

### 2014/4/29 version 1.3.4

- [仕様変更] JSONIC でも BeanUtils 同様の仕組みを持っているため、
  [Struts1 の ClassLoader 脆弱性](http://www.nca.gr.jp/2014/struts_s20/index.html)
  が発生する懸念があり調査いたしましたが、次の理由から JSONIC には影響しないことが確認できました（この仕様は JSONIC 全バージョンで同一です）。

  - JSON#ignore や Container#limit メソッド内で java.lang.Object クラスで定義されたフィールド／メソッドは無視されるようになっている。

  - convert時の動作では、setterしか利用しないため、getClass() が呼びだされることがない。

  しかしながら、今後同様の問題が発生する可能性を少なくし安全性を高めるため、Bean 情報取得の時点で以下の制限を行なうよう修正を実施しました。

  **java.lang.Object クラスで定義された getter/setter はプロパティとして認識しない（メソッドとしては認識する）。**

  この制限により Object#getClass() がプロパティとして呼び出されること自体がなくなります。

  **java.lang.Class のプロパティを不可視にする。**

  この制限により、開発者が明示的にjava.lang.Classを返すプロパティを定義した場合でも、Class#getClassLoader() などシステムの内部情報にアクセスされることがなくなります。

### 2014/3/16 version 1.3.3

- [不具合修正] JSONIC をロードしたクラスローダがコンテキストクラスローダの親に存在しない場合、初期化に失敗する問題を修正しました（1.3.1～1.3.2で発生）。

### 2014/2/24 version 1.3.2

- [不具合修正] 列挙型にて定数ごとに継承を行なうと encode/decode に失敗する問題を修正しました。

- [不具合修正] encode/format に OutputStream や BufferedWriter を引き渡すと flush されない問題を修正しました(1.3.1 でのみ発生)

- [不具合修正] parse 時に markSupported が false を返す InputStream を指定すると IOException が発生していた問題を修正しました（1.3.1 でのみ発生）

### 2014/2/13 version 1.3.1

- [仕様変更] setaName、isaName、getaName など1文字目が小文字となるようなプロパティに対応しました。JavaBeans規約では、set/is/getで始まるメソッドはプロパティとしてみなすことになっているため、本来はそのように修正すべきですが、影響範囲が広くなる恐れがあるため部分的な対応に留めることにしました。

- [機能追加] 総称型の解決を改善しました。これにより、複雑な関係にある型変数にも対応できるようになりました。

- [機能追加] type=String.class を指定した場合、 Enum の decode に失敗する問題を改善しました。

- [機能追加] ストリーム的に JSON を出力する JSONWriter を追加しました。

- Object の encode など一部の処理が高速化されました。

### 2012/8/4 version 1.3.0

- [機能追加] JSON のストリーム的に読み取るプルパーサ API である JSONReader を追加しました。JSONReader は、 JSON#getReader() メソッドを使うことで取得できます。

- [機能追加] decode/parseが新たに追加されたJSONReaderベースに書きなおされ、また、速度も大幅に改善しました。

- [機能追加] 総称型を埋め込める TypeReference を追加しました。

- [機能追加] Web Service API にて処理に使用する JSON クラスのプロパティ値をコンフィグから指定できるようになりました。

- [機能追加] NamingStyle に何もしない NOOP を追加しました。また、EnumStyle のデフォルトスタイルが NamingStyle.NOOP に変更されました（1.2まではインデックス値に変換していました）。

- [機能追加] 初期インデント幅を設定する setInitialIndent()、インデントとして使用する文字列を指定する setIndentText() を追加しました。

- [機能追加] getReader()でJSONReaderを取得した場合は、連続したJSONをシーケンシャルに扱えるよう拡張しました。TwitterのJSONストリーミングのように連続したJSONが直接扱えるようになりました。

- [機能追加] Container クラスに例外処理を受け取れる exception メソッドを追加しました（#28806）

- [仕様変更] Web Service API にて debug: true が指定された場合、PrettyPrint が自動的に有効になっていましたが、1.3では明示的に指定する必要があります。

- [仕様変更] setSuppressNull を指定すると parse 時や Map の format 時も null を無視していましたが不適切な場合が多いため、JavaBean あるいは DynaBean の format 時のみ有効となるよう変更しました。

- [仕様変更] parse/decode 時は formatの指定に関わらず日時文字列からDate型へ書式の自動解析による変換を行なっていましたが、formatが指定された場合は書式に従った解析を行なうよう変更しました。

- [仕様変更] TRADITIONAL モードでも、値が常に文字列型に変換されるよう仕様を変更しました（ただし、SCRIPTモードと異なり、マイナスの値も指定可能です。また、nullは文字列ではなく従来通りnull値に変換されます）。

- [仕様変更] ReaderあるいはInputStreamの先頭以外でBOM（Byte Order Mark）が見つかった場合は、例外を出すように変更しました。

- [仕様変更] TRADITIONAL モードでサポートされていたシェルスクリプトスタイルの行コメント（#) を廃止しました。

- [仕様変更] TRADITIONAL モードでサポートされていたシングルクォートで囲まれた文字列の場合、シェルスクリプトのようにエスケープを無視する仕様にしていましたが、誤解する人が多数いたため廃止しました。

- [仕様変更] TRADITIONAL モードでもSCRIPTモードと同様に<、>を\u003C、\u003Eにエスケープするように変更しました。

- [仕様変更] SCRIPT モードで JSON Object のキー値としてとれる値を JavaScript の仕様に合わせ、マイナスの数値の場合エラーとし、また値が常に文字列型に変換されます。

- [仕様変更] メソッド名が不統一となっていたため JSON.Context#getLevel() を非推奨とし、 JSON.Context#getDepth() に変更しました。

- [仕様変更] メソッド名が不統一となっていたため JSON.Context#getPropertyCaseStyle(), JSON.Context#getEnumCaseStyle() を廃止し、それぞれ JSON.Context#getPropertyStyle(), JSON.Context#getEnumStyle() に変更しました。
