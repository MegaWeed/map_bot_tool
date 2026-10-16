# 编译 Mapbot 源码，并可选安装到 Mapbot 目录。
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -MapbotDir "F:\Mapbot-0.94\Mapbot"
#   powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -MapbotDir "F:\Mapbot-0.94\Mapbot" -Install
# 需要 JDK 17 或更高版本（javac 在 PATH 中）。
param(
    [Parameter(Mandatory = $true)][string]$MapbotDir,
    [switch]$Install
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$app = Join-Path $MapbotDir 'app'
if (-not (Test-Path (Join-Path $app 'Mapbot.cfg'))) { throw "找不到 $app\Mapbot.cfg，请确认 -MapbotDir 指向 Mapbot 安装目录" }

$out = Join-Path $root 'out'
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory $out | Out-Null

$cp = (Get-ChildItem $app -Filter *.jar | Where-Object { $_.Name -notlike 'mapbot-*.jar' } | ForEach-Object { $_.FullName }) -join ';'
$sources = Join-Path $out 'sources.txt'
Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } | Set-Content -Encoding ASCII $sources

& javac -nowarn -encoding UTF-8 --release 17 -d $out -cp $cp "@$sources"
if ($LASTEXITCODE -ne 0) { throw '编译失败' }
Remove-Item $sources
Write-Host "编译完成：$out"

if ($Install) {
    $override = Join-Path $app 'map_override'
    New-Item -ItemType Directory -Force $override | Out-Null
    Copy-Item -Recurse -Force (Join-Path $out 'au') $override
    $cfg = Join-Path $app 'Mapbot.cfg'
    $lines = Get-Content $cfg
    if (-not ($lines -contains 'app.classpath=$APPDIR\map_override')) {
        Copy-Item $cfg "$cfg.bak" -Force
        $i = [Array]::FindIndex([string[]]$lines, [Predicate[string]]{ param($l) $l -like 'app.classpath=*' })
        $new = @($lines[0..($i - 1)]) + 'app.classpath=$APPDIR\map_override' + @($lines[$i..($lines.Count - 1)])
        Set-Content -Encoding ASCII $cfg $new
        Write-Host '已在 Mapbot.cfg 中加入 map_override（原文件备份为 Mapbot.cfg.bak）'
    }
    Write-Host "已安装到 $override"
}
