#!/usr/bin/env bash
# このプロジェクトを GitHub に push して、Actions に APK をビルドさせる。
#
#   1) GitHub 側で空のリポジトリを作る（例: yumedebug/aria-music）
#   2) bash tools/publish-to-github.sh
#
# push すると .github/workflows/android-release.yml が走る。
set -euo pipefail

USER="${1:-yumedebug}"
REPO="${2:-aria-music}"
BRANCH="${3:-main}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v git >/dev/null 2>&1 || { echo "git が見つかりません" >&2; exit 1; }

if [ ! -d .git ]; then
  git init -b "$BRANCH"
  echo "git リポジトリを初期化しました（branch: $BRANCH）"
fi

git add -A
if [ -n "$(git status --porcelain)" ]; then
  git commit -m "Aria Music: ローカル再生 + YouTube Music 対応、CI で APK をリリース"
  echo "コミットを作成しました"
else
  echo "コミットする変更はありません"
fi

REMOTE="https://github.com/$USER/$REPO.git"
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE"
else
  git remote add origin "$REMOTE"
fi

echo "push します: $REMOTE ($BRANCH)"
git push -u origin "$BRANCH"

echo
echo "完了。Actions: https://github.com/$USER/$REPO/actions"
