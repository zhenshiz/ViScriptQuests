package com.viscriptquests.gui.blueprint.compiler.task;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.viscriptquests.gui.blueprint.compiler.IQuestTaskNodeCompiler;
import com.viscriptquests.gui.blueprint.compiler.QuestCompileContext;
import com.viscriptquests.gui.blueprint.data.LocationMarkerConfig;
import com.viscriptquests.gui.blueprint.data.LocationTargetConfig;
import com.viscriptquests.gui.blueprint.node.task.LocationTaskNode;
import com.viscriptquests.quest.data.task.ITask;
import com.viscriptquests.quest.data.task.LocationTask;

@LDLRegister(name = "location_task", registry = IQuestTaskNodeCompiler.ID)
public class LocationTaskNodeCompiler implements IQuestTaskNodeCompiler {
    @Override
    public boolean supports(CustomNodeModelImpl node) {
        return node.getNode() instanceof LocationTaskNode;
    }

    @Override
    public ITask compileTask(QuestCompileContext context, CustomNodeModelImpl node, String stepId) {
        LocationTargetConfig target = LocationTaskNode.targetConfigOf(node);
        LocationMarkerConfig marker = LocationTaskNode.markerConfigOf(node);
        LocationTask task = new LocationTask();
        task.stepId = stepId;
        task.targetType = target.targetTypeOrDefault();
        task.dimension = target.dimensionId();
        task.x = target.x;
        task.y = target.y;
        task.z = target.z;
        task.biomeId = target.biomeId();
        task.structureId = target.structureId();
        task.arrivalRadius = target.arrivalRadius();
        task.markerProvider = marker.providerOrDefault();
        task.markerLabel = marker.label();
        task.markerIcon = marker.icon();
        task.markerColor = marker.color;
        return task;
    }
}
