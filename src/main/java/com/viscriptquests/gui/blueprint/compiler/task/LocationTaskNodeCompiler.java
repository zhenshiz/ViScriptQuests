package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.node.task.LocationTaskNode;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.LocationGuideMarkerProvider;
import com.viscriptquests.quest.data.LocationTargetType;
import com.viscriptquests.quest.data.LocationWaypointColor;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.LocationTask;
import net.minecraft.world.item.Items;

@LDLRegister(name = "location_task", registry = IQuestTaskNodeCompiler.ID)
public class LocationTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof LocationTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        LocationTask task = new LocationTask();
        task.stepId = stepId;
        task.targetType = LocationTargetType.fromValue(
                context.getOptionValue(node, LocationTaskNode.TARGET_TYPE_OPTION));
        task.dimension = context.getString(node, LocationTaskNode.DIMENSION_OPTION);
        task.x = context.getDouble(node, LocationTaskNode.X_OPTION, 0.0);
        task.y = context.getDouble(node, LocationTaskNode.Y_OPTION, 64.0);
        task.z = context.getDouble(node, LocationTaskNode.Z_OPTION, 0.0);
        task.biomeId = context.getString(node, LocationTaskNode.BIOME_ID_OPTION);
        task.structureId = context.getString(node, LocationTaskNode.STRUCTURE_ID_OPTION);
        task.arrivalRadius = Math.max(0.0,
                context.getDouble(node, LocationTaskNode.ARRIVAL_RADIUS_OPTION, 3.0));
        task.markerProvider = LocationGuideMarkerProvider.fromValue(
                context.getOptionValue(node, LocationTaskNode.MARKER_PROVIDER_OPTION));
        task.markerLabel = context.getString(node, LocationTaskNode.MARKER_LABEL_OPTION);
        task.markerIcon = context.getOptionValue(node, LocationTaskNode.MARKER_ICON_OPTION) instanceof DisplayIcon
                ? context.getDisplayIcon(node, LocationTaskNode.MARKER_ICON_OPTION)
                : DisplayIcon.item(Items.COMPASS.getDefaultInstance());
        task.markerColor = task.markerProvider.usesBuiltInHudMarker()
                ? context.getInt(node, LocationTaskNode.MARKER_COLOR_OPTION, 0xFFD8C7FF)
                : LocationWaypointColor.fromValue(context.getOptionValue(
                        node, LocationTaskNode.MARKER_WAYPOINT_COLOR_OPTION)).getArgb();
        return task;
    }
}
