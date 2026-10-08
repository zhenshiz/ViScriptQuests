package com.viscriptquests.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.viscriptquests.ViScriptQuests;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 提供任务书物品查询的 JEI 按键与运行时 API。 */
@JeiPlugin
public final class JeiHelper implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ViScriptQuests.MOD_ID, "quest_book");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /**
     * 匹配玩家设置的 JEI 配方、用途按键，并打开目标物品的查询界面。
     *
     * @param itemStack 查询使用的真实目标物品
     * @param keyCode 输入键码
     * @param scanCode 输入扫描码
     * @return JEI 已处理查询时返回 {@code true}
     */
    public static boolean handleRecipeLookupKey(ItemStack itemStack, int keyCode, int scanCode) {
        return handleRecipeLookup(itemStack, InputConstants.getKey(keyCode, scanCode));
    }

    /**
     * 匹配玩家设置的 JEI 鼠标绑定，并打开目标物品的配方或用途。
     *
     * @param itemStack 查询使用的真实目标物品
     * @param button 鼠标按钮编号
     * @return JEI 已处理查询时返回 {@code true}
     */
    public static boolean handleRecipeLookupMouse(ItemStack itemStack, int button) {
        return handleRecipeLookup(itemStack, InputConstants.Type.MOUSE.getOrCreate(button));
    }

    private static boolean handleRecipeLookup(ItemStack itemStack, InputConstants.Key key) {
        if (runtime == null || itemStack.isEmpty()) return false;
        var mappings = runtime.getKeyMappings();
        List<RecipeIngredientRole> roles;
        if (mappings.getShowRecipe().isActiveAndMatches(key)) {
            roles = List.of(RecipeIngredientRole.OUTPUT);
        } else if (mappings.getShowUses().isActiveAndMatches(key)) {
            roles = List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST);
        } else {
            return false;
        }
        var focusFactory = runtime.getJeiHelpers().getFocusFactory();
        runtime.getRecipesGui().show(roles.stream()
                .<IFocus<?>>map(role -> focusFactory.createFocus(role, VanillaTypes.ITEM_STACK, itemStack)).toList());
        return true;
    }
}
