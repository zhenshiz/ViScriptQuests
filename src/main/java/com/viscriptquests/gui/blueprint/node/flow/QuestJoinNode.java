package com.viscriptquests.gui.blueprint.node.flow;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPortBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.QuestJoinMode;
import net.minecraft.network.chat.Component;

// 汇合节点：多条并行路径汇聚，支持三种完成规则
// - 任选其一（ANY）：完成任意一条分支即可继续
// - 全部完成（ALL）：所有分支都完成才继续
// - 至少 N 个（COUNT）：完成指定数量的分支即可继续，此时出现 required_count 配置
@NodeAttribute(name = QuestBlueprintNode.ID + "quest_join", group = QuestBlueprintNode.FLOW_GROUP, graphTypes = QuestBlueprintGraph.class)
public class QuestJoinNode extends QuestBlueprintNode {
    @Override
    public Component getDisplayName() {
        return nodeName("quest_join");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, "join_mode", QuestBlueprintTypes.JOIN_MODE, QuestJoinMode.ANY);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        inputFlow(context);

        // 只在 COUNT 模式下显示 required_count 输入端口（带嵌入常量编辑器）
        // 选择 ANY 或 ALL 时这个端口不会出现，实现动态表单效果
        INodeOption modeOpt = getNodeOptionById("join_mode");
        if (modeOpt != null) {
            modeOpt.tryGetValue(QuestJoinMode.class)
                    .ifSuccess(mode -> {
                        if (mode == QuestJoinMode.COUNT) {
                            IPortBuilder<?> builder = context.addInputPort("required_count", TypeHandles.INT)
                                    .withDisplayName(portName("required_count"));
                            builder.withDefaultValue(2);
                            builder.build();
                        }
                    });
        }

        outputFlow(context, "next");
    }
}
