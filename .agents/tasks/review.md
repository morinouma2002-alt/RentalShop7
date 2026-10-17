# InputHelper を廃止し、nextLine() + 例外のインライン入力処理に統一

`InputHelper`（`readInt` / `readText` の聞き直しループ）を削除し、7か所の利用箇所を `Integer.parseInt(sc.nextLine().trim())` と画面ごとの `try/catch` に置き換えた。`TopMenu` のメニュー選択・`productGuest`・`checkStaff` も `nextInt()` / `next()` / `hasNextInt()` から `nextLine()` に揃えたので、`src` 内の入力読み取りは `nextLine()` だけになった。`Check.java` の既存コンパイルエラー（`d.getValue()`）は `d.getType().getPrice() + "円"` に直してある。回復の仕方は「固定の日本語メッセージを出して、その画面を抜けてトップメニューへ戻る」で、`TopMenu` の既存の流儀と一致している。

Watch for: (1) 私は検証の実行記録そのもの（コーダーの最終報告）を見ていない。コンパイル・実行済みという前提と、`bin` のクラスが最終ソースの時刻で再生成されている事実だけが根拠（confirmed / 実行結果は未確認）。(2) `RentalMenu` / `TopMenu` に意味のない整形変更（空白）が混じっており、初心者が diff を読むときのノイズになる（likely）。(3) `Guest.sc` / `DVD.sc` の未使用 `Scanner` が残っている（confirmed、無害）。いずれもブロッキングではない。

**Verdict**: APPROVED

## High-level view

入力の読み方は `nextLine()` に一本化された。旧実装で壊れていた「`nextInt()` が改行を残し、次の `nextLine()` が空を拾う」問題と、`hasNextInt()` が false のままトークンが残って無限ループする `checkStaff` の経路は、`src` から該当呼び出しが消えたことで構造的になくなっている。

エラー回復は、InputHelper の「正しい入力が来るまで何度でも聞き直す」から「メッセージを出してサブ画面を抜ける」に変わった。これは `TopMenu` のメニュー再表示と同じ回復方法で、ユーザーの「最近習った例外で対処する」という意図に合う。管理者メニュー（`Employee` / `Manager`）はもともと `while` ループなので、catch した後は同じメニューが再表示される。

ID の範囲チェック（0〜999999）は意図的に入れていない。範囲外 ID は「関係者ではない」扱いになるだけで、`int` に入らない値は `NumberFormatException` で受けている。DVD 側（`DVD` / `DVDPrice` / `Shop` / `Guest`）はコーダーが触っていない（更新時刻が作業前のまま）。

<details>
<summary>Issues (4)</summary>

1. **検証記録が見えていない（possible, 非ブロッキング）** — スクリプト入力での実行結果は、このレビューでは直接確認できていない。最終報告の実行ログ（`abc` / `99` / 空タイトル / `x`→範囲外 / `太郎`+`abc` / `9`）を人が一度見ておく。
2. **整形だけの変更が混在（likely, 非ブロッキング）** — `RentalMenu.java` の全体的な空白整理や `TopMenu` の `today = LocalDate.now()` などは入力処理と無関係。次回以降は入力処理の行だけを変える。
3. **未使用 Scanner が残存（confirmed, 非ブロッキング）** — `Guest.sc` と `DVD.sc` は `new Scanner(System.in)` のまま未使用。範囲外なので触っていないが、気になるなら別タスクで削除する。
4. **負の数が名前として通る（possible, 非ブロッキング）** — `productGuest` の `[0-9]+` は `-5` を弾かない。必要なら `-?[0-9]+` にする。

</details>

<details>
<summary>Details</summary>

## 回復動作が InputHelper から変わった点

旧 `readInt` は範囲外・非数値のとき同じ場所で聞き直した。新しい実装は `RentalMenu` / `ReturnMenu` / `Search` で `return` してトップメニューに戻るため、貸出で番号を間違えるとメニュー選択からやり直しになる。`Menu.display` が throws を持たない制約と `TopMenu` の流儀に合わせた計画どおりの挙動差で、欠陥ではない。

`ReturnMenu` は範囲チェックを `dvdSave.remove(n)` の `IndexOutOfBoundsException` に任せている。範囲外では何も削除されないので catch 後にリストは壊れない。`RentalMenu` の `dvd[n]` も `0` 入力（添字 -1）を `ArrayIndexOutOfBoundsException` で受けている。

## 名前の拒否条件が旧実装と少し違う

`productGuest` の拒否条件は「空・20文字以上・数字のみ（`[0-9]+`）」。旧実装は `hasNextInt()` で数値を弾いていたため `-5` も拒否されたが、新実装では `-5` が名前として通る（possible、実害は小さい）。

## 既存コンパイルエラーの扱い

`Check.java` の `d.getValue()` は `DVD.getType().getPrice()` に置き換えるのが機械的に決まる最小修正で、`"円"` の付加だけが新しい表示文字列。`Guest.java` の `GuestRank` は作業開始前にすでに `GuestStatus` へ直っており（更新時刻が作業前）、コーダーの変更ではない。

</details>

<details>
<summary>File map</summary>

- `src/Screen/InputHelper.java` — 削除
- `src/Screen/TopMenu.java` — `nextLine()` 統一、`checkStaff` に `throws SystemMenuException`、`productGuest` の名前検証
- `src/Screen/Search.java` — タイトル入力を `nextLine()` + 空チェックに
- `src/Screen/RentalMenu.java` — 番号入力を `try/catch`、整形変更あり
- `src/Screen/ReturnMenu.java` — 番号入力を `try/catch`
- `src/Manager/Employee.java` — メニュー入力を `try/catch`
- `src/Manager/Manager.java` — メニュー入力を `try/catch`
- `src/Screen/Check.java` — 既存コンパイルエラーの最小修正

差分全体: `git -C Rental_System3 diff -- src`（作業ツリーにはユーザーの未コミットの DVD 側変更も含まれる）

</details>
