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
