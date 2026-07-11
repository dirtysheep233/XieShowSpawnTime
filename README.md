# ShowSpawnTime Fabric

[简体中文](#简体中文) | [English](#english)

## 简体中文

ShowSpawnTime 的现代 Fabric 迁移版本，为 Hypixel Zombies 提供波次计时和 HUD 辅助功能。

本项目面向 Minecraft 1.21.5～1.21.11。迁移目标是在现代 Fabric API 和渲染架构下保持原版 ShowSpawnTime 的功能逻辑，同时修复跨版本 API、Mixin 和状态生命周期差异。

## 功能

- 各地图波次生成时间 HUD
- 普通波、最终波和最终波倒计时音效
- Alien Arcadium 特殊波颜色提示
- Powerup 生成提醒、预测和实体倒计时
- DPS 统计
- 回合用时与清场时间记录
- Wave 3 剩余僵尸估算
- sidebar 玩家生命值和 Fast Revive 冷却
- Lightning Rod 队列
- 近距离玩家透明显示
- 可拖动 HUD 编辑器
- Internal / LiveSplit AutoSplits
- 英文、简体中文、繁体中文及 Hypixel 多语言文本识别

## 支持版本

`multiversion-builds` 提供四个发行文件：

| 文件 | Minecraft 版本 |
|---|---|
| `ShowSpawnTime-2.1.1+1.21.5.jar` | 1.21.5 |
| `ShowSpawnTime-2.1.1+1.21.6-1.21.8.jar` | 1.21.6～1.21.8 |
| `ShowSpawnTime-2.1.1+1.21.9-1.21.10.jar` | 1.21.9～1.21.10 |
| `ShowSpawnTime-2.1.1+1.21.11.jar` | 1.21.11 |

每个版本组都使用对应 Minecraft、Yarn mappings 和 Fabric API 独立构建。不同版本组不能混用。

## 安装

1. 安装对应 Minecraft 版本的 Fabric Loader。
2. 安装对应版本的 Fabric API。
3. 从 `multiversion-builds` 选择匹配的 JAR，放入实例的 `mods` 文件夹。
4. 不要同时安装多个 ShowSpawnTime JAR。

## 使用

- `/sstconfig`：打开配置界面
- `/ssthud`：编辑 HUD 位置
- `/sst ins <2|3>`：手动修正 Insta Kill 模式
- `/sst max <2|3>`：手动修正 Max Ammo 模式
- `/sst ss <5|6|7>`：手动修正 Shopping Spree 模式
- `/sst mode <normal|hard|rip>`：修正难度识别
- `/sst autosplits`：切换 AutoSplits
- `/sst lang`：查看当前 Show Spawn Time 界面语言
- `/sst lang zh_cn`：将 Show Spawn Time 界面切换为简体中文
- `/sst lang en_us`：将 Show Spawn Time 界面切换为英文
- `/sst checkupdate`：检查更新

Show Spawn Time 界面默认使用简体中文。语言设置会保存到模组配置中，并在重启游戏后保留；该命令只切换 Show Spawn Time 自身的界面、HUD 和提示文本，不会修改 Minecraft 全局语言，也不会影响 Hypixel 服务端文本识别。

玩家透明、配置界面和 AutoSplits 也可以通过按键绑定操作。

## 构建

项目需要 Java 21。

当前默认目标为 Minecraft 1.21.8：

```bash
./gradlew clean build
```

构建结果位于 `build/libs`。

其它 Minecraft 版本需要同时指定对应的 `minecraft_version`、`yarn_mappings`、`fabric_version` 和 `minecraft_constraint`。发布产物必须按版本分别执行干净构建，避免兼容源码跨版本残留。

## 兼容性说明

- 这是客户端 Fabric Mod。
- Mixin 使用严格注入检查；Minecraft 内部方法发生变化时会明确失败，而不是静默丢失功能。
- sidebar 增强保留 Minecraft 原版排序、背景、宽度、队伍格式和 15 行限制。
- JAR 内嵌 MixinExtras，用户无需单独安装。
- 某些服务器或 Zombies 社区可能限制 HUD、计时或辅助功能，请遵守所在社区规则。

## 来源与许可

ShowSpawnTime 最初由 SeoSean 开发。本仓库是在原项目功能基础上进行的 Fabric 迁移与多版本适配，不宣称原始设计和 1.8.9 实现的作者身份。

AutoSplits 相关代码包含源自 tahmid-23 项目的 MIT 许可内容，许可文本见 `LICENSE_ZombiesAutoSplits`。

Minecraft、Fabric、Hypixel 和 LiveSplit 均属于各自权利人。本项目与 Mojang、Microsoft 或 Hypixel 无官方关联。

---

## English

ShowSpawnTime is a modern Fabric port that provides wave timing and HUD utilities for Hypixel Zombies.

This project supports Minecraft 1.21.5 through 1.21.11. Its goal is to preserve the behavior of the original ShowSpawnTime on the modern Fabric API and rendering architecture while addressing cross-version differences in APIs, Mixins, and state lifecycles.

### Features

- Wave spawn time HUD for every map
- Sounds for regular waves, final waves, and final-wave countdowns
- Special-wave color indicators for Alien Arcadium
- Powerup spawn alerts, predictions, and entity countdowns
- DPS counter
- Round time and room-clear time tracking
- Remaining zombie estimate for Wave 3
- Player health and Fast Revive cooldown on the sidebar
- Lightning Rod queue
- Nearby player transparency
- Draggable HUD editor
- Internal and LiveSplit AutoSplits
- Recognition of English, Simplified Chinese, Traditional Chinese, and other multilingual Hypixel text

### Supported Versions

The `multiversion-builds` directory contains four release files:

| File | Minecraft version |
|---|---|
| `ShowSpawnTime-2.1.1+1.21.5.jar` | 1.21.5 |
| `ShowSpawnTime-2.1.1+1.21.6-1.21.8.jar` | 1.21.6–1.21.8 |
| `ShowSpawnTime-2.1.1+1.21.9-1.21.10.jar` | 1.21.9–1.21.10 |
| `ShowSpawnTime-2.1.1+1.21.11.jar` | 1.21.11 |

Each version group is built independently with its corresponding Minecraft version, Yarn mappings, and Fabric API. Builds from different version groups are not interchangeable.

### Installation

1. Install Fabric Loader for your Minecraft version.
2. Install the matching version of Fabric API.
3. Select the matching JAR from `multiversion-builds` and place it in your instance's `mods` folder.
4. Do not install multiple ShowSpawnTime JARs at the same time.

### Usage

- `/sstconfig`: Open the configuration screen
- `/ssthud`: Edit HUD positions
- `/sst ins <2|3>`: Manually correct the Insta Kill pattern
- `/sst max <2|3>`: Manually correct the Max Ammo pattern
- `/sst ss <5|6|7>`: Manually correct the Shopping Spree pattern
- `/sst mode <normal|hard|rip>`: Correct the detected difficulty
- `/sst autosplits`: Toggle AutoSplits
- `/sst lang`: Show the current ShowSpawnTime interface language
- `/sst lang zh_cn`: Switch the ShowSpawnTime interface to Simplified Chinese
- `/sst lang en_us`: Switch the ShowSpawnTime interface to English
- `/sst checkupdate`: Check for updates

The ShowSpawnTime interface uses Simplified Chinese by default. The selected language is stored in the mod configuration and persists after restarting the game. This command only changes ShowSpawnTime's interface, HUD, and notification text; it does not change Minecraft's global language or affect recognition of Hypixel server text.

Player transparency, the configuration screen, and AutoSplits can also be controlled with key bindings.

### Building

Java 21 is required.

The default target is currently Minecraft 1.21.8:

```bash
./gradlew clean build
```

Build artifacts are written to `build/libs`.

Other Minecraft versions require matching `minecraft_version`, `yarn_mappings`, `fabric_version`, and `minecraft_constraint` values. Release artifacts must be built separately with clean builds to prevent compatibility sources from leaking between versions.

### Compatibility Notes

- This is a client-side Fabric mod.
- Mixins use strict injection checks. Changes to Minecraft internals therefore fail explicitly instead of silently disabling features.
- Sidebar enhancements preserve vanilla Minecraft sorting, backgrounds, width, team formatting, and the 15-line limit.
- MixinExtras is bundled in the JAR; users do not need to install it separately.
- Some servers or Zombies communities may restrict HUD, timing, or utility features. Follow the rules of the community in which you play.

### Credits and Licenses

ShowSpawnTime was originally developed by SeoSean. This repository ports and adapts the original project's functionality for Fabric and multiple Minecraft versions; it does not claim authorship of the original design or the Minecraft 1.8.9 implementation.

The AutoSplits code contains MIT-licensed material originating from tahmid-23's project. See `LICENSE_ZombiesAutoSplits` for the license text.

Minecraft, Fabric, Hypixel, and LiveSplit belong to their respective owners. This project is not officially affiliated with Mojang, Microsoft, or Hypixel.
