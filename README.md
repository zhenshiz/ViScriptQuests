# ViScriptQuests

任务工程和运行文件上传需要服务端 4 级权限。客户端提交目标时只携带任务 ID、小任务 ID 和目标索引；服务端检查接取状态、当前目标与真实完成条件，并从服务端任务文件发放奖励。任务结束状态会在执行完成奖励指令之前写入运行时状态，避免指令重入重复结算。

联动 VSS 的权限回归场景可在主工程运行 `./gradlew :ViScriptQuests:runClient -PldTest=command_authorization -PldTestWindow=1280x720`。该开发场景使用隔离世界验证伪造购买、配置上传、目标提交与重复领奖。

ViScriptQuests 是一个面向 NeoForge 1.21.1 的 RPG 任务系统模组。它使用 LDLib2 的 UI、持久化、自动注册和节点图能力，把任务编辑、任务运行时、任务书、HUD 追踪和模组联动放在同一套数据模型里。

这个模组的目标不是只提供几个固定任务，而是让整合包作者和开发者可以用蓝图编辑器制作任务流程，再导出成服务端可加载的运行时任务文件。

## 主要功能

- 可视化任务蓝图编辑器：通过 LDLib2 节点图编辑任务开始、小任务、条件分支、汇合和任务结束。
- 任务书 UI：玩家可以查看任务分类、任务状态、当前步骤、目标进度和奖励信息。
- HUD 追踪：玩家追踪任务后，客户端 HUD 会显示当前目标和导航标记。
- 服务端任务进度持久化：玩家任务状态、分类、追踪目标、目标进度和待发奖励保存到世界数据。
- 小任务多目标支持：一个小任务可以包含多个目标，全部完成后才会结算该小任务。
- 流程节点支持：任务可以做线性流程、条件分支、并行/汇合流程。
- 蓝图导出：编辑器项目和运行时任务文件分离，方便继续编辑和服务器加载。
- 可扩展目标/奖励系统：目标、奖励和蓝图编译器都通过 LDLib2 注册表扩展。

## 任务编辑流程

任务编辑器可以通过命令打开：

```mcfunction
/viscript_quests editor
```

编辑器中保存两类文件：

- `.questproj`：编辑器项目文件，保存完整蓝图图数据，用于重新打开继续编辑。
- `.quest`：运行时任务文件，由蓝图编译导出，供服务端发放和执行任务。

默认目录由 LDLib2 资产目录管理：

- `viscript_quests/project`：服务端项目文件目录。
- `viscript_quests/quest`：服务端运行时任务文件目录。

编辑器可以将当前蓝图上传为项目文件，也可以编译并上传为运行时任务文件。

## 蓝图节点

当前任务蓝图按节点类型分组：

- 流程控制：任务开始、小任务、任务结束、条件分支、汇合节点。
- 任务目标：配置玩家需要完成的具体目标。
- 任务奖励：配置小任务或任务完成后的奖励。
- 逻辑与数学：用于条件分支、数值比较和简单计算。
- 调试节点：用于蓝图调试输出。

注意：LDLib2 的 Subgraph 目前可以作为编辑器整理工具保存进 `.questproj`，但当前编译器主要遍历根图。把目标或奖励只放进子图里，暂时不会自动编译进 `.quest`。

## 已支持目标

- 物品目标：检测玩家背包、末影箱和已接入容器来源中的物品数量，支持严格组件匹配、自动提交或手动提交，也可以选择提交时是否扣除物品。
- 货币目标：联动 ViScriptShop，检查玩家余额或提交指定货币数量。支持整数数量与动态数值表达式、自动或手动提交；开启“提交时扣除货币”后，手动模式允许分批缴纳并累计实际扣款，关闭时检查余额而不扣款。货币的小数部分保留在钱包中，扣款不会透支。
- 到达位置：检查玩家是否进入指定维度和坐标半径内，并可在 HUD 中显示导航标记。
- 击杀实体：按实体类型和实体标签累计击杀数量。
- 破坏方块：按方块类型累计破坏数量。
- 访问维度：玩家进入指定维度后自动完成。
- 实体交互：玩家右键指定实体类型或带指定标签的实体后完成。
- 进度目标：玩家完成指定 Advancement 后完成。
- 自定义触发：通过指令或服务端 API 触发，用于更具体的剧情、脚本或联动场景。

所有目标都支持自定义 `taskHint`。不填写时会使用目标自己的默认提示文本。

## 已支持奖励

- 物品奖励：向玩家发放指定物品。
- 经验奖励：给予指定经验点数。
- 指令奖励：以领奖玩家为命令源执行服务端指令。
- 战利品表奖励：支持数据包 loot table，也支持简单的自定义概率物品表。
- 货币奖励：可选联动 ViScriptShop，安装 ViScriptShop 时给玩家增加货币；未安装时会跳过，不会导致本模组崩溃。

奖励支持 ViScriptTeam 队伍联动。安装 ViScriptTeam 后，奖励节点可以配置是否只发给队长；离线成员的奖励会进入待发奖励列表，玩家上线后自动补发。

## 玩家功能

- 默认按键 `J`：打开任务书。
- 任务书打开后，按 `Esc`、当前绑定的“开关物品栏”键或已启用的“打开任务书”键可以关闭。后两项遵循玩家的改键设置。
- 任务书中可以查看任务分类、任务列表、当前步骤、目标进度、奖励和任务状态。
- 任务书会按玩家保存最后访问的分类，下次打开时恢复对应分类和分页；记录随服务端世界数据保存，分类被删除后回到第一个分类。
- 任务书打开期间，任务进度、自动提交结果和新解锁的小任务会随服务端同步更新，无需关闭重开；更新保留当前分类、分类分页和仍然可见的任务选择。
- 安装 JEI 后，鼠标指向物品获取目标的图标或文字，使用 JEI 配置的“查看配方”或“查看用途”键即可查询目标物品。键盘和鼠标两组绑定都支持，默认分别为 `R` / 鼠标左键、`U` / 鼠标右键；自定义展示图标不会改变查询物品，提交按钮仍正常处理任务提交。
- 任务书只接入 JEI 的配方查询快捷键，任务书界面不显示 JEI 侧边物品栏。
- 玩家可以选择追踪任务，追踪状态会同步到 HUD。
- 支持手动提交目标，例如需要分批提交物品或货币的任务。

如果客户端配置关闭了任务书按键注册，玩家仍可通过服务端同步入口或命令打开任务书。

## 服务端命令

根命令为：

```mcfunction
/viscript_quests
```

常用子命令：

```mcfunction
/viscript_quests book
/viscript_quests editor
/viscript_quests editor <project>
/viscript_quests category config
/viscript_quests reload <target>
/viscript_quests grant <target> <quest> [autoTrack]
/viscript_quests revoke <target> <quest>
/viscript_quests complete <target> <quest>
/viscript_quests submit <target> <quest> <step>
/viscript_quests trigger <target> <trigger_id>
```

命令主要用于管理、调试和制作任务。玩家日常查看任务可以使用任务书按键。

`grant` 末尾的 `autoTrack` 参数为 `true` 或省略时自动追踪新任务；为 `false` 时仅发放任务，保留玩家当前的追踪选择，没有追踪任务时保持未追踪。队伍共享发放也遵循这个选项。任务 ID 含中文、空格或目录分隔符时需要加双引号，命令补全会自动添加，例如 `/viscript_quests grant @s "主线/初次冒险" false`。

## 可选联动

- ViScriptTeam：队伍任务进度同步、队伍范围内任务状态复用、奖励队长限定和离线奖励补发。
- ViScriptShop：货币目标和货币奖励。
- JEI（19.36.0+）：任务书物品获取目标支持配方和用途查询，使用 JEI 自己的快捷键绑定。
- Sophisticated Backpacks：物品目标可以统计和扣除玩家背包模组中的物品。
- Beyond Dimensions：物品目标可以读取超越维度的统一存储。
- KubeJS：监听大任务、小任务、目标和奖励事件，并通过脚本主动操作任务状态。

这些联动通过独立的兼容层接入，任务运行时会尽量避免把可选模组变成硬前置。

KubeJS 与 NeoForge 任务事件的目标格式、过滤方法和完整示例见：

- `docs/kubejs-events.md`

## 开发者扩展

本模组的目标和奖励是注册式设计。附属模组通常需要同时提供运行时数据类、蓝图节点和蓝图编译器。

目标扩展需要：

- 继承 `ITask`，并用 `@LDLRegister(name = "...", registry = ITask.ID)` 注册运行时目标。
- 继承 `QuestBlueprintNode` 或 LDLib2 `Node`，并用 `@NodeAttribute(..., graphTypes = QuestBlueprintGraph.class)` 注册蓝图节点。
- 实现 `IQuestTaskNodeCompiler`，把蓝图节点参数编译成运行时目标。

奖励扩展需要：

- 继承 `IReward`，并用 `@LDLRegister(name = "...", registry = IReward.ID)` 注册运行时奖励。
- 添加奖励蓝图节点。
- 实现 `IQuestRewardNodeCompiler`，把蓝图节点参数编译成运行时奖励。

更完整的联动说明见：

- `docs/addon-task-reward-integration.md`

## 依赖与环境

- Minecraft：1.21.1
- NeoForge：21.1+
- Java：21
- LDLib2：2.2.10+

## 开发与构建

ViScriptQuests 现在作为 [ViScriptLib](https://github.com/zhenshiz/ViScriptLib) 多项目工程的一个子模块开发。**本仓库的代码无法单独构建和启动**：构建脚本依赖主工程统一提供的插件、依赖版本与运行配置，编译期也依赖同工程中的 ViScriptLib 子项目。

请通过主工程获取完整代码：

```bash
# 克隆主工程并同时拉取全部子模块
git clone --recursive https://github.com/zhenshiz/ViScriptLib.git
cd ViScriptLib

# 已克隆过主工程时，可手动拉取/更新子模块
git submodule update --init --recursive
```

常用命令（均在主工程根目录执行，需要 Java 21）：

| 命令 | 作用 |
| --- | --- |
| `./gradlew :ViScriptQuests:build` | 单独构建本模组 |
| `./gradlew :ViScriptQuests:runClient` | 启动客户端调试本模组 |
| `./gradlew buildAll` | 构建所有子项目，产物在主工程根目录 `build/libs` |
| `./gradlew cleanLibs` | 清理所有子项目的构建产物 |

对本仓库的修改需要以子模块的形式提交并推送到本仓库；随后在主工程中再提交一次指向新 commit 的子模块引用。

## 当前状态

ViScriptQuests 仍处于开发中。运行时任务、任务书、HUD 追踪、蓝图编译和多种目标/奖励已经具备基础可用能力；后续仍可能继续完善蓝图子图编译、更多运行时事件、脚本 API 和编辑器体验。
