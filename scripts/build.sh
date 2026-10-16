#!/bin/bash
# 在 Linux/macOS 上编译（只用于检查能否编译）。用法：scripts/build.sh /path/to/Mapbot
set -e
APP="$1/app"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$APP/Mapbot.cfg" ] || { echo "找不到 $APP/Mapbot.cfg"; exit 1; }
CP=$(ls "$APP"/*.jar | grep -v '/mapbot-' | tr '\n' ':')
rm -rf "$ROOT/out" && mkdir "$ROOT/out"
javac -nowarn -encoding UTF-8 --release 17 -d "$ROOT/out" -cp "$CP" $(find "$ROOT/src" -name '*.java')
echo "编译完成：$ROOT/out"
