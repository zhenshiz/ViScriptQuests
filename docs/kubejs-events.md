# KubeJS 与 NeoForge 任务事件

ViScriptQuests 先通过 NeoForge 事件总线发布任务事件，再由可选的 KubeJS 桥接层包装为脚本事件。没有安装 KubeJS 时，NeoForge 任务事件仍然正常工作，任务运行时不会加载任何 KubeJS 类。

## 事件列表

KubeJS 服务端脚本可以监听以下事件：

- `ViScriptQuestsEvents.questStarted`
- `ViScriptQuestsEvents.questCompleted`
- `ViScriptQuestsEvents.questFailed`
- `ViScriptQuestsEvents.questRevoked`
- `ViScriptQuestsEvents.taskStarted`
- `ViScriptQuestsEvents.taskCompleted`
- `ViScriptQuestsEvents.taskFailed`
- `ViScriptQuestsEvents.taskSkipped`
- `ViScriptQuestsEvents.objectiveProgress`
- `ViScriptQuestsEvents.objectiveCompleted`
- `ViScriptQuestsEvents.rewardGranted`

这些事件是状态变化后的通知事件，不支持取消。

## 精确监听目标

所有事件都可以不填写目标以监听全部，也可以传入目标字符串精确监听：

| 层级 | 目标格式 |
|---|---|
| 大任务 | `questId` |
| 小任务 | `questId/stepId` |
| 具名目标 | `questId/stepId/objectiveId` |
| 无 ID 目标 | `questId/stepId/@objectiveIndex` |

推荐使用工具方法生成目标，避免手动拼接：

```js
const questTarget = ViScriptQuestsUtil.questTarget('example_quest')
const taskTarget = ViScriptQuestsUtil.taskTarget('example_quest', 'collect_dirt')
const objectiveTarget = ViScriptQuestsUtil.objectiveTarget(
    'example_quest',
    'collect_dirt',
    'dirt_objective'
)
```

精确监听某个小任务开始：

```js
ViScriptQuestsEvents.taskStarted(
    ViScriptQuestsUtil.taskTarget('example_quest', 'collect_dirt'),
    event => {
        event.player.tell('收集泥土的小任务已经开始')
    }
)
```

精确监听某个目标完成：

```js
ViScriptQuestsEvents.objectiveCompleted(
    ViScriptQuestsUtil.objectiveTarget(
        'example_quest',
        'collect_dirt',
        'dirt_objective'
    ),
    event => {
        event.player.tell(`目标完成：${event.objectiveId}`)
    }
)
```

如果目标没有配置稳定的 `objectiveId`，可以按目标在小任务中的索引监听：

```js
ViScriptQuestsEvents.objectiveCompleted(
    ViScriptQuestsUtil.objectiveIndexTarget('example_quest', 'collect_dirt', 0),
    event => {
        event.player.tell('第一个目标已经完成')
    }
)
```

## 在通用监听器中判断场景

不传目标时会接收该类型的全部事件。事件提供结构化匹配方法，不需要解析目标字符串：

```js
ViScriptQuestsEvents.taskStarted(event => {
    if (event.matchesTask('example_quest', 'collect_dirt')) {
        event.player.tell('进入指定的小任务')
    }
})
```

```js
ViScriptQuestsEvents.objectiveProgress(event => {
    if (!event.matchesObjective('example_quest', 'collect_dirt', 'dirt_objective')) {
        return
    }

    console.info(
        `目标进度：${event.previousAmount} -> ${event.currentAmount}/${event.requiredAmount}`
    )
})
```

所有任务事件都提供：

- `player`：触发任务状态变化的服务端玩家；奖励事件中表示实际收到奖励的玩家。
- `quest`：当前运行时大任务状态；离线待发奖励补发时可以为空。
- `questId`：规范化后的大任务 ID。
- `target`：当前事件用于定向监听的完整目标。
- `onlineMembers`：当前任务队伍中的在线玩家。
- `matchesQuest(questId)`：判断是否属于指定大任务。

小任务事件额外提供：

- `task`
- `stepId`
- `matchesTask(questId, stepId)`

目标事件额外提供：

- `objective`
- `objectiveId`
- `objectiveIndex`
- `previousAmount`
- `currentAmount`
- `amountDelta`
- `previousRequiredAmount`
- `requiredAmount`
- `automatic`
- `matchesObjective(questId, stepId, objectiveId)`
- `matchesObjectiveIndex(questId, stepId, objectiveIndex)`

## 生命周期事件字段

`questCompleted` 和 `taskCompleted` 提供 `forced`，用于区分正常完成和管理员、脚本强制完成。

`taskStarted` 提供 `reentered`。蓝图流程循环回已经结束的小任务时，该值为 `true`。

`taskFailed` 提供 `failedObjective`。失败由目标触发时，该字段指向触发失败的小任务目标。

`taskSkipped` 提供 `reason`：

- `branch`：分支或 Join 选择了其他路径。
- `quest_finished`：大任务结束时清理尚未完成的小任务。

`questFailed` 提供 `failedStepId`。失败来自某个小任务且没有可继续的失败出口时，该字段是对应的小任务 ID；失败结束节点直接结束任务时可以为空。

## 奖励事件

`rewardGranted` 在一份奖励真正发给在线玩家后触发。队伍奖励会按实际收件人分别触发。

```js
ViScriptQuestsEvents.rewardGranted(event => {
    console.info(
        `${event.player.name.string} 收到 ${event.rewardSource} 奖励，任务=${event.questId}`
    )
})
```

额外字段：

- `player`：实际收到奖励的玩家。
- `sourcePlayer`：触发任务结算的玩家。
- `reward`：已经解析动态数值后的奖励对象。
- `stepId`：小任务奖励或目标动作奖励所属的小任务；大任务奖励为空。
- `rewardSource`：`task`、`quest`、`objective` 或 `pending`。
- `teamDelivery`：奖励收件人与触发结算的玩家不同时为 `true`。

## 主动操作任务

服务端脚本可以使用：

```js
ViScriptQuestsUtil.grant(player, questId)
ViScriptQuestsUtil.revoke(player, questId)
ViScriptQuestsUtil.complete(player, questId)
ViScriptQuestsUtil.submit(player, questId, stepId)
ViScriptQuestsUtil.submitObjective(player, questId, stepId, objectiveIndex)
ViScriptQuestsUtil.triggerCustom(player, triggerId)
ViScriptQuestsUtil.track(player, questId)
ViScriptQuestsUtil.setVariable(player, questId, variableName, value)
ViScriptQuestsUtil.getQuest(player, questId)
ViScriptQuestsUtil.getTask(player, questId, stepId)
ViScriptQuestsUtil.openQuestBook(player)
```

## NeoForge 监听

其他 NeoForge 模组可以直接监听 `QuestEvent`，不需要依赖 KubeJS：

```java
@SubscribeEvent
public static void onTaskStarted(QuestEvent.TaskStarted event) {
    if (event.matchesTask("example_quest", "collect_dirt")) {
        ServerPlayer player = event.getPlayer();
        TaskProgress task = event.getTask();
    }
}
```

事件发布顺序为：目标进度变化、目标完成、目标动作、小任务完成、小任务奖励、后续流程、大任务完成。队伍共享进度的生命周期事件只发布一次，并通过 `onlineMembers` 提供当前在线成员；奖励事件按实际收件人发布。
