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
