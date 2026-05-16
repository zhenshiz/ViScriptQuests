# ViScriptQuests

服务于 `ViScriptNpc` 的 RPG 任务系统。

核心目标：

- 一个 RPG 地图只使用一个任务书 UI
- 任务书按分类展示任务
- 一个“任务”表示一整段剧情或一整条主线/支线
- 任务内部通过蓝图控制多个“小任务/目标”的出现、完成、分支和奖励
- 支持通过蓝图变量控制剧情走向，例如 NPC 好感度决定 `bad end / he`
- 支持命令直接给玩家颁布任务、完成任务、完成某个小任务
- 首版先只做“物品目标 + 物品奖励”用于测试

---

## 一、设计定位

这个模组不是 `FTB Quests` 那种“把所有任务都预先铺在书里”的目录型任务系统。

它更接近传统 RPG 的任务推进方式：

- 玩家当前只会得到当前阶段需要知道的任务
- 主线任务通常是一个接一个推进
- 一个任务内部可能包含多个运行时出现的小目标
- 后续会出现什么目标，由蓝图逻辑和变量决定，而不是提前静态列出

因此：

- 不做任务书本体类
- 不做顶层 `dependencies`
- 不做顶层 `visibility`
- 不做 `QuestContentType` 枚举
- 不做 `tags`
- 不做顶层 `acceptPolicy`
- 不做顶层 `display`

这些逻辑要么由蓝图直接控制，要么由配置文件控制。

---

## 二、LDLib2 约束

当前项目依赖：

- `Minecraft 1.21.1`
- `ldlib2 2.2.6`

当前蓝图系统实际是：

- `com.lowdragmc.lowdraglib2.nodegraphtookit`

它适合承载：

- 任务逻辑图编辑
- 节点参数配置
- 黑板变量
- 常量节点
- 分支逻辑
- 图资源导出为 `CompoundTag`

它当前不适合作为首版核心依赖的能力：

- 开箱即用的完整子图复用
- 开箱即用的图级输入输出接口

因此 ViScriptQuests 首版的单位应该是：

- 一个任务文件 = 一个完整任务 = 一张蓝图图

而不是：

- 一个任务文件里再去引用另一个单独蓝图文件

---

## 三、任务书 UI 方向

任务书 UI 参考当前确认方向：

- 左上：任务分类列表
- 左中：当前分类下已获得的任务列表
- 右侧：当前选中任务的标题、描述、奖励

注意：

- 常规任务书 UI 不展示小任务 / objective 进度列表
- 小任务由蓝图在运行时控制，但不作为首版常规任务书展示内容
- 这样可以避免把未来分支目标、动态目标、隐藏目标错误地提前暴露给玩家
- objective 仍然需要存在于运行时数据中，用于蓝图推进、命令调试和任务结算

---

## 四、全局配置

不需要任务书定义类。

任务书相关的固定信息直接放配置文件。

### QuestSystemConfig

建议当前只保留：

- `double questHudX`
  - 任务 HUD 距离左侧的百分比
- `double questHudY`
  - 任务 HUD 距离上侧的百分比
- `List<QuestCategoryConfig> categories`
  - 任务分类数组

### 说明

当前不需要：

- `defaultTrackedQuestCount`
- `rememberLastCategory`
- `showCategoryProgress`

原因：

- HUD 尽量简洁，只展示当前执行任务标题
- 分类完成度意义不大，任务数量会动态增加
- 是否记忆分类不是当前核心需求

---

## 五、任务分类配置

分类不在蓝图编辑器里编辑。

分类应该直接写在配置文件数组里，因为：

- 分类是长期稳定内容
- 不需要在游戏过程中动态变化
- 顺序由数组本身决定，不需要额外 `order`

### QuestCategoryConfig

当前建议只保留 3 个参数：

- `String categoryId`
  - 分类唯一 id
- `IconTexture icon`
  - 分类图标
- `String tooltip`
  - 鼠标悬停在图标上时的介绍文本，例如“主线任务”

### 分类顺序

分类顺序由配置数组顺序决定，不需要：

- `order`

---

## 六、任务文件本体

一个任务文件就是导出的最终任务文件，里面同时包含：

- 当前任务的基础信息
- 当前任务的蓝图图数据
- 当前任务的蓝图变量定义
- 当前任务的可读语义索引

不再拆成“任务定义引用蓝图”。

### QuestFile

建议参数：

- `String questId`
  - 任务唯一 id
- `String categoryId`
  - 所属分类 id
- `String name`
  - 任务名
- `String subTitle`
  - 副标题，可选
- `String description`
  - 当前任务的主描述
- `IconTexture icon`
  - 任务图标
- `QuestGraphMeta graphMeta`
  - 图元信息
- `CompoundTag graphNbt`
  - 蓝图原始导出数据
- `QuestGraphSemanticIndex semanticIndex`
  - 便于命令和调试的可读摘要

### QuestGraphMeta

建议参数：

- `String graphVersion`
  - 图结构版本
- `long lastModified`
  - 最后编辑时间
- `String author`
  - 作者，可选

### QuestGraphSemanticIndex

这是为了让命令和调试更容易读。

建议参数：

- `Map<String, QuestNodeSnapshot> nodes`
  - 节点快照
- `Map<String, QuestVariableDescriptor> variables`
  - 图中声明的变量描述
- `List<QuestFlowEdgeSnapshot> edges`
  - 主要流程边

### QuestNodeSnapshot

建议参数：

- `String nodeId`
- `String nodeType`
- `String nodeName`
- `String displayText`
  - 节点语义摘要，例如“检查玩家是否持有铁剑”

### QuestFlowEdgeSnapshot

- `String fromNodeId`
- `String fromPortId`
- `String toNodeId`
- `String toPortId`
- `String edgeType`

---

## 七、主任务与小任务的关系

这里正式定义两个层次：

### 7.1 主任务 Quest

主任务表示一整段剧情或一整条任务线。

例如：

- 去村长家接任务
- 修好装备并进入森林
- 追查神达三三事件

主任务负责：

- 在任务书左中列表里展示
- 在右侧显示标题、描述、奖励
- 作为蓝图运行的容器

### 7.2 小任务 / 当前目标 QuestObjective

小任务不是独立任务文件，而是主任务内部运行时生成的目标对象。

例如：

- 去某个地点
- 与某个 NPC 交互
- 获取某个物品
- 把某个物品提交给 NPC

这些小任务之间的逻辑：

- 由蓝图决定
- 可以线性推进
- 可以并行出现
- 可以由变量控制是否出现

所以小任务不是静态脚本列表，而是蓝图运行时产物。

---

## 八、QuestObjective 运行时模型

小任务必须有稳定 id，因为要支持命令直接完成某个小任务。

### QuestObjective

建议参数：

- `String objectiveId`
  - 小任务唯一 id
  - 默认可以自动生成 UUID
  - 开发者后续可手动修改
- `String title`
  - 当前目标标题
- `String description`
  - 当前目标描述
- `QuestObjectiveType type`
  - 目标类型
- `QuestObjectiveState state`
  - 当前状态
- `String sourceNodeId`
  - 来源蓝图节点 id
- `Map<String, String> displayArgs`
  - 运行时展示参数

### QuestObjectiveType

首版建议先只做：

- `ITEM`

后续再扩展：

- `INTERACT`
- `LOCATION`
- `KILL`
- `CUSTOM`

### QuestObjectiveState

运行时内部建议保留：

- `ACTIVE`
- `COMPLETED`
- `FAILED`
- `HIDDEN`

任务书首版默认不展示 objective 列表。

是否保留已完成 objective 历史，仅作为运行时与调试层需求，不作为首版 UI 强需求。

### 多个小任务同时激活

这是允许的。

如果蓝图里一个节点后面激活了多个目标节点，那么运行时就应该允许：

- 一个主任务同时存在多个 `ACTIVE` objective

这不需要在任务根参数里单独开关，直接由蓝图逻辑决定。

---

## 九、变量设计

变量是剧情分支的核心，例如：

- NPC 好感度
- 某个剧情旗标
- 玩家是否拿过某个关键物品
- 当前剧情分支是否进入 `bad end`

### QuestVariableDescriptor

这是蓝图黑板里声明的变量定义。

建议参数：

- `String name`
- `String displayName`
- `QuestValueType valueType`
- `QuestVariableAccess access`
- `String defaultValueNbt`
  - 默认值，保存为结构化 NBT 或 SNBT
- `String tooltip`
- `boolean persistent`

### QuestVariableAccess

业务层建议定义：

- `READ_ONLY`
- `WRITE_ONLY`
- `READ_WRITE`

但实现时要注意：

- `ldlib2 2.2.6` 里的 `READ_WRITE` 变量节点体验并不天然完整
- 更适合做成显式 getter / setter 节点

### QuestValueType

首版建议支持：

- `BOOL`
- `INT`
- `LONG`
- `FLOAT`
- `DOUBLE`
- `STRING`
- `ITEM_STACK`
- `UUID`
- `COMPOUND`

对于当前测试阶段，优先保证：

- `BOOL`
- `INT`
- `STRING`
- `ITEM_STACK`

### 变量持久化

运行时建议统一保存：

- `type`
- `value`
- `dirty`
- `lastModified`

例如：

```nbt
{
  variables: {
    "affection": {
      type: "INT",
      value: 15,
      dirty: 1b,
      lastModified: 1234999L
    },
    "bad_end_locked": {
      type: "BOOL",
      value: 0b,
      dirty: 0b,
      lastModified: 1235000L
    }
  }
}
```

这样做的好处：

- 命令可读
- 蓝图变量和运行时变量能对应
- 以后迁移方便

---

## 十、提交方式

提交方式是需要的，但不应该做成任务根字段，而应该由蓝图里的提交节点控制。

当前确认支持 3 种：

- 自动提交
- 手动提交
- 指令提交

### 10.1 自动提交

蓝图监听条件成立后：

- 自动扣除需要提交的物品
- 自动发放奖励
- 自动推进后续逻辑

### 10.2 手动提交

玩家打开任务书后手动点击完成。

在点击时：

- 校验条件
- 扣除需要提交的物品
- 发放奖励
- 推进蓝图

### 10.3 指令提交

为了兼容性和调试，必须支持指令直接完成某个任务或某个小任务。

建议后续支持类似：

- `/vsq quest grant <player> <questId>`
- `/vsq quest complete <player> <questId>`
- `/vsq objective complete <player> <questId> <objectiveId>`

其中：

- `objectiveId` 直接来自运行时 objective 的稳定 id

---

## 十一、奖励设计

奖励也不建议先做成任务根字段统一管理，而是由蓝图奖励节点决定。

因为：

- 你明确希望小任务完成时立即发奖励
- 不希望所有奖励都等整个主任务结算后统一发

所以奖励模型应该允许：

- 某个小任务完成后立刻发奖励
- 某个主任务结束时再发总结奖励

### 首版奖励范围

首版先只做：

- `ItemReward`

### ItemReward

建议参数：

- `ItemStack itemStack`

首版不额外拆分：

- 数量
- NBT
- 组件

因为这些都已经包含在 `ItemStack` 里。

---

## 十二、物品目标设计

首版测试目标也先只做：

- `ItemObjective`

### ItemObjective

建议参数：

- `String objectiveId`
- `String title`
- `String description`
- `ItemStack itemStack`
- `boolean strictComponents`

### 说明

这里的 `ItemObjective` 只负责“检测”或“匹配”。

至于：

- 是否在提交时扣除物品
- 什么时候扣除
- 自动提交还是手动提交

这些行为不放在 `ItemObjective` 参数里，而是放在蓝图的提交节点里处理。

这样设计更适合蓝图：

- 一个目标节点负责监听玩家是否拥有该物品
- 一个提交节点负责决定是否消耗该物品并发奖

---

## 十三、运行时任务状态

### PlayerQuestState

每个玩家对每个主任务都有一份运行时状态。

建议参数：

- `String questId`
- `QuestState state`
- `long acceptedTime`
- `long completedTime`
- `boolean tracked`
- `Map<String, QuestRuntimeValue> variables`
- `Map<String, QuestObjective> objectives`
- `List<QuestExecutionTrace> traces`

### QuestState

当前建议保留：

- `ACTIVE`
- `COMPLETED`
- `FAILED`

如果后续需要，再扩展：

- `AVAILABLE`
- `ABANDONED`
- `COOLDOWN`

### QuestRuntimeValue

- `String type`
- `Tag value`
- `boolean dirty`
- `long lastModified`

### QuestExecutionTrace

为了调试蓝图逻辑，需要保留执行轨迹。

建议参数：

- `long time`
- `String nodeId`
- `String nodeType`
- `String summary`
- `String result`

用途：

- 命令调试
- 玩家任务异常排查
- 蓝图逻辑验证

---

## 十四、命令层需求

命令不是附属功能，而是首版必要功能。

原因：

- 任务发放靠指令
- 任务完成调试靠指令
- 小任务强制结算靠指令

建议至少支持：

- `grant quest`
- `complete quest`
- `complete objective`
- `inspect quest state`
- `inspect variables`

命令展示必须能稳定读出：

- 当前主任务状态
- 当前 active objectives
- 当前变量值
- 最近执行轨迹

所以任务文件中保留：

- 稳定的 `questId`
- 稳定的 `objectiveId`
- 稳定的 `nodeId`

是必须的。

---

## 十五、当前首版最小实现范围

为了尽快跑通系统，当前首版收敛为：

1. 配置文件里维护分类数组
2. 一个任务文件就是一个完整任务蓝图文件
3. 任务支持蓝图变量
4. 小任务作为运行时 objective 由蓝图激活
5. 运行时支持多个 active objectives
6. 支持自动提交、手动提交、指令提交
7. 首版只做 `ItemObjective`
8. 首版只做 `ItemReward`
9. HUD 只显示当前主任务标题
10. 任务书 UI 采用“左侧列表 + 右侧详情”，不展示 objective 进度列表

---

## 十六、结论

ViScriptQuests 当前确认的数据方向不是：

- `QuestStep -> transition -> reward`

而是：

- `QuestSystemConfig(categories + hud)`
- `QuestFile(meta + graphNbt + semanticIndex)`
- `PlayerQuestState(variables + objectives + traces)`

其中：

- 分类由配置文件维护
- 主任务由任务文件定义
- 小任务由蓝图在运行时生成
- 提交和奖励由蓝图节点控制
- 变量负责推动剧情分支和结局变化

这套结构更符合：

- 你想要的 RPG 推进方式
- `ldlib2 2.2.6` 当前蓝图能力
- 后续命令调试与 NBT 持久化需求

---

## 十七、正式任务系统蓝图需求

这一节作为从学习蓝图切换到正式任务系统后的实现基准。

### 17.1 核心原则

- 任务蓝图不是通用脚本编辑器，而是 RPG 任务编辑器
- 蓝图中的事件节点用于声明任务响应什么外部事件
- 指令里的事件触发只用于调试，不作为正式运行方式
- 一个任务文件就是一个主任务
- 一个主任务内部可以通过蓝图激活多个运行时目标
- 首版不追求覆盖所有 RPG 功能，先跑通物品目标、物品提交、物品奖励、命令发放、状态保存

### 17.2 编辑流程

地图作者的主要编辑流程应该是：

1. 在任务编辑器中新建任务
2. 编辑任务基础信息
3. 在同一个编辑器中打开任务蓝图
4. 通过任务专用节点编排事件、目标、提交、奖励、分支
5. 导出 `.quest` 任务文件
6. 在游戏中通过指令给玩家发放任务
7. 任务运行时由事件监听自动触发蓝图逻辑

编辑器不应该要求作者理解通用蓝图 VM。

编辑器应该围绕任务语义组织节点，例如：

- 任务事件
- 目标
- 条件
- 提交
- 奖励
- 状态
- 命令
- 调试

### 17.3 任务文件

正式任务文件建议保存为：

```nbt
{
  version: 1,
  quest: {
    questId: "main_001",
    categoryId: "main",
    title: "离乡旅人",
    subtitle: "神达之旅",
    description: "...",
    icon: {...}
  },
  graph: {...},
  index: {...}
}
```

其中：

- `quest` 是任务书和命令可直接读取的基础信息
- `graph` 是 LDLib2 蓝图数据
- `index` 是可选的语义索引，用于调试和命令补全

不要再设计单独的 `blueprint reference` 字段。

### 17.4 任务分类

分类仍然不放进任务文件中编辑。

分类来自配置文件：

- `categoryId`
- `icon`
- `tooltip`

任务文件只通过 `categoryId` 归属分类。

### 17.5 玩家任务状态

玩家运行时状态必须和任务文件分离。

建议保存到 SavedData：

```nbt
{
  players: {
    "<playerUuid>": {
      quests: {
        "main_001": {
          state: "ACTIVE",
          tracked: 1b,
          variables: {...},
          objectives: {...},
          traces: [...]
        }
      }
    }
  }
}
```

后续多人共享任务时，可以把作用域从玩家切到队伍：

```nbt
{
  teams: {
    "<teamId>": {
      quests: {...}
    }
  }
}
```

首版可以先实现玩家作用域，但数据结构要避免把进度写死成只能单人。

### 17.6 正式任务蓝图节点范围

首版任务蓝图节点建议只做这些。

事件节点：

- `当任务被发放`
- `当玩家获得物品`
- `当玩家打开任务书`
- `当玩家手动提交目标`
- `当指令提交目标`

目标节点：

- `创建物品目标`
- `完成目标`
- `失败目标`
- `隐藏目标`

条件节点：

- `拥有物品`
- `目标是否激活`
- `目标是否完成`
- `任务变量比较`

提交节点：

- `自动提交物品`
- `手动提交物品`
- `指令提交入口`

奖励节点：

- `给予物品奖励`
- `执行奖励命令`

任务状态节点：

- `完成任务`
- `失败任务`
- `追踪任务`
- `取消追踪任务`

变量节点：

- 优先使用 LDLib2 Blackboard 变量
- 只为任务进度提供必要的持久变量读写节点
- 不提供通用实体变量、列表变量、Object 变量等复杂节点

调试节点：

- `打印任务日志`
- `打印给玩家`

### 17.7 首版物品目标

物品目标参数：

- `objectiveId`
- `title`
- `description`
- `ItemStack itemStack`
- `boolean strictComponents`

行为不放进物品目标本身。

是否扣除物品、是否自动提交、奖励什么时候发，由提交节点和奖励节点决定。

### 17.8 提交方式

首版保留三种提交入口：

- 自动提交：事件触发后蓝图自行判断条件并结算
- 手动提交：任务书按钮触发提交事件
- 指令提交：命令触发提交事件，用于兼容和调试

注意：

- 提交入口是事件，不是任务根字段
- 蓝图决定提交后接什么奖励和后续目标
- 指令提交不应该绕过蓝图，除非是明确的管理员强制命令

### 17.9 任务书 UI 需求

首版任务书只需要：

- 左侧分类
- 左侧任务列表
- 右侧任务标题
- 右侧任务描述
- 右侧奖励预览
- 手动提交按钮

首版默认不展示小任务列表。

原因：

- 小任务可能是隐藏的、分支的、临时的
- 展示所有 objective 会破坏 RPG 剧情节奏
- HUD 和任务书都应该尽量少暴露未来信息

### 17.10 命令需求

首版命令至少需要：

- `grant <player> <questId>`
- `revoke <player> <questId>`
- `complete <player> <questId>`
- `submit <player> <questId> <objectiveId>`
- `track <player> <questId>`
- `inspect <player> <questId>`
- `reload`

其中：

- `grant` 触发任务蓝图的“任务被发放”事件
- `submit` 触发任务蓝图的“指令提交目标”事件
- `complete` 是管理员强制完成任务
- `inspect` 用于查看变量、目标和最近执行轨迹

### 17.11 验收标准

首个正式版本完成时，至少要能验证：

1. 配置文件里能定义任务分类
2. 编辑器能创建一个任务文件并导出
3. 指令能给玩家发放任务
4. 发放任务后任务进入对应分类
5. 蓝图能创建一个物品目标
6. 玩家拥有物品后能自动提交
7. 玩家点击任务书按钮能手动提交
8. 指令能提交指定 objective
9. 物品奖励能发放
10. 玩家退出重进后任务状态不丢失

如果这 10 条没跑通，不继续扩展复杂节点。
