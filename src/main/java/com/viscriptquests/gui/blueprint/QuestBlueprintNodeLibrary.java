package com.viscriptquests.gui.blueprint;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibrary;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.viscriptquests.ViScriptQuests;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务蓝图节点库的显示规则。
 * <p>
 * LDLib2 默认节点库会平铺 Java 类名，这里按 @NodeAttribute.group 分组并使用翻译键显示。
 */
public final class QuestBlueprintNodeLibrary {
    public static void rebuild(ItemLibrary itemLibrary, GraphModel graphModel) {
        rebuild(itemLibrary, graphModel.getSupportNodes());
    }

    public static void rebuild(ItemLibrary itemLibrary, List<Class<? extends Node>> nodes) {
        hideDefaultLibraryTrees(itemLibrary);

        Map<String, List<Class<? extends Node>>> grouped = new LinkedHashMap<>();
        for (var nodeClass : nodes) {
            var attr = nodeClass.getAnnotation(NodeAttribute.class);
            String group = attr != null && !attr.group().isEmpty() ? attr.group() : "other";
            grouped.computeIfAbsent(group, key -> new ArrayList<>()).add(nodeClass);
        }

        var rootItem = new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.nodes"));
        var builder = TreeBuilder.<ItemLibraryItem, Void>start(rootItem);

        for (var entry : grouped.entrySet()) {
            addGroup(builder, entry.getKey(), entry.getValue());
        }

        var root = builder.build();
        itemLibrary.nodeTree.setRoot(root);
        itemLibrary.nodeTree.expandNode(root);
    }

    private static void hideDefaultLibraryTrees(ItemLibrary itemLibrary) {
        itemLibrary.contextTree.setRoot(null);
        itemLibrary.contextTree.setDisplay(false);
    }

    private static void addGroup(TreeBuilder<ItemLibraryItem, Void> builder, String group, List<Class<? extends Node>> nodes) {
        var categoryItem = new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable(ViScriptQuests.MOD_ID + ".blueprint.category." + group));

        builder.branch(categoryItem, branchBuilder -> {
            for (var nodeClass : nodes) {
                addNodeLeaf(branchBuilder, nodeClass);
            }
        });
    }

    private static void addNodeLeaf(TreeBuilder<ItemLibraryItem, Void> builder, Class<? extends Node> nodeClass) {
        var attr = nodeClass.getAnnotation(NodeAttribute.class);
        String nodeId = attr != null && !attr.name().isEmpty() ? attr.name() : nodeClass.getSimpleName();
        String translationKey = ViScriptQuests.MOD_ID + ".blueprint.node." + nodeId;

        var nodeItem = new NodeModelLibraryItem(
                translationKey,
                data -> QuestBlueprintGraphModel.createNodeFromData(data, nodeClass)
        );
        nodeItem.setSearchableName(translationKey + " " + nodeId + " " + nodeClass.getSimpleName());
        builder.leaf(nodeItem, null);
    }
}
