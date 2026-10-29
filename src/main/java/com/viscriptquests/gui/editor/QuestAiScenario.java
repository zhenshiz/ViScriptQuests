package com.viscriptquests.gui.editor;

import com.google.gson.*;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.settings.EditorSettings;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.viscript_lib.gui.ai.AiGraphTools;
import com.viscript_lib.gui.editor.ViScriptEditor;
import com.viscript_lib.gui.editor.dev.AiAdapterScenario;
import com.viscriptquests.gui.blueprint.QuestBlueprintGraph;
import com.viscriptquests.gui.blueprint.model.QuestSubQuestNodeModel;
import com.viscriptquests.quest.data.task.ItemTask;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.List;

/** 验证任务主图、子图、节点数据与连线通过真实会话编辑并可整轮恢复。 */
@LDLRegisterClient(name = "quest_ai_adapter", group = "viscript_quests",
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestAiScenario extends AiAdapterScenario {
    private static final String ADDED = "438bb32e-ec80-4b0b-9207-f0d0e5a16a20";

    @Override
    protected ViScriptEditor createEditor(TestContext ctx) {
        var project = new QuestProject(); project.initNewProject();
        var graph = new QuestBlueprintGraph(); graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), project.currentGraphTag());
        for (var raw : AiGraphTools.rows((com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl) graph.graphModel)) {
            var row = raw.getAsJsonObject();
            if (row.has("type") && row.get("type").getAsString().endsWith(":quest_start")) ctx.put("quest-start", row);
        }
        var subquest = graph.graphModel.getNodeModels().stream().filter(QuestSubQuestNodeModel.class::isInstance).map(QuestSubQuestNodeModel.class::cast).findFirst().orElseThrow();
        ctx.put("quest-sub", subquest.getUid().toString());
        for (var raw : AiGraphTools.rows((com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl) subquest.ensureLocalSubgraph())) {
            var row = raw.getAsJsonObject();
            if (row.has("type") && row.get("type").getAsString().endsWith(":item_task")) ctx.put("quest-task", row);
            if (row.has("type") && row.get("type").getAsString().endsWith(":item_reward")) ctx.put("quest-reward", row);
        }
        var editor = new QuestEditor() {
            @Override protected EditorSettings createSettings() { return super.createSettings().setSettingsFile(Path.of(System.getProperty("ldlib2.uitest.out"), "quest-ai-settings.json").toFile()); }
        };
        editor.loadProject(project, null); return editor;
    }

    @Override
    protected List<JsonObject> reads(TestContext ctx) {
        return List.of(json("{\"section\":\"catalog\",\"query\":\"item_reward\",\"graph\":\"" + ctx.get("quest-sub") + "\"}"), json("{\"section\":\"graph\"}"), json("{\"section\":\"graph\",\"graph\":\"" + ctx.get("quest-sub") + "\"}"));
    }

    @Override
    protected JsonObject edits(TestContext ctx) {
        var start = (JsonObject) ctx.get("quest-start"); var task = (JsonObject) ctx.get("quest-task"); var reward = (JsonObject) ctx.get("quest-reward");
        var ops = new JsonArray();
        var title = operation("set_node", start.get("id").getAsString(), null);
        var values = new JsonObject(); values.addProperty(key(start, "title"), "AI Quest"); title.add("values", values); ops.add(title);
        var update = operation("set_node", task.get("id").getAsString(), ctx.get("quest-sub"));
        values = new JsonObject(); values.add(key(task, "item_stack"), json("{\"id\":\"minecraft:diamond\",\"count\":6}")); values.addProperty("item_count", 6); update.add("values", values); ops.add(update);
        var add = operation("add_node", ADDED, ctx.get("quest-sub")); add.addProperty("type", reward.get("type").getAsString()); add.addProperty("x", 630); add.addProperty("y", 120); ops.add(add);
        var connect = operation("connect", null, ctx.get("quest-sub")); connect.add("from", reward.get("id")); connect.addProperty("output", "next"); connect.addProperty("to", ADDED); connect.addProperty("input", "in"); ops.add(connect);
        var result = new JsonObject(); result.add("operations", ops); return result;
    }

    @Override
    protected void verifyEdited(TestContext ctx) {
        var file = ((QuestProject) editor(ctx).getCurrentProject()).compileQuestFile();
        ctx.check("主图标题已编译", file.quest.title.equals("AI Quest"));
        ctx.check("子图物品任务已更新", file.tasks.stream().anyMatch(t -> t instanceof ItemTask item && item.itemStack.is(Items.DIAMOND) && item.itemCount == 6));
        ctx.check("新增连线的奖励已编译", file.rewards.size() == 2);
        ctx.check("模型读取到主图及子图", readResults(ctx).size() == 3 && readResults(ctx).get(2).get("graph").getAsString().equals(ctx.get("quest-sub")));
        ctx.check("目录包含真实节点和常量", readResults(ctx).getFirst().getAsJsonArray("entries").get(0).getAsJsonObject().has("values"));
    }

    @Override
    protected void verifyValidation(TestContext ctx) {
        var view = ctx.query(".__graph-editor-view__").one().as(com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView.class).graphView;
        var node = (com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel) view.getGraph().graphModel.getNodeModels().getFirst();
        var oldPosition = new org.joml.Vector2f(node.getPosition());
        var newPosition = new org.joml.Vector2f(oldPosition).add(20, 0);
        view.getHistoryStack().pushHistory(net.minecraft.network.chat.Component.literal("manual"), com.lowdragmc.lowdraglib2.configurator.EditAction.of(
                () -> node.setPosition(new org.joml.Vector2f(newPosition)), () -> node.setPosition(new org.joml.Vector2f(oldPosition))), null, true);
        view.rebuildGraphUI();
        ctx.put("quest-original-view", view); ctx.put("quest-manual-history", view.getHistoryStack().getUndoStack().size());
        var project = (QuestProject) editor(ctx).getCurrentProject(); var before = project.serializeNBT(Platform.getFrozenRegistry()).copy();
        var draft = new QuestProject(); draft.deserializeNBT(Platform.getFrozenRegistry(), before.copy());
        var start = (JsonObject) ctx.get("quest-start");
        boolean rejected = false;
        try { editor(ctx).createAiEditorAdapter().edit(draft, json("{\"operations\":[{\"op\":\"connect\",\"from\":\"" + start.get("id").getAsString() + "\",\"output\":\"missing\",\"to\":\"" + ctx.get("quest-sub") + "\",\"input\":\"in\"}]}")); }
        catch (IllegalArgumentException exception) { rejected = true; }
        ctx.check("无效端口被拒绝且不改动当前工程", rejected && before.equals(project.serializeNBT(Platform.getFrozenRegistry())));
    }

    @Override
    protected void verifyRestored(TestContext ctx) {
        var view = (com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView) ctx.get("quest-original-view");
        ctx.check("AI 切换保留手动画布历史", view.getHistoryStack().getUndoStack().size() == (int) ctx.get("quest-manual-history"));
        var node = (com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel) view.getGraph().graphModel.getNodeModels().getFirst();
        float x = node.getPosition().x;
        view.getHistoryStack().undo();
        ctx.check("AI 放弃或撤销后手动历史仍可撤销", node.getPosition().x == x - 20);
        view.getHistoryStack().redo();
        ctx.check("手动历史仍可重做", node.getPosition().x == x);
    }

    private static JsonObject operation(String name, String id, String scope) { var value = new JsonObject(); value.addProperty("op", name); if (id != null) value.addProperty("id", id); if (scope != null) value.addProperty("graph", scope); return value; }
    private static String key(JsonObject node, String suffix) { return node.getAsJsonObject("values").keySet().stream().filter(k -> k.equals(suffix) || k.equals(com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption.PORT_ID_PREFIX + suffix)).findFirst().orElseThrow(); }
}
