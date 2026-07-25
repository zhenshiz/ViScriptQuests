package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.data.LocationMarkerConfig;
import com.viscriptquests.gui.blueprint.data.LocationTargetConfig;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import net.minecraft.network.chat.Component;

// 位置目标节点，用于生成“到达指定位置”的任务和 HUD 导航标记。
@NodeAttribute(name = QuestBlueprintNode.ID + "location_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class LocationTaskNode extends QuestBlueprintNode {
    public static final String TARGET_CONFIG_OPTION = "location_target";
    public static final String MARKER_CONFIG_OPTION = "location_marker";

    @Override
    public Component getDisplayName() {
        return nodeName("location_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        option(context, TARGET_CONFIG_OPTION, QuestBlueprintTypes.LOCATION_TARGET_CONFIG, LocationTargetConfig.defaults());
        taskCommonOptions(context);
        option(context, MARKER_CONFIG_OPTION, QuestBlueprintTypes.LOCATION_MARKER_CONFIG, LocationMarkerConfig.defaults());
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
    }

    public static LocationTargetConfig targetConfigOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + TARGET_CONFIG_OPTION);
        if (constant != null && constant.getValue() instanceof LocationTargetConfig target) {
            target.ensureDefaults();
            return target;
        }
        return LocationTargetConfig.defaults();
    }

    public static LocationMarkerConfig markerConfigOf(NodeModel nodeModel) {
        Constant constant = nodeModel.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + MARKER_CONFIG_OPTION);
        if (constant != null && constant.getValue() instanceof LocationMarkerConfig marker) {
            marker.ensureDefaults();
            return marker;
        }
        return LocationMarkerConfig.defaults();
    }
}
