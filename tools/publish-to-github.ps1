# このプロジェクトを GitHub に push して、Actions に APK をビルドさせる。
#
#   1) GitHub 側で空のリポジトリを作る（例: yumedebug/aria-music, README無し）
#   2) このスクリプトを実行:
#        pwsh -File tools/publish-to-github.ps1
#
# push すると .github/workflows/android-release.yml が走り、
# main への push では `main-latest` プレリリースに APK が添付される。
#
# gh CLI が入っていればリポジトリ作成まで自動で行う。
[CmdletBinding()]
param(
    [string]$User = "yumedebug",
    [string]$Repo = "aria-music",
    [string]$Branch = "main",
    [ValidateSet("private", "public")]
    [string]$Visibility = "private"
)

$ErrorActionPreference = "Stop"
# ネイティブコマンド(git/gh)の非ゼロ終了で例外にしない（PowerShell 7.3+ 対策）
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git が見つかりません。Git for Windows (https://git-scm.com/download/win) を入れてください。"
}

# 初回のみ初期化（既にリポジトリなら何もしない）
if (-not (Test-Path (Join-Path $root ".git"))) {
    git init -b $Branch | Out-Null
    Write-Host "git リポジトリを初期化しました（branch: $Branch）"
}

git add -A

# 変更が無ければ commit はスキップ
$pending = git status --porcelain
if ($pending) {
    git commit -m "Aria Music: ローカル再生 + YouTube Music 対応、CI で APK をリリース" | Out-Null
    Write-Host "コミットを作成しました"
} else {
    Write-Host "コミットする変更はありません"
}

# gh CLI があればリポジトリも作る（既にあれば失敗するだけなので無視）
if (Get-Command gh -ErrorAction SilentlyContinue) {
    gh repo create "$User/$Repo" "--$Visibility" --source $root --remote origin 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "GitHub リポジトリ $User/$Repo を作成しました"
    } else {
        Write-Host "gh での作成はスキップ（既存、または未ログイン）。手動で作成済みなら問題ありません。"
    }
}

$remote = "https://github.com/$User/$Repo.git"
git remote get-url origin *> $null
if ($LASTEXITCODE -eq 0) {
    git remote set-url origin $remote
} else {
    git remote add origin $remote
}

Write-Host "push します: $remote ($Branch)"
git push -u origin $Branch

Write-Host ""
Write-Host "完了。Actions の実行状況:"
Write-Host "  https://github.com/$User/$Repo/actions"
