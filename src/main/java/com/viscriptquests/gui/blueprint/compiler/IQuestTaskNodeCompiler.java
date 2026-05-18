package com.viscriptquests.gui.blueprint.compiler;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.task.ITask;

import java.util.function.Supplier;

// 将任务目标节点编译成运行时 ITask 的扩展点，附属模组新增目标节点时只需要注册对应 compiler。
public interface IQuestTaskNodeCompiler extends ILDLRegister<IQuestTaskNodeCompiler, Supplier<IQuestTaskNodeCompiler>> {
    String ID = ViScriptQuests.MOD_ID + ":blueprint_task_node_compiler";

    boolean supports(CustomNodeModelImpl node);

    ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId);
}
