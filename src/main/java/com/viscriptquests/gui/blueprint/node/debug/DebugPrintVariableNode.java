package com.viscriptquests.gui.blueprint.node.debug;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 调试打印变量节点，支持两种方式指定变量：
// 1. 通过 value 输入端口连线到黑板变量节点（自动检测变量名）
// 2. 通过 variable_name 选项手动填写变量名（备用方式）
// 运行时默认将变量值写入日志，需要玩家聊天栏反馈时可显式勾选。
@NodeAttribute(name = "debug_print_var", group = QuestBlueprintNode.DEBUG_GROUP, graphTypes = QuestBlueprintGraph.class)
public class DebugPrintVariableNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("debug_print_var");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        stringOption(context, "variable_name", "");
        boolOption(context, "send_to_chat", false);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        // 使用 Object 类型，兼容所有黑板变量类型（Float/Int/String/Bool 等），
        objectInput(context, "value");
        outputFlow(context, "next");
    }
}
