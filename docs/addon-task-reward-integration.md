# ViScriptQuests 目标/奖励扩展接入说明

本文给附属模组说明：如果要给 ViScriptQuests 新增一种任务目标或奖励，需要注册哪些内容，以及这些内容在蓝图编辑器和运行时里分别承担什么职责。

当前任务系统分成两层：

- **运行时数据层**：`.quest` 文件里真正保存和执行的 `ITask` / `IReward`。
- **蓝图编辑层**：编辑器里能拖出来的节点，以及把节点编译成运行时数据的 compiler。

如果只想通过代码或数据生成 `.quest` 文件，可以只接入运行时数据层。  
如果希望作者在任务蓝图编辑器里可视化创建这个目标/奖励，就还需要注册蓝图节点和节点 compiler。

## 目标扩展清单

新增一个目标通常需要 3 个类：

1. 运行时目标类：继承 `ITask`，用 `@LDLRegister` 注册到 `ITask.ID`。
2. 蓝图目标节点：继承 `QuestBlueprintNode` 或 LDLib2 `Node`，用 `@NodeAttribute` 绑定到 `QuestBlueprintGraph`。
3. 目标节点编译器：实现 `IQuestTaskNodeCompiler`，用 `@LDLRegister` 注册到 `IQuestTaskNodeCompiler.ID`。

### 1. 运行时目标类

运行时目标类是真正写进 `.quest` 文件、随任务进度执行的对象。

```java
@LDLRegister(name = "my_task", registry = ITask.ID)
public class MyTask extends ITask {
    @Persisted
    public String value = "";

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return false;
    }

    @Override
    public boolean onComplete(ServerPlayer player) {
        return true;
    }

    @Override
    public Component getTaskHint() {
        return Component.translatable("mymod.task_hint.my_task", value);
    }
}
```

关键点：

- `@LDLRegister(name = "...", registry = ITask.ID)` 是必须的，`name` 是稳定序列化 ID，保存到任务文件后不要随便改。
- 可保存字段用 `@Persisted`，保持可变、无参构造。没有显式构造函数时 Java 会自动提供无参构造。
- `ITask` 已经通过 ViScriptQuests 注册了多态 `CustomDirectAccessor`，普通目标子类不需要再给自己单独注册 accessor。
- 如果目标字段里放了自己的复杂数据类，这个复杂数据类仍然需要在附属模组里注册 LDLib2 `CustomDirectAccessor`。

常用方法含义：

- `checkCompletion(ServerPlayer)`：检查玩家当前状态是否满足目标。
- `onComplete(ServerPlayer)`：目标完成时执行副作用，比如扣物品。返回 `false` 会阻止完成。
- `allowsAutoSubmit()`：是否允许自动提交。默认 `true`。
- `getTaskHint()`：任务书/HUD 展示文本。
- `getDisplayIcon()`：任务书/HUD 展示图标。
- `getRequiredAmount()`：目标需要的数量，默认 `1`。
- `refreshObjectiveProgress(...)`：刷新 UI 进度，不应该发奖励、扣物品、推进流程。
- `refreshesProgressFromPlayerState()`：目标进度是否能从玩家当前状态重算。击杀、对话这类事件累计目标通常返回 `false`。
- `submitObjective(...)`：手动提交目标时执行。
- `getGuideMarker(...)`：需要 HUD 导航点时覆盖，比如位置目标。

## 运行时目标进度接入

目标真正运行时，玩家进度保存在这条链路里：

```text
QuestSavedData
  -> QuestPlayerData
    -> PlayerQuestState
      -> TaskProgress
        -> TaskObjectiveProgress
```

其中：

- `PlayerQuestState` 表示某个玩家身上的一个大任务。
- `TaskProgress` 表示一个小任务节点的运行时状态，字段 `status` 决定它是 `LOCKED`、`ACTIVE`、`COMPLETED` 还是 `SKIPPED`。
- `TaskObjectiveProgress` 表示小任务里的单个目标进度，字段 `currentAmount`、`requiredAmount`、`completed` 是目标进度核心。

附属模组一般不应该直接把 `TaskProgress.status` 改成 `COMPLETED`，也不应该自己发奖励或推进流程。  
正确做法是：只让目标自己的逻辑更新目标进度，后面的保存、奖励、流程推进、HUD 刷新交给 `QuestSubmissionService`。

### 状态型目标：从玩家当前状态重算

物品数量、玩家位置、经验等级这类目标，可以通过玩家当前状态直接判断是否完成。  
这类目标的运行时逻辑应该写在目标类里：

- `checkCompletion(player)` 判断是否满足。
- `onComplete(player)` 执行完成副作用，比如扣物品。
- `refreshObjectiveProgress(player, progress)` 只刷新 UI 进度。
- `refreshesProgressFromPlayerState()` 保持默认 `true`。

然后在合适的服务端事件里调用标准提交入口即可：

```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
        return;
    }
    if (player.level().getGameTime() % 20 != 0) {
        return;
    }
    QuestSubmissionService.submitActiveTasks(player, MyItemTask.class);
}
```

这会做这些事：

1. 遍历玩家所有 `ACTIVE` 大任务。
2. 找到包含 `MyItemTask` 的 `ACTIVE` 小任务。
3. 调用标准提交流程刷新 `TaskObjectiveProgress`。
4. 如果目标完成，调用目标的 `onComplete(...)`。
5. 如果小任务下所有目标都完成，自动发放小任务奖励并推进蓝图流程。
6. 标记 `QuestSavedData#setDirty()`，刷新任务追踪 HUD，并同步队伍任务状态。

内置 `ItemTask` 当前的 tick 入口在 `ItemEvents` 里，它调用的是 `QuestManager.submitTracked(player)`，只自动检查玩家当前追踪的小任务。附属模组如果希望检查自己类型的所有激活目标，可以用 `submitActiveTasks(player, MyTask.class)`。

### 事件累计型目标：只在事件发生时累加

击杀数量、对话次数、打开方块次数这类目标不能从玩家当前状态反推，应该：

- `refreshesProgressFromPlayerState()` 返回 `false`。
- `checkCompletion(player)` 通常不要用来累计，事件发生时再加进度。
- 在事件监听里调用 `QuestSubmissionService.recordTaskProgress(...)`。

示例：

```java
@SubscribeEvent
public static void onSomeEvent(MyEvent event) {
    if (!(event.getPlayer() instanceof ServerPlayer player)) {
        return;
    }
    QuestSubmissionService.recordTaskProgress(player, MyCounterTask.class, (p, task, objective) -> {
        if (!task.matches(event)) {
            return false;
        }
        if (objective.completed) {
            return false;
        }
        int required = Math.max(1, task.getRequiredAmount());
        objective.requiredAmount = required;
        objective.currentAmount = Math.min(required, Math.max(0, objective.currentAmount) + 1);
        objective.completed = objective.currentAmount >= required;
        return true;
    });
}
```

`recordTaskProgress(...)` 会负责这些后续逻辑：

- 遍历玩家所有激活任务和激活小任务。
- 只匹配指定 `taskType` 的目标。
- 在调用 recorder 前同步目标展示结构，保证 `objectives` 数量、提示文本、图标、需求量是最新的。
- recorder 返回 `true` 后，如果小任务还没全部完成，只保存数据、刷新 HUD、同步队伍状态。
- 如果小任务所有目标都完成，会设置 `TaskProgress.status = COMPLETED`，发放小任务奖励，调用 `QuestFlowExecutor.completeStepNode(...)` 推进流程，再刷新追踪。

所以联动方的 recorder 只应该改当前 `TaskObjectiveProgress`，一般只改：

- `requiredAmount`
- `currentAmount`
- `completed`

不要在 recorder 里直接发奖励、推进流程、修改 `TaskProgress.status`、清理 `activeFlowNodes`，这些由主任务系统统一处理。

### 2. 蓝图目标节点

目标节点是编辑器里的配置入口。目标/奖励节点属于小任务内容图，正常应放在 `SubQuest` 节点内部，不要在目标节点里再定义自己的 `step_id`。

```java
@NodeAttribute(
        name = "my_task",
        group = QuestBlueprintNode.TASK_GROUP,
        graphTypes = QuestBlueprintGraph.class
)
public class MyTaskNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.blueprint.node.my_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "value", "");
        intOption(context, "amount", 1);
    }
}
```

关键点：

- `@NodeAttribute(... graphTypes = QuestBlueprintGraph.class)` 让 LDLib2 自动把节点注册到任务蓝图图类型。
- `group = QuestBlueprintNode.TASK_GROUP` 会让节点出现在小任务内容图的“任务目标”分类里。
- 节点库显示名目前按 `viscript_quests.blueprint.node.<node_id>` 查翻译键，所以附属模组需要提供这个翻译键。
- 使用 `QuestBlueprintNode` 的便捷方法时，端口/选项显示名也会使用 `viscript_quests.blueprint.port.<option_id>`。
- 如果不想使用 `viscript_quests.*` 翻译键，可以自己直接调用 LDLib2 的 `context.addOption(...)` 并设置自己的 `Component.translatable("mymod...")`，但节点库分类名仍受当前 ViScriptQuests 节点库规则影响。

常用内置选项工具：

- `stringOption(...)`
- `intOption(...)`
- `floatOption(...)`
- `boolOption(...)`
- `itemStackOption(...)`
- `displayIconOption(...)`
- `dimensionOption(...)`
- `entityTypeOption(...)`
- `enumOption(...)`

如果节点需要自定义类型，优先使用 LDLib2 `TypeHandleHelpers` 注册默认值、图标和 configurator。这个类型如果要被持久化或通过 RPC 传输，也要注册 `CustomDirectAccessor`。

### 3. 目标节点编译器

节点 compiler 负责把编辑器节点里的选项读出来，生成真正的 `ITask`。

```java
@LDLRegister(name = "my_task", registry = IQuestTaskNodeCompiler.ID)
public class MyTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof MyTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        MyTask task = new MyTask();
        task.stepId = stepId;
        task.value = context.getString(node, "value");
        return task;
    }
}
```

关键点：

- `@LDLRegister(name = "...", registry = IQuestTaskNodeCompiler.ID)` 是必须的。
- `supports(...)` 应该只匹配自己的节点类。
- `compileTask(...)` 的 `stepId` 由外层 `SubQuest` 注入，必须写入 `task.stepId`。
- compiler 注册名不强制和运行时目标 ID 相同，但强烈建议保持一致，方便排查和迁移。

`QuestCompileContext` 常用读取方法：

- `getString(node, optionId)`
- `getBool(node, optionId)`
- `getInt(node, optionId)`
- `getFloat(node, optionId)`
- `getItemStack(node, optionId)`
- `getDisplayIcon(node, optionId)`
- `getSubmitMode(node, optionId)`

## 奖励扩展清单

新增一个奖励同样通常需要 3 个类：

1. 运行时奖励类：继承 `IReward`，用 `@LDLRegister` 注册到 `IReward.ID`。
2. 蓝图奖励节点：用 `@NodeAttribute` 绑定到 `QuestBlueprintGraph`，分组用 `QuestBlueprintNode.REWARD_GROUP`。
3. 奖励节点编译器：实现 `IQuestRewardNodeCompiler`，注册到 `IQuestRewardNodeCompiler.ID`。

### 1. 运行时奖励类

```java
@LDLRegister(name = "my_reward", registry = IReward.ID)
public class MyReward extends IReward {
    @Persisted
    public int amount = 1;

    @Override
    public void grant(ServerPlayer player) {
        // 在这里发放奖励
    }

    @Override
    public Component getRewardHint() {
        return Component.translatable("mymod.reward_hint.my_reward", amount);
    }

    @Override
    public DisplayIcon getRewardIcon() {
        return new DisplayIcon();
    }
}
```

关键点：

- `@LDLRegister(name = "...", registry = IReward.ID)` 是必须的。
- 可保存字段用 `@Persisted`。
- `grant(ServerPlayer)` 是真正发奖励的位置，只会在运行时小任务/大任务结算时调用。
- `stepId` 是 `IReward` 基类字段，compiler 会根据所在小任务写入。
- `teamLeaderOnly` 也是 `IReward` 基类字段。如果奖励希望支持 ViScriptTeam 队长领取规则，可以在节点里暴露选项并在 compiler 里写入。

### 2. 蓝图奖励节点

```java
@NodeAttribute(
        name = "my_reward",
        group = QuestBlueprintNode.REWARD_GROUP,
        graphTypes = QuestBlueprintGraph.class
)
public class MyRewardNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return Component.translatable("viscript_quests.blueprint.node.my_reward");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        intOption(context, "amount", 1);
        boolOption(context, "team_leader_only", false);
    }
}
```

奖励节点同样应该放在 `SubQuest` 节点内部。编译时系统会把所属小任务的 `stepId` 传给 compiler。

### 3. 奖励节点编译器

```java
@LDLRegister(name = "my_reward", registry = IQuestRewardNodeCompiler.ID)
public class MyRewardNodeCompiler implements IQuestRewardNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof MyRewardNode;
    }

    @Override
    public IReward compileReward(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        MyReward reward = new MyReward();
        reward.stepId = stepId;
        reward.amount = context.getInt(node, "amount");
        reward.teamLeaderOnly = context.getBool(node, "team_leader_only");
        return reward;
    }
}
```

## 翻译键

不要在节点、任务提示、奖励提示里硬编码给玩家看的文本。附属模组需要在自己的语言文件里补翻译键。

当前节点库默认会查这些键：

```json
{
  "viscript_quests.blueprint.node.my_task": "我的目标",
  "viscript_quests.blueprint.node.my_reward": "我的奖励",
  "viscript_quests.blueprint.port.value": "参数",
  "viscript_quests.blueprint.port.amount": "数量",
  "viscript_quests.blueprint.port.team_leader_only": "只发给队长",
  "mymod.task_hint.my_task": "完成目标：%s",
  "mymod.reward_hint.my_reward": "获得奖励：%s"
}
```

如果新增了 `task` / `reward` 之外的节点分组，还要提供：

```json
{
  "viscript_quests.blueprint.category.my_group": "我的分类"
}
```

## 注册 ID 汇总

| 用途 | 注册入口 | 注解示例 |
| --- | --- | --- |
| 运行时目标 | `ITask.ID` = `viscript_quests:task` | `@LDLRegister(name = "my_task", registry = ITask.ID)` |
| 运行时奖励 | `IReward.ID` = `viscript_quests:reward` | `@LDLRegister(name = "my_reward", registry = IReward.ID)` |
| 目标节点 compiler | `IQuestTaskNodeCompiler.ID` = `viscript_quests:blueprint_task_node_compiler` | `@LDLRegister(name = "my_task", registry = IQuestTaskNodeCompiler.ID)` |
| 奖励节点 compiler | `IQuestRewardNodeCompiler.ID` = `viscript_quests:blueprint_reward_node_compiler` | `@LDLRegister(name = "my_reward", registry = IQuestRewardNodeCompiler.ID)` |
| 蓝图节点 | `QuestBlueprintGraph.class` | `@NodeAttribute(name = "my_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)` |

## 附属模组依赖建议

附属模组需要在运行时依赖 ViScriptQuests 和 LDLib2。`neoforge.mods.toml` 里建议声明对 `viscript_quests` 的依赖，确保注册顺序和类可用。

```toml
[[dependencies."mymod"]]
modId = "viscript_quests"
type = "required"
ordering = "AFTER"
side = "BOTH"
```

如果附属模组只在服务端提供运行时目标/奖励，但不提供编辑器节点，也仍然建议 `side = "BOTH"`，因为任务文件和注册表在客户端任务书同步、图标展示时也可能需要解析对应类型。

## 最小验证流程

1. 启动客户端，打开任务编辑器。
2. 创建或打开一个任务项目。
3. 在 `SubQuest` 节点内部确认新目标/奖励节点能出现在节点库里。
4. 填写参数并导出 `.quest`。
5. 用命令发放任务，确认任务书能显示提示文本、图标和进度。
6. 完成目标，确认奖励发放、流程推进、任务追踪 HUD 刷新都正常。

如果导出时报“缺少目标”“找不到 compiler”之类的问题，优先检查：

- 节点是否用了 `graphTypes = QuestBlueprintGraph.class`。
- 目标/奖励节点是否放在 `SubQuest` 内容图里。
- compiler 是否注册到了正确的 `IQuestTaskNodeCompiler.ID` / `IQuestRewardNodeCompiler.ID`。
- `supports(...)` 是否能匹配自己的节点类。
- 运行时类型是否注册到了 `ITask.ID` / `IReward.ID`。
- 语言文件是否提供了节点和端口翻译键。
