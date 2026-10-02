# WebサービスAPI (JSONIC 1.4)

[JSONIC](../README.md)

## 目次

- [基本的な説明](#webservice)
- [RPCサーブレット](#rpcservlet)
- [RESTサーブレット](#restservlet)
- [DIコンテナ対応](#dicontainer)
- [Gatewayフィルタ](#gatewayfilter)

<a id="webservice"></a>

## 基本的な説明

JSONICには、JSONを使ったWebサービスが簡単に構築できるサーブレットが二種類用意されています。

| サーブレット | 説明 |
| --- | --- |
| RPCServlet | [JSON-RPC](http://json-rpc.org) [1.0](http://json-rpc.org/wiki/specification)/[2.0](http://groups.google.com/group/json-rpc/web/json-rpc-2-0) 仕様に対応したRPC(Remote Procedure Call)サービスを構築できます。 |
| RESTServlet | GET/POST/PUT/DELETEなどHTTP Methodをベースに操作を行うRESTfullなWebサービスを構築できます。 |

<a id="rpcservlet"></a>

## RPCサーブレット

RPCサーブレットは、[JSON-RPC 1.0](http://json-rpc.org/wiki/specification) および [JSON-RPC 2.0](http://groups.google.com/group/json-rpc/web/json-rpc-2-0) の両方をサポートしたWEBサービス構築用サーブレットです。

### RPCサーブレットの概要

RPCサーブレットを使うと、指定したパスに対しJSONをPOSTすることで、対象クラスのメソッドを呼び出すことができます（GET/PUT/DELETEは無効です）。paramsに指定された配列の値はメソッドの引数に指定された型に従い自動的に変換されます。なお、クラス名はUpperCamel、メソッド名はLowerCamelに自動的に変換されます。そして、実行後、戻り値がJSONに変換されクライアントに返されます。

```http
POST /rpc.json HTTP/1.0
...
Content-Type: application/json

{ "method": "class.method", "params": [ arg1, arg2, ... ], "id": request_id }
```

class, methodにはそれぞれ変数の値、argNにはメソッドの引数を設定してください。requesst_idには送受信の同期確認用のキーとしてnull以外の任意の値を設定してください（HTTPでは、送信と受信は同期処理ですのでほとんど意味はありませんが、省略すると通知(Notification)モードとなりレスポンスのメッセージボディが返されませんので必ず値を指定してください）。

例えば、mappingsに "/{package}/{class}.{ext}": "boo.${package}.${class}Service" という指定があった場合、 /foo/woo/rpc.jsonというパスに次のJSONがPOSTすると、boo.foo.woo.CalcServiceクラスのplusメソッドが呼び出されます。

```http
POST /foo/woo/rpc.json HTTP/1.0
...
Content-Type: application/json

{ "method": "calc.plus", "params": [1,2], "id": 1 }
```

boo.foo.woo.CalcServiceは次のように実装されていたとします。

```java
package boo.foo.woo;

public class CalcService {
    public int plus(int a, int b) {
        return a + b;
    }
}
```

この時、レスポンスのメッセージボディとしては次のような結果が返されます。

```http
HTTP/1.0 200 OK
...
Content-Type: application/json

{ "result": 3, "error": null, "id": 1 }
```

各パラメータの意味は次の通りです。

| 方向 | パラメータ | 説明 |
| --- | --- | --- |
| リクエスト | jsonrpc | JSON-RPC 2.0で接続する時のみ"2.0"を指定します。1.0で接続する時は指定しません。 |
| リクエスト | method | メソッド名を指定します。パス変数で`class`が指定されている場合はメソッド名を、指定していない場合はクラス名とメソッド名を「`class.method`」の形式で指定する必要があります。なお、クラス名はパッケージを含んだ完全名ではなく、後述で設定するマッピングに対応した名前を指定してください。 |
| リクエスト | params | パラメータを指定します。JSON arrayを指定した場合はメソッドの引数に指定された型に変換され引渡されます。JSON-RPC 2.0ではJSON objectを指定できますが、その場合は、第一引数に指定された型に変換され引渡されます。 |
| リクエスト | id | 設定するとレスポンスのid値として同じ値が戻されます。なお、JSON-RPC 1.0ではidがnullの時、2.0ではidを省略すると通知(Notification)モードとなりレスポンスのメッセージボディが返されませんので必ず値を指定してください。 |
| レスポンス | jsonrpc | JSON-RPC 2.0で接続した時のみ"2.0"が返されます。1.0では設定されません。 |
| レスポンス | result | 成功した時には、結果が設定されます。JSON-RPC 2.0では成功した時のみ設定されます。 |
| レスポンス | error | エラーの時にはエラー情報が設定されます。JSON-RPC 2.0ではエラーの時のみ設定されます。 |
| レスポンス | id | リクエストで設定されたidの値がそのまま戻されます。 |

### エラーオブジェクト

RPCサーブレットでエラーが発生した場合にはレスポンスのメッセージボディでクライアントに通知されます。ステータスコードは、エラーの有無に関わらず200 OKが返されます。

```http
HTTP/1.0 200 OK
...
Content-Type: application/json

{ "result": null, "error": { "code": -32600, "message": "Invalid Request.", "data": {} }, "id": 1 }
```

errorの値には code, message, dataの三つのキーを持つJSON objectが設定されます。codeとmessageについては次表を参照してください。dataには投げられた例外のプロパティがセットされます（ただし、Throwableクラスのプロパティは除外されます）。

| エラー内容 | HTTP Status Code | Message Body |
| --- | --- | --- |
| JSONリクエストがJSON-RPCのリクエストとして不正 | 200 OK | エラー例 1 |
| methodで指定したクラス/メソッドが見つからない(※1) | 200 OK | エラー例 2 |
| paramsが不適切(※2) | 200 OK | エラー例 3 |
| JSONの解析に失敗した | 200 OK | エラー例 4 |
| errorsに設定された例外が発生した | 200 OK | エラー例 5 |
| その他の例外が発生した | 200 OK | エラー例 6 |

**エラー例 1**

```text
{
  "code": -32600,
  "message": "Invalid Request."
}
```

**エラー例 2**

```text
{
  "code": -32601,
  "message": "Method not found."
}
```

**エラー例 3**

```text
{
  "code": -32602,
  "message": "Invalid params."
}
```

**エラー例 4**

```text
{
  "code": -32700,
  "message": "Parse error.",
  "data": {
    "columnNumber": エラーが発生した列番号,
    "errorOffset": エラーが発生した位置,
    "lineNumber": エラーが発生した行番号
  }
}
```

**エラー例 5**

```text
{
  "code": errorsで設定した値,
  "message": 例外オブジェクトの単純クラス名 + ": " + getMessage()の値,
  "data": 例外オブジェクト（ただし、Throwableクラスに定義されているプロパティは除く）
}
```

**エラー例 6**

```text
{
  "code": -32603,
  "message": "Internal error."
}
```

(※1) クラス/メソッドが見つからなかった時だけでなく、メソッドからIllegalStateExceptionやUnsupportedOperationExceptionが投げられた場合も同じエラーが返されます。

(※2) Convertに失敗した場合だけでなく、メソッドからIllegalArgumentExceptionが投げられた場合も同じエラーが返されます。

<a id="configuration"></a>

### 設定方法

RPC サーブレットは、web.xmlにRPCServletを指定し、パスとClassのマッピングなどの設定を行うだけです。

```xml
<servlet>
    <servlet-name>rpcServlet</servlet-name>
    <servlet-class>net.arnx.jsonic.web.RPCServlet</servlet-class>
    <init-param>
        <param-name>config</param-name>
        <param-value>
        {
            "debug": true,
            "mappings": {
                "/{package}/{class}.json": "sample.web.${package}.service.${class}Service",
                "/rpc.json": "sample.${class}Service"
            }
        }
        </param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>rpcServlet</servlet-name>
    <url-pattern>*.json</url-pattern>
</servlet-mapping>
```

configで設定できる値は次の通りです（errorsを除き、RESTServletと同じです）。

| キー | 値型 | 説明 |
| --- | --- | --- |
| container | `net.arnx.jsonic.web.Container` | クラスのインスタンスを取得するためのコンテナを設定します。デフォルトは、`net.arnx.jsonic.web.Container`です。 |
| encoding | `java.lang.String` | Request/Responseの文字エンコーディングを設定します。デフォルトはUTF-8です。 |
| expire | `java.lang.Boolean` | クライアントキャッシュを抑制するHTTPヘッダを出力します(`Cache-Control: no-cache, Pragma: no-cache, Expires: Tue, 29 Feb 2000 12:00:00 GMT`)。デフォルトはtrueです。 |
| debug | `java.lang.Boolean` | デバッグモードの有効/無効を切り替えます。デフォルトはfalseです。 |
| mappings | `java.util.Map<String, String>` | URLパスとクラスのマッピングを行います。 パス中の`{name}`で囲まれた部分はパス変数として、クラス名の`${name}`に置換されたりメソッドの引数に設定されます(※3)。また、`{name:regex}`と記載することで、変数の定義を設定することができます（definitionsより優先します）。 |
| definitions | `java.util.Map<String, Pattern>` | mappings中の変数の定義を正規表現で設定します。設定されない場合は`[^/().]+`が設定されたものと扱われます。 |
| init | `java.lang.String` | 処理の実行前に呼び出されるメソッド名を設定します。デフォルトは`"init"`です。 |
| destroy | `java.lang.String` | 処理の実行後に呼び出されるメソッド名を設定します。デフォルトは`"destroy"`です。 |
| processor | `net.arnx.jsonic.JSON` | 処理に使用するJSONクラスを設定します。デフォルトではThrowableのメソッドのみ無視するJSONクラスが設定されます。 |
| namingConversion | boolean | 呼び出し時のクラス名、メソッド名の変換を行うか否か設定します。デフォルトはtrueです。 |
| errors | `java.util.Map<Class< extends Exception>, Integer>` | Exceptionクラスとエラーコードのマッピングを行います（継承したクラスも対象になります）。 |

(※3) 変数名のうち、classとpackageだけは特殊な扱いがされます。デフォルトでは、class変数中の文字列はUpperCamelに変換され、package変数中の「/」は「.」に変換されます。 また、URLパスにはコンテキストパスを含める必要はありません。

<a id="restservlet"></a>

## RESTサーブレット

RESTサーブレットは、GET/POST/PUT/DELETEなどHTTP Methodをベースに操作を行うRESTfullなWebサービス構築用サーブレットです。

### RESTサーブレットの概要

RESTサーブレットを使うと、GET/POST/PUT/DELETEなどのHTTP Methodに従って、対象となったクラスのメソッドが呼び出されます。その後、戻り値がJSONに変換されクライアントに返されます。

HTTP MethodとJava メソッド名のデフォルトのマッピングは次の通りです(※4)。

| HTTP Method | Java メソッド名 | 引数 |
| --- | --- | --- |
| GET | find | リクエストパラメータを`.`あるいは`[]`で区切られた階層構造とみなし引数の型に従い変換し設定されます。 |
| POST | create | Content-Typeが「`application/json`」の時は、メッセージボディのJSON文字列を引数の型に従い変換し設定されます。<br> Content-Typeが「`application/x-www-form-urlencoded`」の時はリクエストパラメータを`.`あるいは`[]`で区切られた階層構造とみなし引数の型に従い変換し設定されます。 |
| PUT | update | Content-Typeが「`application/json`」の時は、メッセージボディのJSON文字列を引数の型に従い変換し設定されます。<br> Content-Typeが「`application/x-www-form-urlencoded`」の時はリクエストパラメータを`.`あるいは`[]`で区切られた階層構造とみなし引数の型に従い変換し設定されます。 |
| DELETE | delete | Content-Typeが「`application/json`」の時は、メッセージボディのJSON文字列を引数の型に従い変換し設定されます。<br> Content-Typeが「`application/x-www-form-urlencoded`」の時はリクエストパラメータを`.`あるいは`[]`で区切られた階層構造とみなし引数の型に従い変換し設定されます。 |

(※4) ブラウザなどでは、PUT/DELETEが使えない場合があります。そのような場合の代替手段として、POSTリクエストのクエリ変数に「_method=HTTP Method名」を指定することもできます。POST以外のリクエストでは_methodは無視されます。更新リクエストの条件は[CSRF対策とクライアント設定](#csrf)を参照してください。<br>

例えば、mappingsに `"/{package}/{class}.{ext}": "boo.${package}.${class}Service"` という指定があった場合、 `/foo/woo/resource.json`というパスをGETすると、`boo.foo.woo.ResourceService`クラスの`find`が呼び出されます。

```http
GET /foo/woo/resource.json HTTP/1.0
...
```

boo.foo.woo.ResourceServiceは次のように実装されていたとします。

```java
package boo.foo.woo;

public class ResourceService {
	public Object find(Map params) {
	    List<Map> list = Database.select("select * from resource", params);
	    return list;
	}
}
```

この時、レスポンスのメッセージボディとしては次のような結果が返されるかもしれません(※5)。

```http
HTTP/1.0 200 OK
...
Content-Type: application/json

[
  { "id": 1, "name": "boo", "age": 10 },
  { "id": 2, "name": "foo", "age": 12 },
  { "id": 3, "name": "woo", "age": 14 }
]
```

(※5) JSONはobjectかarrayより始まる必要があるため、それ以外の要素に変換される型の戻り値（例えば、boolean/int/Dateなど）の場合にはSC_NO_CONTENTが返されます。<br>

引数には送信されたデータが指定された型に従い変換され設定されます。引数への設定は、データの送信方法によって次のような違いがあります。

| Content Type | Request Type | 説明 |
| --- | --- | --- |
| application/json | JSON object | パス変数、リクエストパラメータの順に追加されたJSON objectが設定されます（同じキーが複数出現した場合は配列化されます）。 |
| application/json | JSON array | 送信されたJSON arrayを引数リストとして扱います。なお、第一引数がJSON objectである場合には、上記と同様にパス変数、リクエストパラメータ、第一引数の順でデータが追加されます。 その他の型や第二引数以降はそのまま設定されます。 |
| URLパラメータ<br>application/x-www-form-urlencoded | URLパラメータ<br>application/x-www-form-urlencoded | リクエストパラメータを`.`あるいは`[]`で区切られた階層構造とみなし変換したオブジェクトが設定されます。 |

さきほどの例でidが3のデータを指定する場合は次のようにします。

```http
GET /foo/woo/resource.json?id=3 HTTP/1.0
...
```

この時、レスポンスのメッセージボディとしては次のような結果が返されるかもしれません。

```http
HTTP/1.0 200 OK
...
Content-Type: application/json

[
  { "id": 3, "name": "woo", "age": 14 }
]
```

よりREST的にしたいのであれば、mappingsのパスに `/{package}/{class}/{id}.{ext}` を定義するなどしてパラメータをURLに含めることなどもできます。

### エラーオブジェクト

エラーの発生はHTTP Status Codeによりクライアントに通知されます。

| エラー内容 | HTTP Status Code | Message Body |
| --- | --- | --- |
| クラス/メソッドが見つからない(※6) | 404 Not found |  |
| 送信されたJSONの解析/変換に失敗した | 400 Bad request |  |
| errorsに設定された例外が発生した | errorsで設定したステータスコード | エラー例 1 |
| その他の例外が発生した | 500 Internal Server Error |  |

**エラー例 1**

```text
{
  "name": 例外オブジェクトの単純クラス名
  "message": 例外オブジェクトのgetMessage()の値,
  "data": 例外オブジェクト（ただし、Throwableクラスに定義されているプロパティは除く）
}
```

(※6) クラス/メソッドが見つからなかった時だけでなく、メソッドからIllegalStateExceptionやUnsupportedOperationExceptionが発生した場合も同じエラーが返されます。

(※7) メソッドからIllegalStateException、UnsupportedOperationExceptionが発生した場合は除きます。

<a id="configuration"></a>

### 設定方法

REST サーブレットは、web.xmlにRESTServletを指定し、パスとClassのマッピングなどの設定を行うだけです。

```xml
<servlet>
    <servlet-name>restServlet</servlet-name>
    <servlet-class>net.arnx.jsonic.web.RESTServlet</servlet-class>
    <init-param>
        <param-name>config</param-name>
        <param-value>
        {
            "debug": true,
            "mappings": {
                "/{package}/{class}/{id:[0-9]+}.json": "sample.web.${package}.service.${class}Service",
                "/{package}/{class}.json": "sample.web.${package}.service.${class}Service",
                "/{class}.json": "sample.${class}Service"
            }
        }
        </param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>restServlet</servlet-name>
    <url-pattern>*.json</url-pattern>
</servlet-mapping>
```

configで設定できる値は次の通りです（method, verbを除き、RPCServletと同じです）。

| キー | 値型 | 説明 |
| --- | --- | --- |
| container | `net.arnx.jsonic.web.Container` | クラスのインスタンスを取得するためのコンテナを設定します。デフォルトは、`net.arnx.jsonic.web.Container`です。 |
| encoding | `java.lang.String` | Request/Responseの文字エンコーディングを設定します。デフォルトはUTF-8です。 |
| expire | `java.lang.Boolean` | クライアントキャッシュを抑制するHTTPヘッダを出力します(`Cache-Control: no-cache, Pragma: no-cache, Expires: Tue, 29 Feb 2000 12:00:00 GMT`)。デフォルトはtrueです。 |
| debug | `java.lang.Boolean` | デバッグモードの有効/無効を切り替えます。デフォルトはfalseです。 |
| mappings | `java.util.Map<String, String>` | URLパスとクラスのマッピングを行います。 パス中の`{name}`で囲まれた部分はパス変数として、クラス名の`${name}`に置換されたりメソッドの引数に設定されます(※8)。また、`{name:regex}`と記載することで、変数の定義を設定することができます（definitionsより優先します）。 |
| definitions | `java.util.Map<String, Pattern>` | mappings中の変数の定義を正規表現で設定します。設定されない場合は`[^/().]+`が設定されたものと扱われます。 |
| init | `java.lang.String` | 処理の実行前に呼び出されるメソッド名を設定します。デフォルトは`"init"`です。 |
| destroy | `java.lang.String` | 処理の実行後に呼び出されるメソッド名を設定します。デフォルトは`"destroy"`です。 |
| processor | `net.arnx.jsonic.JSON` | 処理に使用するJSONクラスを設定します。デフォルトではThrowableのメソッドのみ無視するJSONクラスが設定されます。 |
| namingConversion | boolean | 呼び出し時のクラス名、メソッド名の変換を行うか否か設定します。デフォルトはtrueです。 |
| errors | `java.util.Map<Class< extends Exception>, Integer>` | ExceptionクラスとHTTP Status Codeのマッピングを行います（継承したクラスも対象になります）。 |
| method | `java.util.Map<String, String>` | HTTP Methodに対応するメソッド名を設定します。デフォルトは、`{ "GET": "find", "POST": "create", "PUT": "update", "DELETE": "delete" }`です。なお、パス変数にmethodが設定されている場合は無視されます。 |
| verb | `java.util.Set<String>` | 使用できるHTTP Methodを制限します。デフォルトは、`["HEAD", "GET", "POST", "PUT", "DELETE", "OPTIONS"]`です。HEADとOPTIONSを使う場合は、methodも対応付ける必要があります。 |

(※8) 変数名のうち、classとpackageだけは特殊な扱いがされます。デフォルトでは、class変数中の文字列はUpperCamelに変換され、package変数中の「/」は「.」に変換されます。また、URLパスにはコンテキストパスを含める必要はありません。

なお、`method, verb`に関しては、`mappings`の各データ毎にも設定できます。その場合は次のようにマッピング先をJSON objectにします（マッピング先のプロパティ名は`target`にしてください）。

```text
    "mappings": {
        "/{package}/{class}.json": {
            "target": "sample.web.${package}.service.${class}Service",
            "method": { "GET": "print" },
            "verb": [ "GET" ]
        },
        ...
    }
```

<a id="path-method"></a>

### パス変数によるメソッドの指定

本来RESTでは、HTTP Methodで処理が決定されるためRPC的な任意のメソッド呼び出しは推奨されませんが、それでは不便が多いためパス変数に`method`を指定することで、任意のメソッド呼び出しを可能にしました。

例えば、次のように設定を行うと `/foo/calc.sum.json` を呼び出すと `sample.web.foo.service.CalcService` の `sum` メソッドが呼びだされます。

```text
    "mappings": {
        "/{package}/{class}.{method}.json": "sample.web.${package}.service.${class}Service"
    }
```

なお、このような使い方をする際は、verbと組み合わせてHTTP Methodを制限して使うことが推奨されます（GETで更新処理などを行うと、検索エンジンのクロールでデータが削除されるなどの問題が発生する可能性があります）。

<a id="jsonp"></a>

### JSONPの廃止

JSONPには対応していません。callbackパラメータによる関数呼び出しの付加は行わず、通常のJSONレスポンス（application/json）を返します。

<a id="csrf"></a>

### CSRF対策とクライアント設定

RESTのPOST・PUT・DELETEは、サービスを呼び出す前に次の条件で検証します。POSTの`_method`による上書きも対象です。

- `Origin`がある場合は、リクエスト先とスキーム・ホスト・ポートが一致する必要があります。異なるOrigin、`null`、不正な値、複数のOriginは403で拒否します。
- `Origin`がない場合は、`Content-Type: application/json`または`X-Requested-With: XMLHttpRequest`が必要です。どちらもない更新リクエストは403で拒否します。
- `Sec-Fetch-Site: cross-site`がある更新リクエストは403で拒否します。

Originを送らないクライアントでフォーム形式のPOSTを行う場合は、次のようにヘッダーを追加します。`_method=PUT`や`_method=DELETE`を使うPOSTも同様です。

```bash
curl -X POST 'http://localhost:8080/basic/rest/memo.json' \
  -H 'X-Requested-With: XMLHttpRequest' \
  --data-urlencode 'title=sample' \
  --data-urlencode 'text=sample memo'
```

このヘッダーを付けても、異なるOriginを送るリクエストは許可されません。同一Originを送るブラウザフォームと、同梱のjQueryサンプルは引き続き利用できます。

リバースプロキシを使う場合は、Servletコンテナが公開URLのスキーム・ホスト・ポートを認識するよう、信頼するプロキシをコンテナ側で設定してください。RESTServletはクライアントからの`Forwarded`・`X-Forwarded-*`ヘッダーを直接信用しません。コンテナが内部URLの情報を返す設定では、正当なOriginも不一致となり403になります。

<a id="response"></a>

### JSON以外のレスポンス

CSVやXMLなどJSON以外のレスポンスを返したい場合やリダイレクトしたい場合は、処理の最後でHttpServletResponse#flushBuffer()を実行して出力をコミットしてください。ステータスコードやコンテントタイプの設定、JSONの出力などJSONIC側の後続処理を抑制することができます。

```java
public class HogeService {
    // 自動インジェクション
    public HttpServletResponse response;
    
    // CSVを出力する
    public void print() throws IOException {
        response.setCharcterEncoding("MS932");
        PrintWriter writer = response.getWriter();
        writer.write("a,b,c\r\n");
        
        // バッファをフラッシュして、出力を確定させる
        response.flushBuffer();
    }
}
```

<a id="dicontainer"></a>

## DIコンテナ対応

RPCサーブレット、RESTサーブレットは、内部のコンテナを切り替えることで呼び出し対象のインスタンスを任意のDIコンテナにて管理することが可能です。

JSONICでは、[Spring Framework](https://spring.io/projects/spring-framework)に対応したSpringContainerを標準添付しています。このコンテナを利用すると、Springで管理されているコンポーネントをWebサービスとして利用できます。

```xml
<servlet>
    <servlet-name>rpcServlet or restServlet</servlet-name>
    <servlet-class>net.arnx.jsonic.web.RPCServlet or RESTServlet</servlet-class>
    <init-param>
        <param-name>config</param-name>
        <param-value>
          {
            "container": "net.arnx.jsonic.web.extension.SpringContainer"
          }
        </param-value>
    </init-param>
</servlet>
```

デフォルトでは最低限の機能のみ持つnet.arnx.jsonic.web.Containerが使われます。このコンテナが持つ機能は次の通りです。

- オブジェクトはClass#newInstance()により毎回生成されます。
- ログは、ServletContext#log()を使って書き出されます。
- 呼び出し対象となるクラスにinitあるいはdestroyという名前のメソッドがある場合、処理の前後に呼び出します（この機能は全コンテナ共通です）。(※9)
- JSPの暗黙オブジェクトライクなパブリックフィールドベースの簡易DIを提供します。

(※9) 呼び出されるメソッド名は設定で変更可能です。

暗黙オブジェクトは以下のように設定してください。クラスだけでなくフィールド名も合わせる必要があります（デフォルトコンテナのみの機能です）。

```java
public class HogeService {
    public ServletConfig config;
    public ServletContext application;
    public HttpServletRequest request;
    public HttpServletResponse response;
    public HttpSession session;
}
```

コンテナ自身にHttpServletRequestとHttpServletResponseのDI機能がないSpringContainerに関しては、setterによるインジェクション機能を提供しています（下記例を参照）。

```java
public class SpringDrivenService {
    // setterを用意すると自動で挿入します。プロパティ名を一致させる必要はありません。
    public void setRequest(HttpServletRequest request) {
        ...
    }
    
    public void setResponse(HttpServletResponse response) {
        ...
    }
}
```

### 処理エラーの取り扱い

処理でエラーが発生した場合、専用のログを取ったり、、トランザクションをロールバックしたいなどあるかもしれません。その場合は、コンテナを継承してexecuteメソッドをオーバーライドします。

```java
public class TransactionalContainer extends Container {
    ...

    // 処理実行時によばれます。
    public Object execute(JSON json, Object component, Method method, List<?> params) throws Exception {
        Object ret = null;
        try {
            ret = super.execute(json, component, method, params);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
```

<a id="gatewayfilter"></a>

## Gatewayフィルタ

JSONICでは、おまけ機能としてJSONを使ってServletで良く使う各種の機能を実装したFilterを提供しています。JSONICの書式を使えるため手軽に設定が可能です。

最初にマッチしたパスの設定が使われますが、そこで設定が行われなわれていない場合、ルートの設定が初期値として利用されます。パスには正規表現が利用できます。

```xml
<filter>
    <filter-name>Gateway Filter</filter-name>
    <filter-class>net.arnx.jsonic.web.GatewayFilter</filter-class>
    <init-param>
        <param-name>config</param-name>
        <param-value>
            // 共通設定
            encoding: 'UTF-8'          // 文字コード設定
            locale: 'en'               // Responseのロケールを設定
            compression: true          // GZip圧縮
            
            // 拡張子がjsonのパスを対象
            '.+\.json': {
                expire: true           // クライアントキャッシュを無効化
            }
            
            // 例：日本向け設定
            '/ja/([^.]+)': {
                forward: '/$1.json'     // JSON Web Serviceに転送
                encoding: 'SHIFT_JIS'
                expire: true
                locale: 'ja-JP'
                access: ['jpuser']    // アクセス可能なロール
            }
        </param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>Gateway Filter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
    <dispatcher>FORWARD</dispatcher>
</filter-mapping>
```

**設定上の注意:** `FORWARD`を省略すると、転送先でGatewayFilterが実行されず、転送先の認可チェックが働きません。`/*`に対して`REQUEST`と`FORWARD`を必ず登録してください。`filter-name`は使用しているfilter定義の名前に合わせます。

configで設定できる値は次の通りです。accessはforward先でも毎回検証します。転送先の認可を有効にするため、上記のようにREQUESTとFORWARDの両方へフィルタを登録してください。圧縮・文字コードなどの応答設定と、設定によるforwardは1リクエストにつき1回だけ適用します。

| キー | 値型 | 説明 |
| --- | --- | --- |
| encoding | `java.lang.String` | Request/Responseの文字エンコーディングを設定します。デフォルトはnullです。 |
| compression | `java.lang.Boolean` | クライアントから`Accept-Encoding: gzip or x-gzip`が送られる場合、ResponseをGZip圧縮します。 |
| expire | `java.lang.Boolean` | クライアントキャッシュを抑制するHTTPヘッダを出力します(`Cache-Control: no-cache, Pragma: no-cache, Expires: Tue, 29 Feb 2000 12:00:00 GMT`)。デフォルトは`false`です。 |
| forward | `java.lang.String` | 指定されたパスに転送します（パスはコンテキストパス以下を指定します。正規表現の置換変数が利用できます）。 |
| access | `java.util.Set<String>` | アクセス可能なアプリケーションロールを配列で指定します（認証そのものはコンテナの機能などを使う必要があります）。 |
| locale | `java.util.Locale` | Responseのロケールを設定します。 |

なお、encodingとexpireに関してはRPCサーブレットやRESTサーブレット側にも同様の設定が用意されており、そちら側の設定が優先されます。
