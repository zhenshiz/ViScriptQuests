package com.viscriptquests.gui.blueprint.node.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.QuestBlueprintTypes;
import com.viscriptquests.gui.blueprint.node.QuestBlueprintNode;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.LocationTargetType;
import com.viscriptquests.quest.data.LocationWaypointColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

// 位置目标节点，用于生成“到达指定位置”的任务和 HUD 导航标记。
@NodeAttribute(name = QuestBlueprintNode.ID + "location_task", group = QuestBlueprintNode.TASK_GROUP, graphTypes = QuestBlueprintGraph.class)
public class LocationTaskNode extends QuestBlueprintNode {
    public static final String TARGET_TYPE_OPTION = "target_type";
    public static final String DIMENSION_OPTION = "dimension";
    public static final String X_OPTION = "x";
    public static final String Y_OPTION = "y";
    public static final String Z_OPTION = "z";
    public static final String ARRIVAL_RADIUS_OPTION = "arrival_radius";
    public static final String BIOME_ID_OPTION = "biome_id";
    public static final String STRUCTURE_ID_OPTION = "structure_id";
    public static final String MARKER_PROVIDER_OPTION = "marker_provider";
    public static final String MARKER_LABEL_OPTION = "marker_label";
    public static final String MARKER_ICON_OPTION = "marker_icon";
    public static final String MARKER_COLOR_OPTION = "marker_color";
    public static final String MARKER_WAYPOINT_COLOR_OPTION = "marker_waypoint_color";

    @Override
    public Component getDisplayName() {
        return nodeName("location_task");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        enumOption(context, TARGET_TYPE_OPTION, QuestBlueprintTypes.LOCATION_TARGET_TYPE, LocationTargetType.COORDINATES);
        switch (selectedTargetType()) {
            case COORDINATES -> {
                dimensionOption(context, DIMENSION_OPTION, "minecraft:overworld");
                doubleOption(context, X_OPTION, 0.0);
                doubleOption(context, Y_OPTION, 64.0);
                doubleOption(context, Z_OPTION, 0.0);
                doubleOption(context, ARRIVAL_RADIUS_OPTION, 3.0);
            }
            case BIOME -> biomeOption(context, BIOME_ID_OPTION, "minecraft:plains");
            case STRUCTURE -> structureOption(context, STRUCTURE_ID_OPTION, "minecraft:village_plains");
        }
        taskCommonOptions(context);
        enumOption(context, MARKER_PROVIDER_OPTION, QuestBlueprintTypes.LOCATION_MARKER_PROVIDER,
                LocationGuideMarkerProvider.BUILT_IN);
        stringOption(context, MARKER_LABEL_OPTION, "");
        if (selectedMarkerProvider().usesBuiltInHudMarker()) {
            displayIconOption(context, MARKER_ICON_OPTION,
                    DisplayIcon.item(Items.COMPASS.getDefaultInstance()));
            colorOption(context, MARKER_COLOR_OPTION, 0xFFD8C7FF);
        } else {
            option(context, MARKER_WAYPOINT_COLOR_OPTION, MARKER_COLOR_OPTION,
                    QuestBlueprintTypes.LOCATION_WAYPOINT_COLOR, LocationWaypointColor.PURPLE);
        }
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        taskFlowPorts(context);
    }

    @Override
    public boolean retainsOptionValue(String optionId) {
        return super.retainsOptionValue(optionId) || switch (optionId) {
            case DIMENSION_OPTION, X_OPTION, Y_OPTION, Z_OPTION, ARRIVAL_RADIUS_OPTION,
                    BIOME_ID_OPTION, STRUCTURE_ID_OPTION, MARKER_ICON_OPTION,
                    MARKER_COLOR_OPTION, MARKER_WAYPOINT_COLOR_OPTION -> true;
            default -> false;
        };
    }

    private LocationTargetType selectedTargetType() {
        return LocationTargetType.fromValue(getOptionValue(TARGET_TYPE_OPTION));
    }

    private LocationGuideMarkerProvider selectedMarkerProvider() {
        return LocationGuideMarkerProvider.fromValue(getOptionValue(MARKER_PROVIDER_OPTION));
    }
}
