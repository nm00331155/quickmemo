# QuickMemo 実装指示書
対象リポジトリ: https://github.com/nm00331155/quickmemo

この指示書は、QuickMemo の今後の開発を **一括で進めるための実装ロードマップ兼作業仕様書** です。  
Copilot / VSCode 上で、この指示書を元に **順番に、ただし一貫性を保ってまとめて実装** してください。

---

## 最重要ルール

1. **必ず最新のリポジトリコードを読み取ってから作業すること**
2. **完全ビルド可能なコードにすること**
3. **既存機能を壊さないこと**
4. **DB スキーマ変更時は Room Migration を必ず実装すること**
5. **場当たり修正ではなく、共通化・整理を優先すること**
6. **変更ファイルごとに責務を明確にすること**
7. **ログを適切に追加し、原因追跡できるようにすること**
8. **今回 AI 機能は完全削除すること（UI・コード・導線・残骸含む）**
9. **翻訳課金残骸も整理対象とする**
10. **最終的に差分要約を出すこと**

---

# プロダクト方針

QuickMemo は以下を主軸にする。

- 高速キャプチャ
- シンプル
- 軽量
- 安定
- 後から探しやすい
- 便利機能はあるが主張しすぎない

今後の実装・整理は、**「多機能化」より「速さ・安定性・扱いやすさ」** を優先すること。

---

# 今回の開発対象

今回まとめて実装・整理する対象は以下。

## A. 最優先バグ修正
1. メモ保存タイミングバグ修正
2. ハイライト反映問題の根本対策
3. デバッグログ導入

## B. エディタ改善
4. カーソル移動改善
5. 範囲選択改善
6. 読み上げ機能のデフォルトOFF化＋配置変更
7. 電卓の表示順見直し

## C. OCR改善
8. OCR後のルールベース整形
9. OCR撮影後の範囲選択トリミング
10. OCR結果プレビュー改善

## D. ロック画面 Todo 改善
11. ロック画面からの Todo 追加を修正
12. Direct Reply が動かない端末向け代替導線の追加

## E. Widget 全面修正
13. Memo / Todo / NewMemo Widget の更新不具合修正
14. Widget 更新導線の共通化

## F. 機能整理
15. AI機能の完全削除
16. 翻訳課金残骸の削除
17. 読み上げ UI の露出整理
18. 不要な前面導線の整理

---

# 実装順序

以下の順で実装すること。

1. メモ保存バグ修正
2. デバッグログ導入
3. Widget 修正
4. ロック画面 Todo 修正
5. エディタのカーソル/選択改善
6. OCR 改善
7. 読み上げ整理
8. AI完全削除
9. 翻訳課金残骸削除
10. 最終整理・import最適化・不要コード削除

---

# 1. メモ保存タイミングバグ修正

## 背景
compose-rich-editor の `RichTextState.toHtml()` は Compose snapshot 監視に正しく乗らず、IME composing 中に emit されないことがある。  
そのため、文字入力直後に戻ると直前の入力が保存されない。  
ハイライト反映遅延・未反映も同根の可能性が高い。

## 必須修正
### EditorScreen.kt
- 各 RichTextBlock の監視を
  - `snapshotFlow { state.toHtml() }`
  から
  - `snapshotFlow { state.annotatedString.text }`
  に変更する
- emit 時に `state.toHtml()` を取得して block html を更新する
- `resolveCurrentBlocks()` は維持し、保存時は必ずそこから最新 html を取得する
- 3秒ごとのポーリング同期を追加し、各 `RichTextState.toHtml()` と block html に差分があれば同期する
- Undo/Redo と競合しないよう、`isApplyingSnapshot` / restoring 中の挙動を壊さない

## 保存タイミング
以下すべてで `persistCurrentMemo()` が確実に動くこと
- BackHandler
- 上部戻るボタン
- Lifecycle ON_STOP
- 必要なら画面離脱時導線

## 受け入れ条件
- 1文字入力してすぐ戻っても消えない
- 改行なしの直前入力が保存される
- ハイライト直後に戻っても反映が残る
- Undo/Redo が壊れない
- 既存のバックアップ機能が壊れない

---

# 2. デバッグログ導入

## 新規追加
- `app/src/main/java/com/quickmemo/app/util/EditorDebugLog.kt`

## 仕様
- シングルトン
- 最大1000行キュー
- `filesDir/editor_debug.log` に出力
- 500KB 上限でトリム
- FileProvider 経由で共有可能
- clear 機能を持つ

## file_paths.xml
以下を追加すること
```xml
<files-path name="debug_logs" path="." />
Copy
ログ対象
Editor
画面読み込み
textSnapshot 同期
polling 同期
persist 開始
persist 終了
BackHandler
ON_STOP
Undo/Redo 適用時
Widget
refresh 要求
refresh 実行
widget count
更新失敗
Notification / Todo
Direct Reply 受信
Todo insert 実行
失敗時例外
受け入れ条件
ログがファイル出力される
共有 intent が作れる
clear で消える
3. Widget 全面修正
背景
ウィジェットが正常に動作していない。
QuickMemo のコア価値に直結するため、最優先で修正すること。

対象:

MemoView Widget
Todo Widget
NewMemo Widget
実装方針
共通化
新たに Widget 更新の共通窓口を作ること。例:

WidgetUpdateCoordinator
WidgetRefreshManager
少なくとも以下の関数を持つこと

refreshAll(context)
refreshMemoWidgets(context)
refreshTodoWidgets(context)
refreshNewMemoWidgets(context)
更新トリガー
以下のタイミングで必要な widget refresh を必ず呼ぶこと

メモ保存時
メモ削除/復元時
Todo追加時
Todo更新時
Todoチェック切替時
Todo削除時
Widget設定変更時
アプリ起動時に必要な再同期
BOOT_COMPLETED 後に必要な再初期化
バックアップ復元後
Glance / Widget の修正ポイント
state 更新後に update() / updateAll() が漏れていないか確認
glance state と app state の責務を整理
Widget config から戻った後に確実に反映されるよう修正
Widget ごとの設定キー・ID 対応を見直す
ActionCallback / receiver / worker / repository どこからでも共通 refresh を呼べるようにする
ログ
どの widget に refresh したか
件数
失敗理由
受け入れ条件
Memo Widget が保存直後に更新される
Todo Widget が追加/チェック後に更新される
NewMemo Widget の導線が壊れていない
設定変更後に widget 表示が反映される
再起動後も widget が正しく復元される
4. ロック画面からの Todo 追加修正
背景
ロック画面からの Todo 追加は高い価値があるが、現在まともに動いていない。
ただし Android / OEM / 端末設定依存があるため、全端末でロック解除不要を保証する設計は前提にしない。
その代わり、動く端末では Direct Reply、動かない端末では最小導線を提供する。

まずやること
Direct Reply 修正
以下を再点検・修正すること

NotificationActionReceiver
RemoteInput 受け取り
PendingIntent flags
BroadcastReceiver manifest 登録
ForegroundService refresh 呼び出し
DB 書き込みタイミング
tabId 解決
バックグラウンド制限下での動作
追加実装
代替導線
Direct Reply が使えない端末向けに、超軽量 Todo 追加画面を追加すること。例:

QuickAddTodoActivity
仕様:

1行入力専用
可能ならロック画面上でも表示しやすい形
showWhenLocked / setTurnScreenOn 相当の最小構成を検討
ダメな端末では解除要求になってもよい
起動後すぐ入力できること
タブは設定の既定タブを使う
通知アクション
まず Direct Reply
補助として Quick Add Activity を開くアクションも検討
実装時は既存通知 UI を壊さないこと
設定
「ロック画面からの直接追加は端末により制限されます」の注記を追加してよい
受け入れ条件
Direct Reply が対応端末で機能する
動かない端末でも Quick Add 導線で最小追加できる
追加後に通知表示が更新される
5. エディタのカーソル移動・範囲選択改善
背景
メモは追記だけでなく修正も頻繁に行う。
スマホ上の狙った位置へのカーソル移動と狙った範囲選択が難しいと、編集体験が悪化する。

実装方針
最優先
選択保持の安定化
カーソル左右移動ボタン
選択範囲左右拡張ボタン
具体仕様
選択保持
フォーマットバー押下や補助UI押下で選択が消えにくいようにする
最後の selection を保持できるようにする
ハイライトだけでなく、文字色・太字・斜体・辞書登録などでも安定させる
カーソル移動 UI
エディタ下部またはフォーマットバー右端に追加可能な編集補助ボタンを実装する

最低限:

← 1文字左へ
→ 1文字右へ
可能なら:

選択←
選択→
挙動:

選択なし: カーソル移動
選択あり: collapse せず、選択端を拡張/縮小する設計を検討
実装しやすさ優先で、まずは「選択拡張ボタン」を分けてもよい
UI方針
常時大きく主張しない
編集補助としてさりげなく配置
設定で ON/OFF できてもよいが、初期実装では常設でも可
フォーカスを奪わないこと
検討
ダブルタップ単語選択が可能なら導入
ただしライブラリ制約で難しい場合は無理しない
受け入れ条件
装飾ボタン操作で選択が消えにくい
カーソル微調整がボタンでできる
範囲選択を微調整できる
編集体験が悪化しない
6. OCR 改善（AI不使用）
方針
OCR改善は AIを使わず、ルールベース整形＋範囲選択トリミング で行うこと。

6-1. OCR後の整形
新規追加候補
OcrTextFormatter.kt
OcrTextNormalizationMode などの enum / model
整形モード
最低限 3 モードを用意すること

Raw（そのまま）
Normalized（整形）
Bulletized（箇条書き化）
Normalized のルール
前後空白除去
行頭/行末空白除去
タブをスペースへ変換
連続空白を1個に圧縮
連続改行を最大2個に制限
日本語句読点 、。 周辺の不要空白除去
英文記号 , . : ; 周辺の不要空白を軽く補正
括弧 () [] {} 「」 【】 周辺の不要空白を軽く補正
OCR起因の箇条書き行を軽く統一
URL / 電話番号 / 日付 / 時刻らしい文字列を極力壊さない
Bulletized のルール
箇条書き候補行を ・ に統一
1. - ● • * などを bullet とみなして統一
明らかに短い単語列の複数行は箇条書きとして整える
ただし文章本文まで無理に bullet 化しない
6-2. OCR結果プレビュー改善
既存の OCR 結果ダイアログを拡張し、以下を実装すること

Raw / Normalized / Bulletized の切り替え
デフォルトは Normalized
どのモードでも最終編集可能
「挿入」前に内容確認できる
必要なら「原文のまま挿入」も選べる
受け入れ条件
OCR結果が読みやすく整形される
箇条書き向きテキストは見やすく変換できる
整形しすぎて数字・URL・日付を壊さない
7. OCR 撮影後の範囲選択トリミング
方針
撮影前の複雑な自動検出ではなく、まずは 撮影後に手動で範囲選択して OCR する方式を採用する。

実装
新規画面候補
OcrCropScreen.kt
OcrCropActivity or navigation destination
必要な bitmap / uri 処理ユーティリティ
フロー
カメラ撮影 or 画像選択
OCR 実行前に画像プレビューを表示
ユーザーが矩形範囲を選択
選択範囲を crop
crop 後画像で OCR 実行
UI
最低限:

画像表示
ドラッグで矩形選択
四隅 or 辺ハンドルで範囲調整
ボタン
この範囲でOCR
全体をOCR
撮り直す or 戻る
実装方針
まずは軸に平行な矩形トリミングでよい
自動文書検出は不要
パフォーマンス・OOMに注意
大画像は適切にダウンサンプリングすること
将来タスクとして残すもの
撮影時ガイド枠
コントラスト補正
自動台形補正
今回は実装しない。

受け入れ条件
カメラ画像の任意範囲を選んで OCR できる
全体 OCR も従来通り使える
画像が大きくてもクラッシュしない
8. 読み上げ機能の整理
方針
読み上げ機能は削除しないが、デフォルトOFF にする。
表の主機能としては扱わず、設定で有効化した時だけエディタ下部に追加する。

必須対応
Settings
TTS_ENABLED 相当の設定を追加
初期値は false
Editor
上部 TopAppBar の TTS ボタンは削除
TTS有効時のみ、エディタ下部の補助アクション群に表示する
表示場所は目立ちすぎない位置
既存の TTS ダイアログは再利用してよい
受け入れ条件
初期状態で TTS ボタンが見えない
設定 ON でだけ下部に表示される
TTS 自体は正常動作する
9. 電卓機能の扱い
方針
電卓は必要機能のため削除しない。
ただし、ツールバー上の優先度はやや後ろへ下げてよい。

対応
デフォルト ON のまま維持
エディタ下部ツールバーでの並びを後ろ寄せする
OCR / 定型文 / 日時などとの並びを見直す
機能そのものは変えない
受け入れ条件
電卓は今まで通り使える
UI上の露出だけ整理される
10. AI機能の完全削除
方針
AI機能は将来検討に回し、今回 完全削除 する。
コードを綺麗にし、保守性を上げることが目的。

対象
以下をリポジトリ全体から洗い出して削除すること

AI ボタン
AI BottomSheet
AI 用 ViewModel state
AI utility / service / manager
AI 関連 navigation
AI 実験コード
AI 用文字列
AI 用設定項目
AI 用 drawable / icon / unused resources
AI ログ
AI に関連する DI 定義
AndroidManifest / receiver / worker / service で不要なAI関連
注意
OCR は残す
翻訳は AI扱いではなく別機能
Gemini Nano / ML Kit のうち OCR・翻訳以外の AI 用途コードは削除対象
受け入れ条件
AI関連コード・UI・導線が残らない
参照切れがない
未使用 import / resource が残らない
11. 翻訳課金残骸の削除
背景
翻訳機能は無料化済みの方針だが、課金関連の残骸が残っている可能性が高い。

対応
BillingManager
unlock_translation 商品を削除
translation 購入判定を削除
translation product details 取得を削除
purchaseState の translation 項目を整理
Editor / Settings / Premium
translation 購入ダイアログを削除
translation 購入ボタンを削除
purchaseTranslation() を削除
translation の課金分岐を撤去
無料機能として扱う
受け入れ条件
翻訳機能に課金導線が存在しない
Billing が remove_ads のみになる
既存の remove_ads は壊れない
12. Settings の整理
必須追加
以下の設定 UI を SettingsScreen に追加すること

12-1. DeepL API キー
入力欄を追加
既存 DataStore と連携
空でも動作すること
保存即反映
表示は必要に応じて password 風でもよい
12-2. ロック画面 Todo 表示件数
1〜15
default 8
UIは slider / stepper / number picker いずれでもよい
ForegroundService へ反映
12-3. ロック画面 Todo 対象タブ
tabId は 0-indexed
既存 Todo タブ名を使って選択 UI を出す
ForegroundService へ反映
12-4. TTS ON/OFF
初期値 false
Editor へ反映
12-5. Debug log 共有/クリア
editor_debug.log の共有ボタン
clear ボタン
受け入れ条件
Settings から各設定が変更できる
DataStore と同期する
ForegroundService / Editor に反映される
13. ForegroundService の修正
必須
QuickMemoForegroundService で以下を反映すること

DataStore から Todo 表示件数を読む
DataStore から 対象タブID を読む
対象タブの未完了 Todo を表示する
表示件数を設定通りにする
summary text も設定に合わせて調整する
現在の固定値を削除
preview 3件固定
expanded 8件固定
tab全件対象固定
ログ
読み込んだ tabId
読み込んだ maxLines
未完了件数
表示件数
受け入れ条件
設定変更後に通知表示へ反映される
指定タブのみ表示される
件数が設定通りになる
14. 実装・整理ルール
共通ルール
まず現状コードを読む
できる限り最小破壊
ただし責務分離のための新規クラス追加は積極的に行ってよい
巨大ファイルの更なる肥大化は避ける
可能なら util / manager / coordinator に分離する
resource 未使用は削除する
import 整理を必ず行う
エディタ関連
EditorScreen.kt が重い場合、OCR / TTS / selection helper / persist helper の責務分離を検討
ただし中途半端なリファクタで壊さないこと
Widget 関連
update ロジックを個別分散させず、必ず coordinator 経由に寄せる
通知関連
OEM差異を前提に、完全保証ではなく fallback を設計する
15. テスト観点
保存バグ
1文字入力後すぐ戻る
変換中/IME composing 中に戻る
ハイライト直後に戻る
ON_STOP 保存
Undo/Redo 後保存
Widget
メモ保存後更新
Todo追加/削除/チェック後更新
Widget設定変更後更新
再起動後の復元
Widget が空表示にならない
ロック画面 Todo
Direct Reply 動作
Quick Add Activity 動作
通知更新
既定タブへの追加
OCR
カメラ撮影→トリミング→OCR
ギャラリー画像→トリミング→OCR
Raw / Normalized / Bulletized 切替
数字/日付/URL保持
大画像でクラッシュしない
TTS
デフォルト非表示
設定ONで表示
読み上げ自体は動く
AI削除
AI参照が残らない
ビルドエラーがない
navigationエラーがない
16. 最終成果物に含めるもの
Copilot は最終的に以下を返すこと

修正ファイル一覧
追加ファイル一覧
削除ファイル一覧
実装内容要約
DB変更があれば Migration 内容
影響範囲
今後の未対応事項があれば簡潔に列挙
17. 今回の判断の確定事項
以下は仕様として固定する。勝手に変えないこと。

電卓は必要なので削除しない
電卓はデフォルトON
電卓の並びは後ろ寄せ可
AI機能は完全削除
OCRはAIを使わずルールベース整形
OCRは撮影後の範囲選択トリミングを追加
読み上げ機能は削除しない
読み上げはデフォルトOFF
読み上げは有効時のみ下部表示
ロック画面Todo追加は Direct Reply 修正＋ fallback 導線追加
Widget は最優先で修正
メモ保存バグは最優先で修正
18. Copilot への作業スタイル指示
1ファイルずつ雑に直すのではなく、関連導線を横断して読むこと
まず参照検索して全体像を把握すること
巨大差分でもよいが、一貫性を優先すること
最後に compile error がないよう import / reference / resource を必ず整理すること
削除済み機能の残骸を残さないこと
Room / DataStore / Service / Widget / Navigation の接続漏れを防ぐこと
19. もし途中で実装が大きくなりすぎる場合の分割単位
それでも内部的には以下単位で整理してよい

task/editor-persist-fix
task/editor-selection-tools
task/widget-refresh-rework
task/lockscreen-todo-fix
task/ocr-crop-and-format
task/tts-settings-refactor
task/remove-ai
task/remove-translation-billing-remnants
task/settings-notification-options
ただしユーザーへの最終提出は まとめて一貫した状態 にすること。