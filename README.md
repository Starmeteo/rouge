# 双界行者（Dual World Walker）

> 基于 **JavaFX** 的俯视角、单机、**双世界切换** Roguelike 动作射击游戏可玩原型（《双界行者》）。
> 核心机制（需求 §2）：每个房间同时存在**光之界**与**影之界**，玩家不断穿梭两界才能看清全局、
> 找到通路、击败所有敌人；光形态远程 + 影形态近战。

- 开发周期：10 天完成可玩 Demo（单层级，见《双界行者》项目需求说明书.md）
- 当前进度：**第 3 天完成** —— 已完成控制器、游戏会话、玩家模型、场景生命周期、
  登录档案拆分，以及「破碎回廊」双界主题；玩家可移动、鼠标瞄准，并按形态使用远程光弹或近战影刃。
- 详细设计：`《双界行者》项目需求说明书.md`

---

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 21（以 `--release 21` 编译） | 满足需求「JDK 17+」要求 |
| JavaFX | 21.0.6 | 满足需求「JavaFX 17+」要求 |
| Maven | 3.9（内置 `mvnw` / `mvnw.cmd` wrapper） | 无需单独安装 Maven |
| JUnit | 5.12.1 | 已配置，当前包含玩家、档案、输入与世界切换规则测试 |

---

## 快速开始

### 环境要求
- 安装 **JDK 21+**（如 Microsoft OpenJDK 21、Oracle JDK 21+）
- 设置环境变量 **`JAVA_HOME`** 指向 JDK 安装目录（`mvnw` 依赖此变量）

### 运行方式

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

或使用 **IntelliJ IDEA**：
1. `File → Open` 选择项目根目录（Maven 自动导入依赖）；
2. 将 Project SDK 设为 JDK 21；
3. 运行 `com.phantomcorridor.Launcher` / `com.phantomcorridor.App`。

---

## 操作说明（当前已实现部分）

| 操作 | 按键 | 状态 |
|---|---|---|
| 登录确认 | 进入按钮 / Enter | ✅ 已实现 |
| 菜单确认 | 鼠标左键 / Enter / 方向键聚焦 | ✅ 已实现 |
| 暂停 / 继续 | Esc / P | ✅ 已实现 |
| 切换全屏 | F11 / Alt+Enter | ✅ 已实现 |
| 移动 | WASD / 方向键 | ✅ 已实现 |
| 瞄准 / 攻击 | 鼠标 / 左键连发 | ✅ 光弹 / 影刃已实现 |
| 切换世界（光/影） | Shift | ✅ 基础规则与视觉反馈已实现 |

---

## 当前进度（开发日志）

### ✅ 第 1 天：架构对齐 + 双界主题重构

- **工程对齐**：包名迁移为 `com.phantomcorridor`；Maven 模块 `module-info` 更新（仅 `requires javafx.controls`，移除不必要的 FXML 依赖）；JavaFX 21 / JUnit 5；`mvnw` wrapper
- **分层结构**（对应新需求 §9.1）：
  - `config` —— `AppConfig`（窗口/标题/帧率）、`GameConfig`（玩家属性/相位能量回复）、`RoomConfig`（房间尺寸/房间类型权重）、`Settings`（灵敏度/音量/种子）
  - `controller` —— `SceneManager`（统一场景切换 §9.3）
  - `core` —— `GameLoop`（AnimationTimer 固定 60Hz 步长主循环）、`GameState`（LOGIN/MAIN_MENU/PLAYING/PAUSED/GAME_OVER）
  - `model` —— 数据契约枚举 `WorldType`（光/影）、`RoomType`（入口/战斗/奖励/商店/事件/Boss）、`ItemType`（光/影/双/通用）；`entity/room/dungeon/combat/ai/effect` 子包已立（§9.1 规划）
  - `util` —— `CollisionUtil`（圆-圆/圆-矩形/点-矩形）、`RandomUtil`（区间/权重随机、固定种子复现）、`AssetLoader`（资源加载）
  - `view` —— `LoginView`（§8.1 登录：昵称 + 本地密码）、`MainMenuView`（§8.2 主菜单）、`GameView`（Canvas + 游戏主循环）、`PauseView`（暂停）、`SettingsOverlay`（设置）
- **双界主题（光/影）**：左上**光之界**暖金辉光球 + 右下**影之界**冷紫辉光球，标题金→紫渐变字色，按钮影紫面板 + 悬停影紫渐变填充、金光文字；余烬粒子交替光金/影紫
- **按钮动效（参考原项目保留）**：悬停放大 + 前置符文光标 ✦ 淡入；菜单入场错峰淡入上浮；覆盖层滑入滑出；开始游戏双界辉光球放大逼近 + 双界遮罩渐入

### ✅ 第 2.5 天：基础修复与视觉重制

- 登录、菜单与游戏分别接入 Controller，View 不再执行昵称校验或游戏规则。
- `SceneManager` 原子切换状态、界面与生命周期，后台动画和游戏循环可正确停止。
- 玩家档案从 `Settings` 拆出；昵称与密码摘要会保存在系统用户偏好中，再次进入时执行本地凭据校验，设置重置不再清空身份。
- 新增 `GameSession`、`Player`、`InputState` 与 `WorldShiftSystem`，支持移动、影形态加速、满能量切界、冷却和相位脉冲事件。
- 菜单背景改为旧金圣辉/幽紫影域/中央裂隙构成的“破碎回廊”，游戏画面加入相位墙、世界化配色和基础 HUD。
- 新增 7 项自动化测试，并升级 Surefire 以正确运行 JUnit 5。

### ✅ 第 3 天：玩家攻击基本动作

- 鼠标位置决定攻击方向，按住左键可按各形态的攻击间隔连续出招。
- 光形态发射有速度、寿命与冷却的远程光弹；影形态使用攻速更快的定向扇形斩击。
- 攻击状态归入纯模型层，渲染与输入仍保持分离，并新增攻击系统自动化测试。
- 登录和菜单共享同一套动态回廊背景；暂停时保留冻结的游戏画面并显示半透明菜单。

### ✅ 第 4 天：双世界切换闭环

- Shift 仅在相位能量满值时切界，切换后清空能量并进入冷却。
- 切界会触发可见的相位脉冲圆环，并清除玩家周围指定半径内的敌方弹幕。
- 敌方弹幕采用独立模型容器，第 6 天敌人攻击可直接接入，不需要重写清弹规则。

### ✅ 第 5 天：随机回廊与房间导航

- 每局生成入口、分支房间与末端 Boss 房组成的无环节点图，数字或文本种子均可复现。
- 设置页开发种子已接入新游戏流程；留空时自动生成随机种子。
- 入口为无障碍小型安全房，Boss 房明显更大；其余房间按类型生成不同尺寸、方形/矩形/L 形/十字轮廓与随机障碍。
- 玩家可通过高亮出口进入相邻房间；切界时若目标形态与墙重叠，会自动移动到最近安全点。
- 远程攻击受当前房间轮廓和实体/相位墙限制，跨房间时会清除残留攻击，不能穿墙影响其他房间。
- 小地图仅展示已发现节点，以实心/空心区分已探索和未探索房间，并高亮玩家当前节点。
- 登录、主菜单、游戏房间、HUD 和玩家统一为硬边低色阶像素风；角色具有待机、行走、攻击、切界和倒地姿态。

### 📅 后续开发计划（《双界行者》需求 §11）

| 天 | 内容 | 里程碑 |
|---|---|---|
| 第 2 天 | 登录界面 + 主菜单界面 | 登录后可进入主菜单（本阶段已含） |
| 第 3 天 | 玩家移动、角色渲染、攻击基本动作 | 玩家可移动和攻击 |
| 第 4 天 | 双世界切换（视觉变化 + 相位能量限制） | 可按 Shift 切换世界 |
| 第 5 天 | 地图生成算法、房间连接、墙壁碰撞、门禁逻辑 | 随机地图并进出房间 |
| 第 6 天 | 普通敌人 AI 与双世界可见性逻辑 | 光/影敌人分别可攻击 |
| 第 7 天 | 战斗房清理规则、宝箱、出口开启、基础道具掉落 | 房间完整流程打通 |
| 第 8 天 | Boss 战：双阶段跨世界机制 | Boss 可被击败 |
| 第 9 天 | 完善 HUD、场景过渡、死亡界面、重新开始 | 一局完整可玩 Demo |
| 第 10 天 | 数值平衡、Bug 修复、打包交付 | 可演示版本 |

---

## 工程结构

```
src/main/java
├── module-info.java              // 模块声明（requires javafx.controls）
└── com/phantomcorridor
    ├── Launcher.java             // 程序入口
    ├── App.java                  // JavaFX Application 主类 + 场景流转编排
    ├── config/                   // 各配置常量（魔法数值集中管理）
    │   ├── AppConfig.java        // 窗口尺寸、标题、帧率
    │   ├── GameConfig.java       // 玩家属性、相位能量回复参数
    │   ├── RoomConfig.java       // 房间大小、房间类型权重
    │   └── Settings.java         // 全局设置（灵敏度/音量/种子）
    ├── controller/
    │   └── SceneManager.java     // 统一场景切换（§9.3）
    ├── core/
    │   ├── GameLoop.java         // AnimationTimer 固定时间步长主循环
    │   └── GameState.java        // 界面状态（登录/主菜单/运行/暂停/结算）
    ├── model/
    │   ├── WorldType.java        // 光之界 / 影之界
    │   ├── RoomType.java         // 入口/战斗/奖励/商店/事件/Boss
    │   ├── ItemType.java         // 光/影/双/通用
    │   ├── entity/               // Player 已实现；Enemy/Bullet/ItemPickup 待接入
    │   ├── room/                 // Room/Wall/Direction/RoomNavigationSystem
    │   ├── dungeon/              // MapGenerator/DungeonMap
    │   ├── combat/               // 玩家攻击、投射物与敌方弹幕容器
    │   ├── ai/                   // IdleAI/ChaseAI/AttackAI（规划）
    │   └── effect/               // WorldShiftSystem（满能量切界、冷却、相位脉冲）
    ├── util/
    │   ├── CollisionUtil.java    // 圆-圆/圆-矩形/点-矩形碰撞
    │   ├── RandomUtil.java       // 区间/权重随机、固定种子复现
    │   └── AssetLoader.java      // 类路径资源加载
    └── view/
        ├── LoginView.java        // 登录界面（昵称 + 本地密码）
        ├── MainMenuView.java     // 主菜单（双界主题）
        ├── GameView.java         // 输入转发、Canvas 容器与切界闪屏
        ├── GameRenderer.java     // 房间、玩家与 HUD 纯渲染
        ├── DualWorldBackdrop.java // 登录/菜单双界回廊动态背景
        ├── PauseView.java        // 暂停面板
        └── SettingsOverlay.java  // 设置覆盖层
src/main/resources
└── com/phantomcorridor/ui/ui.css // 全局样式（破碎回廊 · 双界主题）
```

*每个包均带 `package-info.java` 说明包职责与规划，方便团队协作。*

---

## 架构要点

- **模型与渲染分离**：`model` 包为纯 POJO，不依赖 JavaFX 节点；渲染统一走 Canvas（§9.2）
- **固定时间步长**：核心 `GameLoop` 逻辑更新恒定 1/60s，与刷新率解耦（$3.3 手感保障）
- **碰撞集中化**：`util.CollisionUtil` 承载全部几何判定
- **场景统一切换**：`SceneManager.switchTo(state, view)` 原子更新状态、界面与生命周期（§9.3）
- **双世界契约**：`model.WorldType` 贯穿玩家/敌人/子弹，跨世界伤害与可见性由 `combat.DamageCalculator` 收敛
- **敌人系统**：`combat.EnemySystem` 按房间生成敌人，`Enemy` 执行世界隔离的追击/前摇/释放/收招状态机；怪物素材位于 `resources/com/phantomcorridor/enemies/`

## 协作约定

- 代码遵循统一规范：类 PascalCase、常量 UPPER_SNAKE、方法 camelCase、4 空格缩进、行宽 ≤120
- 公开类/方法必须写 JavaDoc，关键逻辑行内中文注释（注释引用需求条目便于回溯）
- 每日按《双界行者》需求 §11 的开发计划推进，**当天只做当天内容，优先打磨已交付功能**

---

*本项目为团队协作的可玩原型/演示版本，暂无联网、存档、手柄等高级需求（见《双界行者》需求说明书 §1.4、§4.3）。*
