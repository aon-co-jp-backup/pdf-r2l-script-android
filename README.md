> **後継 / Successor**: このリポジトリは [aon-co-jp/pdf-r2l-rs](https://github.com/aon-co-jp/pdf-r2l-rs) に**統合**されました(Windows / macOS / Linux / Android、綴じ方向=右/左/元と同じ、OCRの書体文字化、画像の高解像度化)。今後の開発・配布は pdf-r2l-rs で行います。
> This repository has been **merged into** [aon-co-jp/pdf-r2l-rs](https://github.com/aon-co-jp/pdf-r2l-rs) (Windows / macOS / Linux / Android; right/left/keep binding; OCR re-typesetting; image upscaling). Development and releases continue there.

# pdf-r2l-script-android

PDFを右綴じ(R2L)かつ見開き(右始まり)にする、Android 用アプリ(APK)です。
(English) An Android app (APK) that changes the binding direction of PDFs (R2L). Source: https://github.com/INORIA/pdf-r2l-script (reimplemented in Rust; the UI is a thin Kotlin layer, the PDF processing is Rust via JNI).

## インストール / Install
Releases の `pdf-r2l-script-android.apk` をダウンロードして開きます(「提供元不明のアプリ」の許可が必要です)。Android 8.0 以上。

## 使い方 / Usage
PDFを選択し、「変換して保存…」で保存先を選びます。ライセンスはアプリ内の「ライセンス / License」ボタンと
同梱の LICENSES.txt に英語・日本語併記で記載しています。

## 開発 / Build
```
cargo test
./gradlew assembleRelease   # 要: cargo-ndk, Android NDK/SDK, JDK 17。署名は環境変数 PDFR2L_KEYSTORE_FILE / PDFR2L_KEYSTORE_PASSWORD / PDFR2L_KEY_ALIAS
```
依存を変えたら `scripts/gen-third-party.ps1 > licenses/third-party.txt` の後に `sh scripts/build-licenses.sh`。

## LICENSE
MIT。詳細・第三者ライセンスは [LICENSES.txt](LICENSES.txt)。

## OCR・テキストフォント文字化機能について / About the OCR text re-typesetting feature

> **開発中(v1.0.1 で搭載予定。v1.0.0 には含まれません) / Planned for v1.0.1 (not included in v1.0.0)**

### 日本語

**この機能は、オンライン環境(インターネットまたはネットワーク接続)を前提とします。**

- **何をする機能か**: PDF の各ページを画像として読み取り(OCR)、文字がかすれて読みにくい部分も含め、認識した全ての文字を、元の書体(ゴシック体・明朝体など)に合わせたテキストフォントの文字に置き換えます。置き換えた文字は検索・コピーができる文字データとして PDF に入ります。既定でチェックが入っており、チェックを外すと元のデータのまま表示します。
- **なぜオンラインが必要か**: 文字の読み取り(OCR)は、`aruaru-llm`(`POST /v1/ocr`、Tesseract)に各ページの画像を送って行います。そのため、`aruaru-llm` に接続できるネットワークが必要です。また、各言語の OCR データ(日本語・英語・中国語・韓国語・ヨーロッパ諸言語・ロシア語・ウクライナ語・ペルシャ語・アラビア語など)は、必要になった時にだけ取得し、手元に保存(キャッシュ)して再利用します。初めて使う言語の取得にもネットワークが必要です。
- **送信される内容**: OCR のために、PDF のページ画像が `aruaru-llm` へ送られます。画像は文字認識のあいだだけ使い、処理後すぐに破棄され、保存されません。機密性の高い PDF は、チェックを外して使ってください。
- **オフラインのとき**: ネットワークに接続できない場合、この機能は使えません。チェックを外すか、従来どおり綴じ方向の変更だけを使ってください。**綴じ方向の変更(R2L/L2R)自体は、これまでどおりオフラインで動作します。**
- **ご注意**: OCR には認識の誤りが起こり得ます。元の書体の判定(ゴシック体・明朝体など)も推定であり、完全には一致しない場合があります。重要な文書は、変換後の内容を確認してください。

### English

**This feature requires an online environment (an Internet or network connection).**

- **What it does**: It reads each PDF page as an image (OCR) and replaces every recognized character — including faded or hard-to-read ones — with text in a font matching the original typeface style (e.g. gothic/sans-serif or mincho/serif). The replaced characters are stored in the PDF as real text, so they can be searched and copied. The option is checked by default; uncheck it to keep the original data as-is.
- **Why it needs to be online**: OCR is performed by sending each page image to `aruaru-llm` (`POST /v1/ocr`, Tesseract), so a network connection to `aruaru-llm` is required. The OCR language data (Japanese, English, Chinese, Korean, European languages, Russian, Ukrainian, Persian, Arabic, etc.) is fetched only when needed and cached locally for reuse; fetching a language for the first time also requires a network connection.
- **What is sent**: For OCR, the page images of the PDF are sent to `aruaru-llm`. They are used only while the text is being recognized, are discarded immediately afterwards, and are not stored. For confidential PDFs, uncheck the option.
- **When offline**: This feature is unavailable without a network connection. Uncheck it, or use the binding-direction change alone. **Changing the binding direction (R2L/L2R) itself still works offline, as before.**
- **Notes**: OCR can make recognition errors, and the detection of the original typeface style (gothic, mincho, etc.) is an estimate that may not match exactly. Please check the converted result for important documents.

## API 方針: REST 全廃・gRPC / Connect へ (2026-11-04)

### 日本語
- **REST は全廃する。新しい API は REST で作らない。** ブラウザや `curl` から呼ぶ API は **Connect の単純呼び出し**(`POST /<パッケージ>.<Service>/<Method>`、`content-type: application/json`、本文は JSON)、サービス間は **gRPC / gRPC-web**(RPoem の管理API `open_runo.admin.v1.Admin` と同じ流儀)。ストリーミングは、必要になってから。
- **既存の REST は、触るときに順に移す。** 移したら旧 REST の経路は消す。移行が終わるまで残るが、**新規には追加しない**。
- **JSON は RS-JSON に統一**する(読み込みは `from_str_secure` / `parse_secure` でサイズ・深さ・重複キーの上限つき、書き出しは `rust_json::fast`)。ブラウザの WASM に `prost` は入れない(サイズが増えるため)。
- **例外(自分たちの API ではなく、相手の規格が決めているもの)**: ACME、OAuth/OIDC のコールバック、第三者(GitHub など)からの Webhook、ヘルスチェック、静的ファイル配信、OTLP など。これらは規格どおりの HTTP を受ける。
- 参考実装: `RPoem`(`open_runo.admin.v1.Admin`)、`open-easy-web`(`server/src/connect.rs`、`proto/open_easy_web/admin/v1/admin.proto`)。
- ※ このリポジトリのコードが既に方針どおりかどうかは、各リポジトリの現状による。この節は方針の記録であり、コードの変更を意味しない。

### English
- **REST is being abolished. New APIs are not built as REST.** APIs called from browsers or `curl` use **Connect unary calls** (`POST /<package>.<Service>/<Method>`, `content-type: application/json`, JSON body); service-to-service calls use **gRPC / gRPC-web** (the same style as RPoem's admin API `open_runo.admin.v1.Admin`). Streaming only when it is actually needed.
- **Existing REST endpoints are migrated one by one, when they are touched.** The old REST route is removed once migrated. Until migration is finished they remain, but **no new ones are added**.
- **JSON is unified on RS-JSON**: reading with `from_str_secure` / `parse_secure` (size, depth and duplicate-key limits), writing with `rust_json::fast`. `prost` is not put into browser WASM (it would grow the binary).
- **Exceptions (shapes dictated by someone else's standard, not our own API)**: ACME, OAuth/OIDC callbacks, webhooks from third parties (e.g. GitHub), health checks, static file serving, OTLP, and so on. These accept plain HTTP as the standard requires.
- Reference implementations: `RPoem` (`open_runo.admin.v1.Admin`) and `open-easy-web` (`server/src/connect.rs`, `proto/open_easy_web/admin/v1/admin.proto`).
- Note: whether the code in a given repository already follows this policy depends on that repository. This section records the policy; it does not mean the code has been changed.

### Deutsch (KI-Übersetzung, redaktionell überarbeitet; noch nicht von Muttersprachlern geprüft)
- **REST wird vollständig abgeschafft; neue APIs entstehen nicht mehr als REST.** APIs, die aus dem Browser oder per `curl` aufgerufen werden, verwenden **unäre Connect-Aufrufe** (`POST /<Paket>.<Service>/<Methode>`, `content-type: application/json`, JSON-Body). Zwischen Diensten kommen **gRPC bzw. gRPC-web** zum Einsatz, im selben Stil wie die Admin-API von RPoem (`open_runo.admin.v1.Admin`). Streaming wird erst ergänzt, wenn es tatsächlich gebraucht wird.
- **Bestehende REST-Endpunkte werden schrittweise umgestellt, und zwar immer dann, wenn man ohnehin an ihnen arbeitet.** Sobald ein Endpunkt umgestellt ist, wird die alte REST-Route entfernt. Bis zum Abschluss der Migration bleiben die übrigen bestehen, es werden aber **keine neuen mehr hinzugefügt**.
- **JSON wird einheitlich über RS-JSON verarbeitet**: Gelesen wird mit `from_str_secure` bzw. `parse_secure` (mit Obergrenzen für Größe, Verschachtelungstiefe und doppelte Schlüssel), geschrieben mit `rust_json::fast`. `prost` wird nicht in das Browser-WASM aufgenommen, da es die Datei vergrößern würde.
- **Ausnahmen (Schnittstellen, deren Form ein fremder Standard vorgibt und die nicht zu unserer eigenen API gehören)**: ACME, OAuth/OIDC-Callbacks, Webhooks von Drittanbietern (z. B. GitHub), Health-Checks, die Auslieferung statischer Dateien, OTLP und Ähnliches. Diese nehmen, wie vom jeweiligen Standard verlangt, gewöhnliches HTTP entgegen.
- Referenzimplementierungen: `RPoem` (`open_runo.admin.v1.Admin`) und `open-easy-web` (`server/src/connect.rs`, `proto/open_easy_web/admin/v1/admin.proto`).
- Hinweis: Ob der Code eines einzelnen Repositorys diese Richtlinie bereits erfüllt, hängt vom jeweiligen Repository ab. Dieser Abschnitt dokumentiert lediglich die Richtlinie; er besagt nicht, dass der Code schon angepasst wurde.

### Русский (перевод выполнен с помощью ИИ и отредактирован; носителями языка пока не проверен)
- **От REST мы полностью отказываемся; новые API больше не создаются в стиле REST.** API, к которым обращаются из браузера или через `curl`, используют **унарные вызовы Connect** (`POST /<пакет>.<Service>/<Method>`, `content-type: application/json`, тело запроса — JSON). Для взаимодействия между сервисами применяются **gRPC и gRPC-web** — в том же стиле, что и административный API RPoem (`open_runo.admin.v1.Admin`). Потоковая передача данных добавляется лишь тогда, когда в ней действительно возникает необходимость.
- **Существующие REST-эндпоинты переводятся поэтапно — по мере того, как с ними приходится работать.** После перевода старый REST-маршрут удаляется. Пока миграция не завершена, оставшиеся эндпоинты сохраняются, но **новые не добавляются**.
- **Работа с JSON унифицирована на основе RS-JSON**: чтение — через `from_str_secure` или `parse_secure` (с ограничениями на размер, глубину вложенности и повторяющиеся ключи), запись — через `rust_json::fast`. `prost` в браузерный WASM не включается, поскольку это увеличило бы размер файла.
- **Исключения (интерфейсы, форму которых определяет чужой стандарт, а не наш собственный API)**: ACME, обратные вызовы OAuth/OIDC, вебхуки сторонних сервисов (например, GitHub), проверки работоспособности, раздача статических файлов, OTLP и тому подобное. Такие интерфейсы принимают обычный HTTP, как того требует соответствующий стандарт.
- Эталонные реализации: `RPoem` (`open_runo.admin.v1.Admin`) и `open-easy-web` (`server/src/connect.rs`, `proto/open_easy_web/admin/v1/admin.proto`).
- Примечание: соответствует ли код конкретного репозитория этой политике, зависит от самого репозитория. Данный раздел лишь фиксирует политику и не означает, что код уже изменён.
