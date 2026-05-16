package com.viscriptquests.gui.blueprint.node.debug;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 调试打印节点默认写入日志；需要玩家聊天栏反馈时可显式勾选。
@NodeAttribute(name = "debug_print", group = QuestBlueprintNode.DEBUG_GROUP, graphTypes = QuestBlueprintGraph.class)
public class DebugPrintNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("debug_print");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        boolOption(context, "send_to_chat", false);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);
        stringInput(context, "message", "");
        outputFlow(context, "next");
    }
}
