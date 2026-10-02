[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic)

# JSONIC

Simple JSON encoder/decoder written in java

2026/10/2 JSONIC は、リポジトリを GitHub に移動するとともに今後機能強化が行われることがないメンテナンスモードに移行しておりましたが、AIエージェント使えば速くなるんじゃね？　ということでパフォーマンスとセキュリティ対応のみを行ったバージョンをリリースすることにしました。

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

JSONICには、JSON操作APIだけでなく、JSONを使ったWebサービスが簡単に構築できるサーブレットも用意されています。詳しくは[WebサービスAPI](docs/webservice.md)のドキュメントを御覧ください。

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
  <version>2.0.0</version>
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

<a id="validation"></a>

### JSONの検証

データのデコードを行わず検証のみを行うvalidateメソッドも用意されています（これは、setDepth(0) を指定した時と同じです）。

```java
  JSON.validate(new FileInputStream("test.json"));
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

JSONReader は複数の連続した JSON も処理できるようになっていますので、Twitter API で返されるような、改行で区切られた JSON Streaming を扱うことができます。

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
