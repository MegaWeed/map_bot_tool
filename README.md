# Mapbot 社区维护版

Mapbot 是一个 Apex 英雄对局记录工具，原作者是 Sean Finocchiaro，0.94 版之后就停止更新了。
本仓库是在 0.94 的基础上继续维护的版本，只包含修改后的 Java 源码和编译脚本。

## 相比原版的改动

- **新赛季地图**：支持新版 2048×2048 地图，更新了信标和圈控台遮罩。
- **修复崩溃**：解决本地对局时 Chrome 驱动为空导致的崩溃。
- **简体中文识别**：新增游戏界面简体中文 OCR，需要 `chi_simbest.traineddata`。
- **界面中英双语**：设置 → 界面语言，可以即时切换；布局也重新设计过。
- **自定义地图**：设置 → 导入/替换地图…，可以给每张地图导入自己的图片，也能一键恢复默认。
- **从游戏导出地图**：调用 [rsx](https://github.com/r-ex/rsx)，从 Apex 的 `mp_rr_*.rpak` 中导出 `uiia` 地图图片并自动识别导入。
- **B 站（bilibili）分析**：直播/回放输入框支持 `bilibili.com/video/BV…`、`live.bilibili.com/房间号`、`b23.tv` 短链接。会用 Chrome 打开页面、自动静音播放并切换网页全屏后截取画面；如果需要登录才能看，请先在弹出的 Chrome 里登录。
- **清理过时内容**：关闭自动更新，删掉已经失效的官网、捐赠、水印等原作者信息。

## 仓库里不包含的内容

- 原版 `mapbot-0.94.jar` 和依赖的 jar（编译时直接使用你本地 Mapbot 安装目录里的 jar）
- 地图图片等游戏素材（版权归 EA / Respawn 所有）
- tessdata 识别模型
- rsx（AGPL 许可，需要的话自行下载）

## 编译与安装（Windows）

需要先装好原版 Mapbot 0.94，以及 JDK 17 或更高版本。

```powershell
# 只编译
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -MapbotDir "D:\Mapbot-0.94\Mapbot"
# 编译并安装
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -MapbotDir "D:\Mapbot-0.94\Mapbot" -Install
```

安装时会做两件事：

1. 把编译好的类复制到 `app\map_override\au\`。
2. 在 `app\Mapbot.cfg` 的类路径最前面加上 `map_override`（原文件备份为 `Mapbot.cfg.bak`），这样新类会覆盖原 jar 里的同名类。

原版 jar 本身不会被修改。想恢复原版，删除 `app\map_override\au` 即可。

## 声明

本项目基于原版 Mapbot 修改，原版代码的版权归原作者所有；仅供学习和个人使用，与原作者、EA、Respawn 均无关联。
