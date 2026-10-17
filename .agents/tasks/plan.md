# Implementation Plan: InputHelper を削除し、例外ベースの入力処理に置き換える

作業場所: `c:\pleiades\2026-03\Java1\Rental_System3`（その場で編集。worktree 禁止、git commit / push 禁止）。
コメント・画面メッセージは日本語（既存コードに合わせる）。メニュー構成・文言・流れは変えない。

## 調査結果（読んだ事実）

- `InputHelper` の使用箇所は全部で 6 ファイル 7 か所（`grep InputHelper` で確認済み）:
  - `src\Screen\Search.java:38` `readText(sc)`（タイトル入力）
  - `src\Screen\RentalMenu.java:48` `readInt(sc,1,dvd.length)-1` → `dvd[n]`（配列）
  - `src\Screen\ReturnMenu.java:37` `readInt(sc,1,dvdSave.size())-1` → `dvdSave.remove(n)`（List、`remove(int)`）
  - `src\Screen\TopMenu.java:112,115` `checkStaff` の名前（readText）と ID（readInt 0..999999）
  - `src\Manager\Employee.java:7(import),38` `switch (InputHelper.readInt(sc,1,2))`
  - `src\Manager\Manager.java:9(import),36` `readInt(sc,1,4)`
- 既存の例外の形（`src\Exception\`）:
  - `SystemMenuException extends Exception`（チェック例外、`String message` を受け取る）
  - `RentalException extends Exception`（`RentalMenu` が catch して `e.getMessage()` を表示）
  - `TopMenu.display` は `while(found){ try{ ...メニュー... } catch(SystemMenuException | InputMismatchException e){ System.out.println(e.getMessage()); } }`。
    つまり **例外を catch → メッセージ表示 → whileで先頭のメニューに戻る** が既存の回復方法。
- `Scanner` は `SystemMain` の 1 個（`static Scanner sc`）を全員で共有している。
  `Guest.sc` と `DVD.sc` も `new Scanner(System.in)` を持つが、どこからも読んでいない（grep 済み）。触らない。
- 現在のコンパイルエラーは **1 件だけ**（`javac -encoding UTF-8`、`メモ.java` 除外で確認）:
  `Screen\Check.java:31` の `d.getValue()`。
  ブリーフに書かれていた `Guest.java` の `GuestRank` は **すでに `GuestStatus` に直っていて、エラーではない**。

### 今の Scanner の読み方がバラバラで壊れている点（今回直す）

| 場所 | 今の読み方 | 問題 |
|---|---|---|
| `TopMenu.display` メニュー選択 | `hasNextInt()` / `nextInt()` / `next()` | `nextInt()` は改行を残す。直後のサブメニューが `nextLine()`（`Register`、`Search`、`InputHelper`）だと **空文字を受け取る** |
| `TopMenu.productGuest` 名前 | `if (sc.hasNextInt() \|\| sc.next().length()>=20) { sc.next(); throw ... }` その後 `String name = sc.next();` | 数字以外の入力では `||` の右側の `next()` で名前を **消費** し、続けて `name = sc.next()` が **次の単語を待つ**（名前を入力しても止まる）。正常な名前でも 2 回入力が必要になる既存バグ |
| `TopMenu.checkStaff` | `InputHelper`（nextLine） | 直前が `nextInt()` なので最初の `readText` が空行を拾い、空なので聞き直しになる |
| `Register.display` | `sc.nextLine().trim().equals("YES")` | `nextInt()` の後だと即「登録しません」になる（既存バグ） |

## 方針の決定

### 決定 1: 全部 `nextLine()` で読み、数値は `Integer.parseInt(line.trim())` で変換する

- 理由: `next()` / `nextInt()` / `nextLine()` を混ぜると改行が残って壊れる。`nextLine()` に統一すれば
  1 回の入力 = 1 行で必ず消費され、**不正な入力を捨て忘れる無限ループも起きない**（`hasNextInt()` が false のまま
  トークンが残り続ける問題がなくなる）。名前やタイトルに空白が入ってもよくなる。
- `sc.nextInt()` / `sc.next()` / `sc.hasNextInt()` は `src` 全体から無くす。`Register` の `nextLine()` はそのままで正しくなる。
- 変換失敗は `NumberFormatException`（`parseInt` が投げる、非チェック例外）で受ける。
  `InputMismatchException` は `Scanner.nextInt()` 専用なので、`nextLine()` 統一後は **どこからも投げられない**。
  よって `TopMenu` の `import java.util.InputMismatchException` と catch 内の `InputMismatchException` は外す。

### 決定 2: ヘルパークラスは作らず、各画面にインラインで try/catch を書く

- 理由: ユーザーは `InputHelper` を「邪魔すぎる」と言っている。新しいクラスやメソッドを足すと同じ不満になる。
  数行の重複は許容する。

### 決定 3: 例外の使い分けと回復の仕方（`TopMenu` の流儀に合わせる）

- 数字ではない → `NumberFormatException` を catch → 短いメッセージ → 戻る。
  メッセージは `e.getMessage()` を **使わない**（`For input string: ...` という英語になるため）。固定の日本語を表示する。
- 範囲外 / 空 / 長すぎ → 既存の `SystemMenuException` を `throw` して catch する（`TopMenu` と同じ）。
  配列・List の添字は `ArrayIndexOutOfBoundsException` / `IndexOutOfBoundsException` に任せてもよい箇所はそれで受ける（下記）。
- 回復は「聞き直しのループ」ではなく **メッセージを出して、その画面を抜けてトップメニューに戻る**（`TopMenu` の `while` が
  メニューを出し直す）。`Menu.display(Scanner)` は throws を持たない（インターフェースは変更しない）ので、
  サブメニュー内の例外はサブメニュー内で catch して `return` する。
  例外: 管理者メニュー（`Employee` / `Manager`）はすでに `while(running)` ループなので、catch したらそのままループ先頭に戻る。
  `TopMenu.productGuest` は元々 `while(true)` の聞き直しなのでそのまま聞き直す。

## 実装手順（依存順。各手順は単独でコンパイルできる状態にする）

作業前に必ずやること: `Rental_System3` で下記「検証コマンド」の javac を実行し、現在のエラーが `Check.java:31` の 1 件だけであることを確認する。
以降の手順でコンパイルが通らなくなったら、その手順内で直す。

- [ ] 1. `Screen\Check.java` の既存コンパイルエラーを最小限で直す。
      31 行目 `d.getValue()` は `DVD` に存在しない（`DVD` は `getType()` で `DVDPrice` を返し、`DVDPrice.getPrice()` がある）。
      `d.getName() + "\s" + d.getValue()` を `d.getName() + "\s" + d.getType().getPrice() + "円"` に変える。
      これは既存のコードだけで決まる機械的な修正（新しい設計判断なし）。**最終報告で「事前からあったエラーを直した」と明記**。
      `DVD.java` / `DVDPrice.java` は触らない。
      Files: `src\Screen\Check.java`
      Verify: 検証コマンドの javac がエラー 0 件（この時点では InputHelper はまだ存在するのでそのままコンパイルできる）。

- [ ] 2. `Screen\TopMenu.java` を `nextLine()` 統一に書き換える（3 か所）。
      Files: `src\Screen\TopMenu.java`
      a. `display` のメニュー選択: `if (!sc.hasNextInt()) {...}` ブロックと `int select = sc.nextInt();` を
         `int select = Integer.parseInt(sc.nextLine().trim());` の 1 行にする。
         （この 1 行が `NumberFormatException` を投げる。`hasNextInt` の説明コメントは削除するか「nextLine で読む」に書き換える）
      b. `display` の catch を 2 つに分ける:
         ```java
         } catch (SystemMenuException e) {
             System.out.println(e.getMessage());
         } catch (NumberFormatException e) {
             System.out.println("半角の数字を入力してください");
         }
         ```
         `import java.util.InputMismatchException;` を削除。`default ->` の `SystemMenuException("数値を入力してください")` は文言そのまま残す。
         この catch が下の `checkStaff` から伝わる例外も受ける（トップメニューに戻る）。
      c. `checkStaff(List<Guest> list)` を `throws SystemMenuException` にして、`InputHelper` を使わず:
         ```java
         String name = sc.nextLine().trim();
         if (name.isEmpty()) {
             throw new SystemMenuException("名前が空です");
         }
         System.out.println("ID番号を入力してください");
         int id = Integer.parseInt(sc.nextLine().trim());   // 数字以外は NumberFormatException → display の catch へ
         ```
         元の「ID は 0..999999」の範囲チェックは **入れない**（社員 ID は一致比較するだけで、範囲外は単に「関係者ではない」になる。
         `int` に入らない巨大な数も `NumberFormatException` で受かる）。それ以外の本体（社員リストの照合）は変更しない。
         呼び出し元の `case 8` は `display` の try の中なのでそのまま。
      d. `productGuest` の名前入力を書き換える（既存バグの修正を兼ねる）。`SystemMenuException` を使う既存の形は残す:
         ```java
         System.out.println("名前を入力してください");
         String name = sc.nextLine().trim();
         if (name.isEmpty() || name.length() >= 20 || name.matches("[0-9]+")) {
             throw new SystemMenuException("名前は数字だけ・空・20文字以上にはできません");
         }
         ```
         （元の条件「数字なら不可」「20 文字以上は不可」を維持し、空も不可にする。`sc.next()` と `hasNextInt` は完全に消す。）
         外側の `while(true)` + `catch (SystemMenuException e)` はそのまま（メッセージを出して聞き直す）。
      Verify: javac がエラー 0 件。`InputHelper` への参照は `TopMenu` から消えたこと（コンパイルは通るが、手順 6 で最終確認）。

- [ ] 3. `Screen\Search.java` の `InputHelper.readText(sc)` を置き換える。
      `display` の中で `String title = sc.nextLine().trim();` と読み、空なら
      `System.out.println("タイトル名が空です"); return;`（`SystemMenuException` を使う場合は
      `import Exception.SystemMenuException;` を足して try/catch。どちらでもよいが **`if` + `return` の方が短く、ここでは採用**）。
      その後の `map.containsKey(title)` 以降は変更しない。
      Files: `src\Screen\Search.java`
      Verify: javac がエラー 0 件。

- [ ] 4. `Screen\RentalMenu.java` の `InputHelper.readInt(sc, 1, dvd.length) - 1` を置き換える。
      既存の `try { ... } catch (RentalException e)` の内側で、商品一覧を表示した後に次のように読む:
      ```java
      DVD dOne;
      try {
          int n = Integer.parseInt(sc.nextLine().trim()) - 1;
          dOne = dvd[n];   // 範囲外は ArrayIndexOutOfBoundsException
      } catch (NumberFormatException e) {
          System.out.println("半角の数字を入力してください");
          return;
      } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println("1から" + dvd.length + "の番号を入力してください");
          return;
      }
      ```
      元の `DVD dOne = dvd[n];` 行は上のものに置き換わるので削除。以降（`getRented` 判定、`manager.cal`、`dOne.rented(today)`、
      `dvdSave.add(dOne)`、`catch (RentalException e)`）は **そのまま**。`dvd[n]` は `n` が配列外のとき（`0` → `-1` も含む）
      `ArrayIndexOutOfBoundsException` になる。`import` は不要（どちらも `java.lang`）。
      Files: `src\Screen\RentalMenu.java`
      Verify: javac がエラー 0 件。

- [ ] 5. `Screen\ReturnMenu.java`、`Manager\Employee.java`、`Manager\Manager.java` を置き換える。
      Files: `src\Screen\ReturnMenu.java`, `src\Manager\Employee.java`, `src\Manager\Manager.java`
      a. `ReturnMenu.display`: `int n = InputHelper.readInt(...) - 1;` と `DVD d = dvdSave.remove(n);` を次にする
         （`d` は後続の `d.returnRented()` で使うので try の外で宣言。`for` ループ内の `DVD d` はスコープが別で衝突しない）:
         ```java
         DVD d;
         try {
             int n = Integer.parseInt(sc.nextLine().trim()) - 1;
             d = dvdSave.remove(n);   // List<DVD>.remove(int)。範囲外は IndexOutOfBoundsException
         } catch (NumberFormatException e) {
             System.out.println("半角の数字を入力してください");
             return;
         } catch (IndexOutOfBoundsException e) {
             System.out.println("1から" + dvdSave.size() + "の番号を入力してください");
             return;
         }
         ```
         注意: `n` は `int` 型なので `remove(int index)` が呼ばれる（`remove(Object)` ではない）。範囲外で例外が出た時点では
         何も削除されないので、リストは壊れない。以降（`d.returnRented()`、延滞料の処理）は変更しない。
      b. `Employee.printCheck`: `import Screen.InputHelper;` を削除。`while (running)` の中身を
         ```java
         System.out.println(textBlock);
         try {
             switch (Integer.parseInt(sc.nextLine().trim())) {
             case 1 -> checkRegisterGuest();
             case 2 -> running = false;
             default -> System.out.println("1か2を選んでください");
             }
         } catch (NumberFormatException e) {
             System.out.println("半角の数字を入力してください");
         }
         ```
         既存の `default` が範囲外を処理するので `SystemMenuException` は不要。catch するとループ先頭に戻る。
      c. `Manager.printCheck`: `import Screen.InputHelper;` を削除。`int n = InputHelper.readInt(sc, 1, 4);` と
         `switch (n) {...}` を同様に `try { int n = Integer.parseInt(sc.nextLine().trim()); switch (n) {...既存のまま...} } catch (NumberFormatException e) { System.out.println("半角の数字を入力してください"); }` で包む。
         `case` と `default` の文言は変更しない。
      Verify: javac がエラー 0 件。

- [ ] 6. `InputHelper` を削除する。
      `src\Screen\InputHelper.java` を削除し、古いコンパイル済みの `bin\Screen\InputHelper.class` も 1 ファイルだけ削除する
      （`bin` は Eclipse の出力先。ほかの `bin` のファイルは触らない、`javac` の出力先に `bin` は使わない）。
      Files: `src\Screen\InputHelper.java`, `bin\Screen\InputHelper.class`
      Verify: `grep -ri InputHelper src bin` 相当の検索でヒット 0（`.agents` 内のメモは除く）。そして検証コマンドの javac がエラー 0 件。
      `grep` 単体は検証に数えない。**コンパイルが通ることが検証**。

- [ ] 7. 最終確認（コードは直さない。問題があれば該当手順に戻って直す）。
      Files: なし
      a. `src` 全体で `sc.next\(|nextInt|hasNext` が **0 件**、`nextLine` だけが残っていることを確認（`sc.nextLine()` を使うのは TopMenu / Search / RentalMenu / ReturnMenu / Employee / Manager / Register）。
      b. 下記「動作確認」の入力を流して、手で確認する。
      c. `git status --short` で、変更が `Check / TopMenu / Search / RentalMenu / ReturnMenu / Employee / Manager` と `InputHelper` の削除だけであること（`DVD.java`、`DVDPrice.java`、`Shop.java`、`Guest.java` などは **無変更**）を確認。
         作業用に作った一時ファイルがあれば消す。

## 検証コマンド（ビルドツールなし。plain javac）

`bin` を汚さないよう、出力先は一時フォルダにする。`メモ.java`（コメントだけのファイル）は除外する。
実行ポリシーで `.ps1` が直接動かないことがあるので、コマンドは 1 行ずつ PowerShell に渡すか `powershell -NoProfile -ExecutionPolicy Bypass -File ...` を使う。
このターミナルは入力のエコーで出力が崩れることがあるため、結果は `Out-File` でファイルに書いてから読むと確実。

```powershell
cd c:\pleiades\2026-03\Java1\Rental_System3
$o = Join-Path $env:TEMP "rs3_out"
New-Item -ItemType Directory -Force $o | Out-Null
$f = Get-ChildItem -Recurse src -Filter *.java | Where-Object { $_.Name -ne 'メモ.java' } | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d $o $f
```

- プロジェクトの文字コードは UTF-8（`.settings\org.eclipse.core.resources.prefs` の `encoding/<project>=UTF-8`）。`-encoding UTF-8` が必須。
- JDK は `javac 25.0.2`、Eclipse の設定は JavaSE-21。新しい構文は使っていないので問題なし（既存コードは `switch ->` と `"""` テキストブロックを使用）。
- 期待: 出力なし（エラー 0 件）。

### 動作確認（コンパイル後、手順 7 で実施）

標準入力を 1 行ずつ流す（最後に必ず `9` で終了させる。入力が足りないと `nextLine()` が `NoSuchElementException` で落ちるのはテスト入力の問題）。
文字化けを避けるため `java -Dfile.encoding=UTF-8 -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp $o SystemMain` にファイルを `Get-Content -Encoding UTF8 input.txt | java ...` で渡し、出力も UTF-8 でファイルに書いて読む。
確認したい入力の流れ（`ZaikoKanri` の作品名は実ファイルで確認して使う）:

1. 名前: `taro`（1 回で通ること = 旧バグが直ったこと）
2. メニュー: `abc` → 「半角の数字を入力してください」 → 次のメニューに戻る
3. メニュー: `99` → 「数値を入力してください」 → メニューに戻る
4. メニュー: `3`（検索）→ 空行 → 「タイトル名が空です」→ メニューに戻る
5. メニュー: `3` → 存在するタイトル → 詳細が出る（`nextInt` の残り改行で空になる旧挙動が無いこと）
6. メニュー: `1`（貸出）→ `x` → 数字メッセージ、`1` → `99` → 範囲メッセージ、`1` → `1` → 貸出成功
7. メニュー: `2`（返却）→ `x` → 数字メッセージ、`2` → `5` → 範囲メッセージ、`2` → `1` → 返却成功
8. メニュー: `4`（会員登録）→ `YES` → 「登録します」（旧: 改行が残って即「登録しません」になっていた）
9. メニュー: `8` → 名前 `太郎`、ID `abc` → 数字メッセージでトップに戻る。もう一度 `8` → `太郎` / `1000` → 店長メニュー
   → `z` → 数字メッセージ（メニューが再表示される）、`9` → 範囲メッセージ（`default`）、`4` で抜ける
10. メニュー: `9` → 「終了しました」

## 触らないもの / 注意

- `DVD.java`、`DVDPrice.java`、`Detail.java`、`Shop.java`、`Guest.java`、`GuestStatus.java`、`ZaikoKanri.java`、`EndDay.java`、`Register.java`、`Menu.java`、`SystemMain.java`、`Exception\*` は変更しない（今回の目的外。ユーザーが最近足した DVD 側の機能を戻さない）。
- `src\メモ.java` はコメントだけなのでコンパイル対象から除外する（削除もしない）。
- 新しいクラス・ヘルパーメソッドは作らない。
- `Menu` インターフェースのシグネチャは変えない。
- 既存の未コミット変更（`git status` に出ている `M` ファイル）は、必要な箇所以外を巻き戻さない。コミット・プッシュはしない。
- `Guest.sc` / `DVD.sc` の余分な `Scanner` は今回は触らない（読んでいないので害なし）。最終報告で「あるが未使用」と一言だけ触れる。
- 事前エラー報告: `Check.java:31` の `getValue()`（手順 1 で直す）。`Guest.java` の `GuestRank` はすでに解消済みなので何もしない。
  もし手順 1 以外にコンパイルエラーが出たら、既存コードだけで一意に直せるなら最小限で直して報告、設計判断が要るなら直さずに報告する。
