# Android メモアプリ「QuickMemo」開発指示書

あなたはAndroidアプリ開発のエキスパートです。以下の仕様に基づいて、Androidメモアプリ「QuickMemo」のプロジェクト全体を構築してください。すべてのファイルを生成し、ビルド可能な状態にしてください。

---

## 1. プロジェクト概要

- **アプリ名**: QuickMemo
- **パッケージ名**: com.quickmemo.app
- **コンセプト**: 「超速キャプチャ × シンプル × さりげないAI」。思いついた瞬間に書ける最速のメモアプリ。多機能すぎず、かゆいところに手が届く設計
- **ターゲットユーザー**: Google Keepでは物足りないが、NotionやObsidianは複雑すぎると感じている層
- **対応OS**: Android 8.0（API 26）以上
- **言語**: 日本語（デフォルト）、英語

---

## 2. 技術スタック

| 領域 | 技術 | バージョン |
|------|------|-----------|
| 言語 | Kotlin | 最新安定版 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2026.02.00 |
| ローカルDB | Room（FTS4で全文検索対応） | 最新安定版 |
| 状態管理 | ViewModel + StateFlow | Jetpack Lifecycle |
| 非同期処理 | Kotlin Coroutines + Flow | 最新安定版 |
| DI | Hilt | 最新安定版 |
| リッチテキスト | compose-rich-editor（com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13） | |
| 広告 | Google AdMob | 最新安定版 |
| 課金 | Google Play Billing Library | 最新安定版 |
| オンデバイスAI | ML Kit GenAI APIs（Gemini Nano） | 最新安定版 |
| 画像 | Coil for Compose | 最新安定版 |
| ナビゲーション | Navigation Compose | 最新安定版 |
| DataStore | Preferences DataStore（設定保存用） | 最新安定版 |

### アーキテクチャ
- **MVVM + Clean Architecture（簡易版）**
- パッケージ構成:
```
com.quickmemo.app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAO
│   │   ├── entity/       # Room Entity
│   │   └── database/     # RoomDatabase
│   ├── repository/       # Repository実装
│   └── datastore/        # DataStore（設定）
├── domain/
│   ├── model/            # ドメインモデル
│   ├── repository/       # Repositoryインターフェース
│   └── usecase/          # UseCase
├── presentation/
│   ├── home/             # メモ一覧画面
│   ├── editor/           # メモ編集画面
│   ├── search/           # 検索結果画面
│   ├── settings/         # 設定画面
│   ├── trash/            # ゴミ箱画面
│   ├── components/       # 共通UIコンポーネント
│   └── theme/            # Material 3テーマ定義
├── di/                   # Hiltモジュール
├── widget/               # App Widget
├── receiver/             # 共有インテント受信
├── service/              # 通知バー常駐サービス
├── ai/                   # Gemini Nano連携
├── billing/              # 課金管理
├── ads/                  # 広告管理
└── util/                 # ユーティリティ
```

---

## 3. データモデル

### Room Entity: MemoEntity

```kotlin
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",                    // 任意。空なら本文1行目が一覧タイトル
    val contentHtml: String = "",              // リッチテキストのHTML
    val contentPlainText: String = "",         // 全文検索用のプレーンテキスト
    val colorLabel: Int = 0,                   // 0=なし, 1=赤, 2=橙, 3=黄, 4=緑, 5=青, 6=紫
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isChecklist: Boolean = false,
    val isDeleted: Boolean = false,            // ゴミ箱フラグ
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null                // ゴミ箱に入れた日時（30日後に自動削除）
)
```

### Room FTS Entity（全文検索用）

```kotlin
@Fts4(contentEntity = MemoEntity::class)
@Entity(tableName = "memos_fts")
data class MemoFtsEntity(
    val title: String,
    val contentPlainText: String
)
```

### カラーラベル定義

```kotlin
enum class MemoColor(val index: Int, val lightColor: Color, val darkColor: Color) {
    NONE(0, Color(0xFFFFFFFF), Color(0xFF1E1E1E)),
    RED(1, Color(0xFFF28B82), Color(0xFF5C2B29)),
    ORANGE(2, Color(0xFFFBBC04), Color(0xFF614A19)),
    YELLOW(3, Color(0xFFFFF475), Color(0xFF635D19)),
    GREEN(4, Color(0xFFCCFF90), Color(0xFF345920)),
    BLUE(5, Color(0xFFA7FFEB), Color(0xFF16504B)),
    PURPLE(6, Color(0xFFD7AEFB), Color(0xFF42275E))
}
```

---

## 4. 画面仕様

### 画面一覧と遷移

```
アプリ起動 → ① メモ一覧（ホーム）
  ├→ ② メモ編集（FABタップ or メモカードタップ）
  │   └→ ③ AIボトムシート（✨タップ、対応端末のみ）
  ├→ ⑤ 検索結果（検索バー入力）
  └→ ⑥ 設定（右上アイコン）
      └→ ゴミ箱一覧

外部エントリーポイント:
  ④-a ウィジェット → ② メモ編集
  ④-b 通知バー → ② メモ編集
  ④-c 共有インテント → ② メモ編集
```

---

### ① メモ一覧画面（HomeScreen）

**レイアウト:**
- 上部: アプリ名「QuickMemo」＋右上に設定アイコン（ギア）
- 検索バー: `SearchBar`コンポーネント。タップで⑤検索結果画面へ遷移
- カラーフィルター: 検索バー下に横一列で6色の丸（Circle）を表示。タップで該当色だけフィルタリング。もう一度タップで解除。横スクロール対応
- メモ一覧: `LazyVerticalStaggeredGrid`（2列）。カードの高さは内容に応じて可変
- 「ピン留め」セクション: ピン留めメモがある場合、上部に「ピン留め」ラベル付きで表示
- 「すべてのメモ」セクション: ピン留め以外を新しい順（updatedAt降順）で表示
- 広告: 一覧の最下部にAdMobバナー広告（320x50）。メモカードの間には入れない。広告除去購入済みの場合は非表示
- FAB: 右下に「＋」FloatingActionButton。タップで②メモ編集（新規）へ

**メモカードの表示内容:**
- 背景色: colorLabelに応じた色
- タイトル（1行。空なら本文1行目）
- 本文プレビュー（最大3行）
- チェックリストの場合: チェックボックス付きのプレビュー
- 画像がある場合: サムネイル表示
- ピンアイコン（📌、ピン留め時のみ）
- ロックアイコン（🔒、ロック時のみ。ロック中は本文プレビューを「ロックされたメモ」に置換）
- 日付（updatedAt、例: 「2月21日」）

**スワイプジェスチャー:**
- 右スワイプ → ピン留め/解除（📌アイコン表示）
- 左スワイプ → ゴミ箱へ移動（🗑️アイコン表示）。Snackbarで「元に戻す」表示

**ロックされたメモをタップ:**
- 生体認証（BiometricPrompt）を要求 → 成功で②メモ編集画面へ

---

### ② メモ編集画面（EditorScreen）

**レイアウト（上から順）:**

1. **トップバー**: 左に「←戻る」、右に「⋮その他メニュー」
2. **タイトル入力欄**: `TextField`、プレースホルダー「タイトル（任意）」。フォントサイズ22sp、太字
3. **区切り線**: `Divider`
4. **本文入力欄**: `RichTextEditor`（compose-rich-editor）。プレースホルダー「メモを入力...」。画面の残りスペースを全て占有。スクロール可能
5. **文字数カウント**: 右下に小さく表示（例: 「128文字」）。contentPlainTextの文字数。設定でON/OFF可能
6. **サブバー**: 後述。Aaボタン押下時のみ表示。メインバーの直上に表示
7. **メインバー**: キーボード直上に固定。キーボード非表示時は画面下部に固定

**この画面に広告は一切表示しない。**

**自動保存:**
- 戻るボタン押下時、ホームキー押下時、他アプリへの切り替え時に自動保存
- 保存ボタンは設置しない
- 新規メモで本文もタイトルも空のまま戻った場合は保存しない

---

#### メインバー（常時表示ツールバー）

キーボード直上に横一列で配置。`Row` + `IconButton`。

```
[☑チェック] [📷画像] [📅日付] [⏰時刻] [Aa書式] [⋯その他]
```

| ボタン | アイコン | タップ動作 | 長押し動作 |
|--------|---------|-----------|-----------|
| チェックリスト | CheckBox | カーソル位置にチェックボックスを挿入/解除。isChecklistフラグも切替 | なし |
| 画像 | Image | ボトムシート表示:「カメラで撮影」「ギャラリーから選択」 | なし |
| 日付 | CalendarToday | カーソル位置に現在日付を即挿入（例:「2月21日(金)」）。フォーマット: `M月d日(E)` | DatePickerDialogを表示。選択した日付を挿入 |
| 時刻 | Schedule | カーソル位置に現在時刻を即挿入（例:「14:35」）。フォーマット: `HH:mm` | TimePickerDialogを表示。選択した時刻を挿入 |
| 書式 | FormatSize (Aa) | サブバーの表示/非表示をトグル | なし |
| その他 | MoreVert (⋯) | ドロップダウンメニュー表示（下記参照） | なし |

**「⋯その他」メニュー内容:**
```
📌 ピン留め         [トグルスイッチ]
🔒 ロック           [トグルスイッチ]  → ONにする時は生体認証を要求
🎨 メモの色         → 6色パレットをサブメニュー表示
✨ AI（要約/タグ）   → ③AIボトムシートを表示 ※対応端末のみ表示
─────────────────
📤 共有...          → システムの共有シート。プレーンテキストで共有
📋 テキストをコピー  → contentPlainTextをクリップボードにコピー
🗑️ ゴミ箱へ移動     → 確認ダイアログ表示後、isDeleted=trueにして一覧に戻る
ℹ️ メモ情報         → ダイアログ: 作成日、更新日、文字数、単語数
```

---

#### サブバー（書式ツールバー・Aaトグル時のみ表示）

メインバーの直上にスライドインアニメーション（`AnimatedVisibility` + `slideInVertically`）で表示。

```
[小][中][大] | [B][𝐼] | ●●●●●●● | [✕閉じる]
 文字サイズ   太字 斜体  文字色     閉じる
```

**文字サイズ（3段階、SegmentedButton風）:**

| ボタン | サイズ | 用途 |
|--------|-------|------|
| 小 | 14sp | 注釈・補足 |
| 中 | 18sp | 本文（デフォルト） |
| 大 | 24sp | 見出し |

- `RichTextState.toggleSpanStyle(SpanStyle(fontSize = XX.sp))` で実装
- 選択中のサイズのボタンは塗りつぶしで視覚フィードバック
- テキスト選択中 → 選択範囲に適用。選択なし → これから入力するテキストに適用

**太字・斜体（トグルボタン）:**

| ボタン | 実装 |
|--------|------|
| B（太字） | `richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))` |
| 𝐼（斜体） | `richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))` |

- 現在のスタイルがONの場合、ボタンを塗りつぶし表示
- `richTextState.currentSpanStyle` で現在の状態を取得

**文字色パレット（7色の丸を横一列で配置）:**

```kotlin
val textColors = listOf(
    Color(0xFF000000),  // 黒（デフォルト・ダークモード時は白）
    Color(0xFFD32F2F),  // 赤
    Color(0xFF1976D2),  // 青
    Color(0xFF388E3C),  // 緑
    Color(0xFFFF8F00),  // 橙
    Color(0xFF7B1FA2),  // 紫
    Color(0xFF455A64),  // グレー
)
```

- タップで `richTextState.toggleSpanStyle(SpanStyle(color = selectedColor))` を適用
- 選択中の色の丸にチェックマーク（✓）を表示
- 同じ色をもう一度タップでデフォルトに戻る

**✕閉じるボタン:** サブバーを非表示にする

---

### ③ AIボトムシート（AiBottomSheet）

**表示条件:** Gemini Nano対応端末でのみ表示。`Generation.getClient().checkStatus()` で `FeatureStatus.AVAILABLE` または `FeatureStatus.DOWNLOADABLE` の場合のみ。非対応端末では「⋯その他」メニューに✨AI項目自体を表示しない。

**レイアウト:**
- `ModalBottomSheet`
- ドラッグハンドル
- タイトル: 「✨ AIアシスタント」
- ボタン1: 「📝 このメモを要約する」+ 説明文「長い文章を3行に要約します」
- ボタン2: 「🏷️ タグを提案してもらう」+ 説明文「内容に合ったタグを自動推薦します」
- 結果表示エリア: AI処理完了後にテキスト表示。処理中はShimmer効果のプレースホルダー
- アクションボタン: 「メモに追記」（結果をメモ末尾に追加）、「閉じる」

**AI要約の実装:**
```kotlin
val summarizer = Summarization.getClient()
val request = SummarizationRequest.builder()
    .setInputText(currentMemo.contentPlainText)
    .setOutputFormat(OutputFormat.BULLET_POINTS)
    .build()
summarizer.generateSummary(request).collect { result ->
    when (result) {
        is SummaryResult.Partial -> updateUiWithPartialResult(result.text)
        is SummaryResult.Complete -> showFinalResult(result.fullText)
    }
}
```

**AIタグ推薦の実装:**
```kotlin
val generativeModel = Generation.getClient()
val response = generativeModel.generateContent(
    generateContentRequest {
        text("以下のメモの内容を分析して、適切なタグを3〜5個、カンマ区切りで提案してください。タグのみを出力してください。\n\n${currentMemo.contentPlainText}")
        temperature(0.3f)
        maxOutputTokens(100)
    }
)
```

---

### ④-a ウィジェット（QuickMemoWidget）

- **サイズ**: 4×2（推奨最小サイズ）
- **Glance API**（Jetpack Glance）で実装
- レイアウト:
  - アプリ名「QuickMemo」
  - 入力バー:「✏️ メモを書く...」→ タップでEditorScreen（新規メモ）を起動
  - ショートカットボタン3つ:
    - ☑ → チェックリストモードで新規メモ（Intent Extra: `is_checklist=true`）
    - 📷 → カメラ起動→撮影→画像付き新規メモ
    - 📅 → 今日の日付を自動入力した新規メモ

---

### ④-b 通知バークイック入力

- `ForegroundService`（`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`）で常駐通知を表示
- 通知内容:
  - タイトル: 「QuickMemo」
  - テキスト: 「タップして新しいメモを作成」
  - アクションボタン1: 「新規メモ」→ EditorScreen起動
  - アクションボタン2: 「チェックリスト」→ チェックリストモードでEditorScreen起動
- 設定画面でON/OFF可能（デフォルト: ON）
- `Notification.CATEGORY_SERVICE`、`PRIORITY_MIN`（控えめに表示）

---

### ④-c 共有インテント受信

- `AndroidManifest.xml` に `<intent-filter>` を設定:
  - `ACTION_SEND`、`type="text/plain"` を受信
  - `ACTION_SEND`、`type="image/*"` を受信
- 受信したテキスト（`EXTRA_TEXT`）またはURI（`EXTRA_STREAM`）を本文に入れた状態でEditorScreenを起動

---

### ⑤ 検索結果画面（SearchScreen）

- 上部に検索入力フィールド（自動フォーカス、キーボード表示）
- Room FTS4による全文検索を実行（`contentPlainText` と `title` を対象）
- 結果は `LazyColumn`（リスト形式・1列）で表示。検索結果では情報量優先のためグリッドにしない
- 各結果カード: タイトル、本文プレビュー（検索キーワードをハイライト＝太字＋アクセントカラー）、日付
- 「X件のメモが見つかりました」を結果上部に表示
- ゴミ箱のメモ（isDeleted=true）は検索対象外
- ロック中のメモは「ロックされたメモ」としてタイトルのみ表示

---

### ⑥ 設定画面（SettingsScreen）

以下のセクションと項目を上から順に表示:

**表示:**
- テーマ: 「ライト / ダーク / システム設定に従う」（デフォルト: システム）。DataStoreに保存
- 一覧の表示形式: 「グリッド / リスト」（デフォルト: グリッド）

**メモ入力:**
- 新規メモのデフォルト色: 7色（なし含む）から選択（デフォルト: なし）
- 文字数カウントを表示: ON/OFF（デフォルト: ON）

**クイックアクセス:**
- 通知バーにクイック入力を表示: ON/OFF（デフォルト: ON）。変更時にサービスを開始/停止

**セキュリティ:**
- アプリ起動時に認証を要求: ON/OFF（デフォルト: OFF）。ONの場合、アプリ起動時にBiometricPrompt

**データ:**
- ゴミ箱: タップで「ゴミ箱一覧画面」へ遷移。説明文「30日後に自動削除されます」

**課金:**
- 「✨ 広告を非表示にする」カード: FilledCard + アクセントカラーで目立たせる。「¥300の買い切り」テキスト。「購入する」ボタン → Google Play Billing で処理。購入済みの場合は「✓ 購入済み」と表示

**その他:**
- バージョン: アプリバージョン表示
- プライバシーポリシー: WebViewまたは外部ブラウザで開く
- 利用規約: 同上
- お問い合わせ: メールインテント
- レビューを書く: Google Play In-App Review API

---

### ゴミ箱一覧画面（TrashScreen）

- ⑥設定画面からの遷移
- `isDeleted=true` のメモを `deletedAt` 降順で表示
- 各メモカードに「復元」ボタンと「完全に削除」ボタン
- 上部に「すべて空にする」ボタン（確認ダイアログ付き）
- `deletedAt` から30日以上経過したメモは `WorkManager` で定期削除（1日1回チェック）

---

## 5. マネタイズ実装

### AdMob広告

- **バナー広告（320x50）** をメモ一覧画面（HomeScreen）の最下部のみに表示
- メモ編集画面、検索画面、設定画面には広告を一切表示しない
- テスト時はテスト広告IDを使用
- 広告除去が購入済みの場合、AdViewを非表示（`Visibility.GONE`相当）

### Google Play Billing（買い切りIAP）

- 商品ID: `remove_ads`
- 商品タイプ: `ProductType.INAPP`（一回限りの購入）
- 価格: ¥300（Google Play Consoleで設定）
- 購入状態の確認: アプリ起動時に `queryPurchasesAsync` で購入済みかチェック
- 購入済みフラグをDataStoreにキャッシュ（オフライン時も参照可能にする）
- 購入フローは設定画面の「購入する」ボタンから開始

---

## 6. テーマ・デザインシステム

### Material 3 + Material You

```kotlin
@Composable
fun QuickMemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You動的カラー
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuickMemoTypography,
        content = content
    )
}
```

### タイポグラフィ
- デフォルトフォント: システムフォント（Noto Sans CJK JP が利用可能な場合はそれ）
- メモカードタイトル: `titleMedium`
- メモ本文プレビュー: `bodyMedium`
- 日付表示: `labelSmall`

---

## 7. その他の実装要件

### 生体認証（BiometricPrompt）
- `androidx.biometric:biometric` を使用
- メモのロック設定時/解除時、ロックされたメモを開く時に使用
- 生体認証が端末で利用不可の場合は、画面ロック認証（PIN/パターン）にフォールバック

### 共有インテント（送信側）
- 「⋯その他」→「共有...」で `ACTION_SEND`、`type="text/plain"`
- 共有テキストは `contentPlainText`（リッチテキストではなくプレーンテキスト）

### WorkManager（定期タスク）
- ゴミ箱の30日超過メモの自動削除: 1日1回、`PeriodicWorkRequest`で実行

### リッチテキストのDB保存
- `RichTextState.toHtml()` でHTML文字列に変換して `contentHtml` カラムに保存
- 読み込み時は `RichTextState.setHtml(contentHtml)` で復元
- 検索用の `contentPlainText` は `RichTextState` からプレーンテキストを抽出して別途保存

### エッジケース処理
- 新規メモで何も入力せずに戻った場合 → 保存しない
- 画像添付後にメモを削除 → 画像ファイルも削除
- アプリが強制終了した場合 → ViewModel の `SavedStateHandle` で入力中のデータを復元

---

## 8. AndroidManifest.xml の主要パーミッション

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.android.vending.BILLING" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

---

## 9. 実装の優先指示

以下の順序で全ファイルを生成してください:

1. プロジェクトセットアップ（build.gradle.kts、依存関係、Hiltセットアップ）
2. Room DB（Entity、DAO、Database、Migration）
3. Repository + UseCase
4. テーマ定義（Material 3、カラー、タイポグラフィ）
5. メモ一覧画面（HomeScreen + ViewModel）
6. メモ編集画面（EditorScreen + ViewModel）＋メインバー＋サブバー
7. 検索画面（SearchScreen + ViewModel）
8. 設定画面（SettingsScreen + ViewModel + DataStore）
9. ゴミ箱画面（TrashScreen + ViewModel）
10. ウィジェット（Glance API）
11. 通知バー常駐サービス
12. 共有インテント受信
13. 生体認証
14. AdMob広告統合
15. Google Play Billing統合
16. Gemini Nano AI統合
17. WorkManager定期タスク
18. Navigation（NavHost、画面遷移）
19. MainActivityとApplicationクラス

すべてのファイルを省略せずに完全な形で出力してください。TODO コメントやプレースホルダーは残さず、実際に動作するコードにしてください。