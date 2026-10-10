#!/bin/sh
# LICENSES.txt (英日併記, UTF-8 BOM付き) を組み立ててアプリのassetsへも置く。依存追加後は先に gen-third-party.ps1 で licenses/third-party.txt を更新する。
cd "$(dirname "$0")/.." || exit 1
{ printf '\357\273\277'; cat licenses/header.txt LICENSE licenses/third-party-header.txt licenses/third-party.txt; } > LICENSES.txt
mkdir -p app/src/main/assets && cp LICENSES.txt app/src/main/assets/LICENSES.txt
