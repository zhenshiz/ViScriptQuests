package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscript_lib.gui.components.DraggableUI;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.QuestCategoryConfigUI;
import com.viscriptquests.quest.data.DisplayIcon;
import com.viscriptquests.quest.data.runtime.QuestCategoryConfigData;
import com.viscriptquests.quest.data.runtime.QuestCategoryData;
import com.viscriptquests.quest.data.runtime.QuestCategoryListData;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

@LDLRegisterClient(name = "quest_category_config", group = ViScriptQuests.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class QuestCategoryConfigScenario implements UIScenario {
    @Override
    public void configure(ScenarioOptions options) {
        options.tags("quest_book", "quest_category", "config");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.openModularUI("打开分类配置", c -> {
            QuestCategoryConfigData config = QuestCategoryConfigData.of(fixture(), Set.of("story"));
            return new ModularUI(UI.of(new QuestCategoryConfigUI(config), QuestCategoryConfigUI::getAutoGuiScaledSize));
        }).awaitModularUI().awaitElement("#quest_category_all")
                .checkExists("#quest_category_item_story")
                .checkExists("#quest_category_delete")
                .step("全部分类图标 NBT 往返", QuestCategoryConfigScenario::checkIconRoundTrip)
                .step("全部分类是固定条目", QuestCategoryConfigScenario::checkAllRowIsFixed)
                .click("#quest_category_all").awaitElement("#quest_category_all_editor")
                .step("全部分类只显示图标配置且不能删除", QuestCategoryConfigScenario::checkAllEditor)
                .click("#quest_category_item_story").awaitElement("#quest_category_editor")
                .checkVisible("#quest_category_delete")
                .checkNotExists("#quest_category_all_editor")
                .closeScreen();
    }

    private static void checkIconRoundTrip(com.lowdragmc.lowdraglib2.uitest.TestContext c) {
        QuestCategoryListData source = fixture();
        CompoundTag tag = source.serializeNBT(Platform.getFrozenRegistry());
        QuestCategoryListData restored = new QuestCategoryListData();
        restored.deserializeNBT(Platform.getFrozenRegistry(), tag);
        c.check("全部分类图标类型保留", restored.allCategoryIcon != null
                && restored.allCategoryIcon.isTexture());
        c.check("全部分类贴图资源保留", restored.allCategoryIcon != null
                && "minecraft:textures/item/diamond.png".equals(restored.allCategoryIcon.getTexture()));
        c.check("普通分类仍然保留", restored.categories.size() == 1
                && "story".equals(restored.categories.getFirst().id));
    }

    private static void checkAllRowIsFixed(com.lowdragmc.lowdraglib2.uitest.TestContext c) {
        var row = c.el("#quest_category_all_row").element();
        boolean insideDraggable = false;
        for (var parent = row.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof DraggableUI<?>) {
                insideDraggable = true;
                break;
            }
        }
        c.check("全部分类不属于可拖拽分类列表", !insideDraggable);
    }

    private static void checkAllEditor(com.lowdragmc.lowdraglib2.uitest.TestContext c) {
        c.check("全部分类删除按钮隐藏", !c.el("#quest_category_delete").isVisible());
        c.check("全部分类编辑器可见", c.el("#quest_category_all_editor").isVisible());
        c.check("普通分类编辑器未显示", !c.exists("#quest_category_editor"));

        var forbidden = Set.of("分类 ID", "显示名称", "任务列表", "Category ID", "Display Name", "Quest IDs");
        boolean ordinaryFieldVisible = c.el("#quest_category_all_editor").element().allChildrenStream()
                .filter(TextElement.class::isInstance)
                .map(TextElement.class::cast)
                .filter(element -> element.isDisplayed() && element.isVisible())
                .map(element -> element.getText().getString())
                .anyMatch(forbidden::contains);
        c.check("全部分类编辑器不显示普通分类字段", !ordinaryFieldVisible);
    }

    private static QuestCategoryListData fixture() {
        QuestCategoryListData data = new QuestCategoryListData();
        data.allCategoryIcon = DisplayIcon.texture("minecraft:textures/item/diamond.png");
        data.categories.add(QuestCategoryData.of("story", "主线任务", DisplayIcon.item("minecraft:book")));
        return data;
    }
}
