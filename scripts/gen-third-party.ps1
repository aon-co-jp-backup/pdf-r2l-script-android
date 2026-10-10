# 依存クレートのライセンス一覧を stdout に出力する (cargo metadata を使用)
$m = cargo metadata --format-version 1 | ConvertFrom-Json
$m.packages | Where-Object { $_.source } | Sort-Object name | ForEach-Object {
  $lic = if ($_.license) { $_.license } else { "(see crate)" }
  "{0} {1} - {2}" -f $_.name, $_.version, $lic
}
