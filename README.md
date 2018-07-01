[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.arnx/jsonic)

# JSONIC

Simple JSON encoder/decoder written in java

2018/7/1 JSONIC は、リポジトリを GitHub に移動するとともに今後機能強化が行われることがないメンテナンスモードに移行します。機能、パフォーマンス共に優れた [jackson](https://github.com/FasterXML/jackson) への移行をおすすめいたします。

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

Version 1.2.6 からは、JavaScript内での直接出力用に escapeScript が追加されました。JSONでは許されていない string, number など値の出力やXSS脆弱性を防ぐ<>のエスケープも行われます

```jsp
// POJOをJavaScriptに変換します（）
var value = <%= JSON.escapeScript(value) %>;
```

JSONICには、JSON操作APIだけでなく、JSONを使ったWebサービスが簡単に構築できるサーブレットも用意されています。詳しくはWebサービスAPIのドキュメントを御覧ください。

## リポジトリ

Maven Central Repository から取得できます。

```xml
<dependency>
  <groupId>net.arnx</groupId>
  <artifactId>jsonic</artifactId>
  <version>1.3.10</version>
</dependency>
```

# JSONエンコーダー

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

<table class="table" summary="POJOからJSONへの変換ルール">
<tr><th style="width: 50%">変換元（Java）</th><th style="width: 50%">変換先（JSON）</th></tr>
<tr><td>Map, DynaBean[^2]</td><td rowspan="2">object</td></tr>
<tr><td>Object[^3]</td></tr>
<tr><td>boolean[], short[], int[], long[], float[], double[], Object[]</td><td rowspan="4">array</td></tr>
<tr><td>Iterable (Collection, Listなど)</td></tr>
<tr><td>Iterator, Enumeration</td></tr>
<tr><td>java.sql.Array, java.sql.Struct</td></tr>
<tr><td>char[], CharSequence</td><td rowspan="3">string</td></tr>
<tr><td>char, Character</td></tr>
<tr><td>TimeZone, Pattern, File, URL, URI, Path, Type, Member, Charset, UUID, java.timeの各クラス</td></tr>
<tr><td>byte[]</td><td>string (BASE64エンコード)</td></tr>
<tr><td>java.sql.RowId</td><td>string (シリアル化後、BASE64エンコード)</td></tr>
<tr><td>Locale</td><td>string (言語コード-国コードあるいは言語コード-国コード-バリアントコード)</td></tr>
<tr><td>InetAddress</td><td>string (IPアドレス)</td></tr>
<tr><td>byte, short, int, long, float, double</td><td rowspan="2">number[^4]</td></tr>
<tr><td>Number</td></tr>
<tr><td>Date, Calendar</td><td>number (1970年からのミリ秒)</td></tr>
<tr><td>Enum</td><td>string (デフォルトは名前で文字列化。<code>setEnumStyle</code> にて動作の変更が可能)<br />
number (<code>setEnumStyle</code> に null を指定すると Enum.ordinal により変換)</td></tr>
<tr><td>Optional型</td><td>isPresent() が false を返す時 null、その他の時、保持値</td></tr>
<tr><td>boolean, Boolean</td><td>true/false</td></tr>
<tr><td>null</td><td>null</td></tr>
</table>

[^2]: DynaBeanを利用する場合、Commons BeanUtilsのjarファイルをクラスパスに追加する必要があります。リフレクションを利用して処理を行っているため、利用しない場合は特に含める必要はありません。
[^3]: 対象となるインスタンスをパブリック・getterメソッド、パブリック・フィールドの優先順で探索します。staticが付加されたメソッドやフィールド、transientが付加されたフィールドは対象となりません。
[^4]: NaN, Infinity, -Infinityに限りそれぞれ文字列"NaN", "Infinity", "-Infinity"に変換されます。
また、org.w3c.dom.Document/ElementからJSONへの変換もサポートしています。詳しくは「高度な使い方 - XMLからJSONへの変換」の項をご覧ください。

なお、JSONはobjectかarrayで始まる必要があるため、直接、intやStringのインスタンスをencodeメソッドの引数に指定した場合エラーとなります。

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

<table class="table" summary="JSONからPOJOへの変換ルール">
<tr><th style="width: 20%">変換元（JSON）</th><th style="width: 40%">指定された型</th><th style="width: 40%">変換先（Java）</th></tr>
<tr><td rowspan="4">object</td><td>なし, Object, Map</td><td>LinkedHashMap</td></tr>
<tr><td>SortedMap</td><td>TreeMap</td></tr>
<tr><td>その他のMap派生型</td><td>指定された型</td></tr>
<tr><td>その他の型</td><td>指定された型（パブリック・フィールド／プロパティに値をセット)[^6]</td></tr>
<tr><td rowspan="9">array</td><td>なし, Object, Collection, List</td><td>ArrayList</td></tr>
<tr><td>Set</td><td>LinkedHashSet</td></tr>
<tr><td>SortedSet</td><td>TreeSet</td></tr>
<tr><td>その他のCollection派生型</td><td>指定された型</td></tr>
<tr><td>short[], byte[], int[], long[], float[], double[]<br />Object[]派生型</td><td>指定された型</td></tr>
<tr><td>Locale</td><td>Locale（「言語コード」「国コード」「バリアントコード」からなる配列とみなし変換）</td></tr>
<tr><td>Map</td><td>インデックスの値をキーとするLinkedHashMap</td></tr>
<tr><td>SortedMap</td><td>インデックスの値をキーとするTreeMap</td></tr>
<tr><td>その他のMap派生型</td><td>インデックスの値をキーとする指定された型のMap</td></tr>
<tr><td rowspan="18">string</td><td>なし, Object, CharSequence, String</td><td>String</td></tr>
<tr><td>char</td><td>char（幅0の時は'\u0000', 2文字以上の時は1文字目）</td></tr>
<tr><td>Character</td><td>Character（幅0の時はnull, 2文字以上の時は1文字目）</td></tr>
<tr><td>Appendable</td><td>StringBuilder</td></tr>
<tr><td>その他のAppendable派生型</td><td>指定された型（値をappend）</td></tr>
<tr><td>Enum派生型</td><td>指定された型（値をEnum.valueOfあるいはint型に変換後Enum.ordinal()で変換）</td></tr>
<tr><td>Date派生型,<br />Calendar派生型</td><td>指定された型（文字列をDateFormatで変換）</td></tr>
<tr><td>java.time の各クラス</td><td>指定された型</td></tr>
<tr><td>byte, short, int, long, float, double,<br />Byte, Short, Integer, Long, Float, Double,<br />BigInteger, BigDecimal</td><td>指定された型（文字列を数値とみなし変換）</td></tr>
<tr><td>byte[]</td><td>byte[]（文字列をBASE64とみなし変換）</td></tr>
<tr><td>Locale</td><td>Locale（文字列を「言語コード」「国コード」「バリアントコード」が何らかの句読文字で区切られているとみなし変換）</td></tr>
<tr><td>Pattern</td><td>Pattern（文字列をcompileにより変換）</td></tr>
<tr><td>Class, Charset</td><td>指定された型（文字列をforNameにより変換）</td></tr>
<tr><td>TimeZone</td><td>TimeZone（文字列をTimeZone.getTimeZoneを使い変換）</td></tr>
<tr><td>UUID</td><td>UUID（文字列をUUID.fromStringで変換）</td></tr>
<tr><td>File, URI, URL, Path</td><td>指定された型（文字列をコンストラクタの引数に指定し変換）</td></tr>
<tr><td>InetAddress</td><td>InetAddress（文字列をInetAddress.getByNameで変換）</td></tr>
<tr><td>boolean, Boolean</td><td>指定された型（"", "false", "no", "off", "NaN"の時false、その他の時true）</td></tr>
<tr><td rowspan="5">number</td><td>なし, Object, Number, BigDecimal</td><td>BigDecimal</td></tr>
<tr><td>byte, short, int, long, float, double,<br />Byte, Short, Integer, Long, Float, Double,<br />BigInteger</td><td>指定された型</td></tr>
<tr><td>Date派生型,<br />Calendar派生型</td><td>指定された型（数値を1970年からのミリ秒とみなし変換）</td></tr>
<tr><td>boolean, Boolean</td><td>指定された型（0以外の時true、0の時false）</td></tr>
<tr><td>Enum派生型</td><td>指定された型（名前あるいは int値をEnum.ordinal()に従い変換）</td></tr>
<tr><td rowspan="6">true/false</td><td>なし, Object, Boolean</td><td>Boolean</td></tr>
<tr><td>char, Character</td><td>指定された型（trueの時'1'、falseの時'0'）</td></tr>
<tr><td>float, double, Float, Double</td><td>指定された型（trueの時1.0、falseの時NaN）</td></tr>
<tr><td>byte, short, int, long,<br />Byte, Short, Integer, Long,<br />BigInteger</td><td>指定された型（trueの時1、falseの時0）</td></tr>
<tr><td>boolean</td><td>boolean</td></tr>
<tr><td>Enum派生型</td><td>指定された型（trueを1、falseを0とみなしEnum.ordinal()に従い変換）</td></tr>
<tr><td>Optional型</td><td>保持値（nullの場合 empty()の値が設定されます）</td></tr>
<tr><td rowspan="4">null</td><td>なし, Object</td><td>null</td></tr>
<tr><td>byte, short, int, long, float, double</td><td>0</td></tr>
<tr><td>boolean</td><td>false</td></tr>
<tr><td>char</td><td>'\u0000'</td></tr>
</table>

[^6]: 対象となるインスタンスに対しパブリックなsetterメソッド、パブリックなフィールドの優先順で探索します。staticやtransientのメソッド/フィールドは対象となりません。なお、プロパティ名は、単純比較が失敗した場合、LowerCamel記法に変換したものと比較します。

## 高度な使い方

JSONICでは、フレームワークなどでの利用を想定していくつかの便利な機能を用意しています。

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

### setMaxDepth - 最大深度の設定

JSONICは、encode/format時に自分自身を戻すようなフィールドやプロパティ、配列を無視することで再帰による無限ループが発生することを防ぎます。 しかし、そのインスタンスにとって孫に当たるクラスが自分のインスタンスを返す場合にも再帰が発生してしまいます。JSONICでは、このような場合へ対処するため 単純に入れ子の深さに制限を設けています。

なお、最大深度の設定はdecode/parse時にも有効ですので深すぎるデータの取得を避けることも可能となります。

この最大深度は、デフォルトでは32に設定されていますが変更することも可能です。

```java
// 5階層以下の情報は取得しない
json.setMaxDepth(5);
```

### setSuppressNull - null値の抑制

JSONICでは、format時に値がnullになっているJSON objectのメンバの出力を抑制できます。初期値はfalseです。余計なメンバが大量に出力されてしまう、プロパティの初期値を優先したいなどの場合に有効です。

```java
// null値の出力を抑制します。
json.setSuppressNull(true);
```

なお、Version 1.2 系では、parse 時や Map の format に対しても null 値が抑制されていましたが、不適切な場合が多いため1.3系では抑制しないよう変更されました。

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

### JSONHintアノテーション - 変換時ヒントの付加

場合によってデフォルトの変換方式では不十分な場合があります。JSONICでは、メソッドやフィールドにJSONHintアノテーションを付加することで、 動作を部分的に制御することが可能です。

設定できる属性は次の通りです。

<table class="table" summary="JSONHintアノテーションの属性">
<tr><th>属性名</th><th>値型</th><th>説明</th></tr>
<tr><td>name</td><td>String</td><td>出力/代入するキー名を変更します</td></tr>
<tr><td>format</td><td>String</td><td>対象の型がNumberあるいはDate型の場合は、指定したフォーマットに従って変換します。<br />
フォーマットの書式はそれぞれjava.text.DecimalFormat、java.text.SimpleDateFormatを参照してください[^9]。</td></tr>
<tr><td>type</td><td>Class</td><td>parse時に指定した型のインスタンスを生成します（対象の型のサブクラスを指定する必要があります）。</td></tr>
<tr><td>ignore</td><td>boolean</td><td>出力/代入対象から除外します</td></tr>
<tr><td>serialized</td><td>boolean</td><td>値がJSONであるものとして扱います。デフォルトはfalseです。
Format時はtoString()の値をそのまま出力[^10] 、Parse時は入力されたJSONをJava Objectに変換し再度formatした文字列が設定されます。</td></tr>
<tr><td>anonym</td><td>String</td><td>単純値型からMapや複合型に変換するときに単純値型を設定するプロパティ名を指定します。anonymを指定しない場合、Mapの場合はnullキーの値として設定されますが、複合型を指定した場合はエラーとなります。</td></tr>
<tr><td>ordinal</td><td>int</td><td>JSON objectへの変換する際のキーの出力順を昇順で指定します。デフォルトはキー値の自然順序順（＝負値指定）です。</td></tr>
</table>

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

### JSONHintによるSerializable指定 - データの部分シリアル化

JSONHintアノテーションのtype属性にjava.io.Serializableを指定することで、データをObjectInputStream/ObjectOutputStreamによりシリアル化されたバイト列データとして取り扱うことができます（バイト列はBase64でエンコードされJSON stringとして出力されます）。

```java
public class TestBean {
  @JSONHint(type=Serializable.class)
  public SerializableBean sb;
}
```

この機能を使うことで、JSON化が困難なオブジェクトもJSON-RPCなどでやり取りすることが可能となります。

## FAQ

- Q. RESTServletでHTTP GETを使うと日本語が文字化けします
- A. 入力文字エンコーディングの問題です。特にApache Tomcat5以降は仕様を厳密に解釈した結果、GETがsetCharacterEncodingを無視するという問題がありますのでuseBodyEncodingForURIを設定し回避する必要があります（JSONIC 1.1 ではGETパラメータを独自に解析していたため、この問題は発生していませんでした）。

- Q. 大量データを読み込むとOutOfMemoryErrorで落ちます。
- A. JSON.decode() や JSON.parse() は、JSON文字列をオブジェクトツリーとして生成するため、大量のデータを取り扱うとメモリを大量に消費してしまいます。そのような場合には、JSON.getReader() メソッドを使うことでデータをひとつずつ読み込み処理することができます。

- Q. Resin サーバで RESTServletやRPCServletが動作しません。
- A. Resin サーバでは、web.xml中にある ${...} を変数として扱うため、\${...} と書かないといけないようです。

## ライセンス

JSONICは、Apache License, Version 2.0下で配布します。

自分のライブラリへの組み込んでいただいたり、その際にパッケージ名の変更や処理の変更など行っていただいて構いません。保障はありませんが、ライセンスの範囲内でご自由にお使いください。

