package com.viscriptquests.uitest;

import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.mojang.authlib.GameProfile;
import com.viscript_lib.gui.editor.EditorServerUploads;
import com.viscript_lib.network.c2s.EditorUploadC2SPackets;
import com.viscript_lib.util.item.ItemOutputTargets;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.gui.blueprint.QuestBlueprintFlowTypes;
import com.viscriptquests.network.c2s.C2SPayload;
import com.viscriptquests.quest.data.*;
import com.viscriptquests.quest.data.reward.CommandReward;
import com.viscriptquests.quest.data.runtime.*;
import com.viscriptquests.quest.data.task.ItemTask;
import com.viscriptquests.quest.runtime.QuestManager;
import com.viscriptquests.util.QuestCategoryFileHelper;
import com.viscriptquests.util.QuestFileHelper;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.*;
import com.viscriptshop.gui.util.ShopEditorUploads;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.PurchaseRequest;
import com.viscriptshop.util.ViScriptShopServerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@LDLRegisterClient(name = "command_authorization", group = ViScriptQuests.MOD_ID,
        modID = "viscript_shop", registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class CommandAuthorizationScenario implements UIScenario {
    private static final String SHOP = "__security_uitest";
    private static final String QUEST = "__security_uitest";
    private static final String STEP = "secure_step";
    private static final String CATEGORY = "c7904646-0288-4404-955a-6c58a1d833a8";
    private static final String MERCHANT = "8a0e51a2-dcc4-446c-92b1-33b3d8c183a8";

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("security").defaultTimeoutMs(15_000);
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.server("create authoritative catalog and remove editor permission", context -> {
            ServerPlayer player = context.player();
            context.put("owner", context.server().getSingleplayerProfile());
            context.server().setSingleplayerProfile(new GameProfile(UUID.randomUUID(), "SecurityFixture"));
            context.server().getPlayerList().setAllowCommandsForAllPlayers(false);
            context.server().getPlayerList().deop(player.getGameProfile());
            context.check("ordinary player has no elevated permission", !player.hasPermissions(2));
            player.getInventory().clearContent();
            player.getEnderChestInventory().clearContent();
            player.totalExperience = 0;
            ViScriptShopServerUtil.setMoney(player, 100);
            var shop = new ShopInfo();
            var category = new CategoryInfo();
            category.setId(CATEGORY);
            category.setShopType(CategoryInfo.ShopType.CURRENCY);
            var merchant = new MerchantInfo();
            merchant.setId(MERCHANT);
            merchant.setMoney(10);
            merchant.setStock(3);
            merchant.setItemResult(new ItemStack(Items.DIAMOND, 2));
            merchant.getCommands().add("experience add @s 11 points");
            category.getMerchants().add(merchant);
            shop.getCategoryInfos().add(category);
            ViScriptShopServerUtil.setShopInfo(SHOP, shop);
            context.put("shop", shop);
            try {
                var shopPath = Shop.FORMAT.functionDirectory().toPath().resolve(SHOP + Shop.SUFFIX);
                Files.createDirectories(shopPath.getParent());
                NbtIo.writeCompressed(Shop.serializeRuntimeNBT(player.registryAccess(), shop), shopPath);
                context.put("shopPath", shopPath);
                context.put("shopBytes", Files.readAllBytes(shopPath));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            context.put("categories", QuestCategoryFileHelper.copyCategories());
            var quest = questFixture();
            try {
                var questPath = QuestFileHelper.writeQuest(QUEST, quest, player.registryAccess());
                context.put("questPath", questPath);
                context.put("questBytes", Files.readAllBytes(questPath));
                var questCategory = QuestCategoryData.of("security", "Security", "minecraft:book");
                questCategory.questIds.add(QUEST);
                QuestCategoryFileHelper.saveCategories(List.of(questCategory));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            context.check("v2 purchase handler is registered",
                    RPCPacketDistributor.getPacketHandler(BuyMerchantPayload.BUY_MERCHANT) != null);
            context.check("old purchase handler is disabled",
                    RPCPacketDistributor.getPacketHandler("viscript_shop:buy_merchant") == null);
        });

        scenario.step("send forged editor uploads through real connection", context -> {
            CompoundTag project = new CompoundTag();
            project.putString("fileName", QUEST);
            project.put("graph", new CompoundTag());
            RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_PROJECT_FILE, project);
            CompoundTag quest = new CompoundTag();
            quest.putString("fileName", QUEST);
            quest.put("quest", questFixture().serializeNBT(context.requirePlayer().registryAccess()));
            quest.getCompound("quest").putString("command", "experience add @s 999 points");
            RPCPacketDistributor.rpcToServer(C2SPayload.UPLOAD_QUEST_FILE, quest);
            var generic = uploadRequest();
            RPCPacketDistributor.rpcToServer(EditorUploadC2SPackets.UPLOAD_EDITOR_FILE, generic);
            RPCPacketDistributor.rpcToServer(com.viscriptshop.network.c2s.C2SPayload.UPLOAD_SHOP_FILE_C2S, generic);
            PacketDistributor.sendToServer(PacketRPCPacket.of("viscript_shop:buy_merchant", new byte[0]));
            RPCPacketDistributor.rpcToServer(com.viscriptquests.network.s2c.S2CPayload.OPEN_EDITOR_WITH_PROJECT,
                    new CompoundTag());
            RPCPacketDistributor.rpcToServer(com.viscriptshop.network.s2c.S2CPayload.OPEN_SHOP_EDITOR,
                    new ShopInfo());
        }).ticks(5).server("uploads did not modify trusted configuration", context -> {
            try {
                context.check("quest fixture remains unchanged", java.util.Arrays.equals(context.get("questBytes"),
                        Files.readAllBytes(context.get("questPath"))));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            context.check("ordinary player cannot create a project", !Files.exists(
                    QuestFileHelper.projectDirectory().resolve(QUEST + QuestFileHelper.PROJECT_SUFFIX)));
            try {
                context.check("generic/shop upload cannot overwrite a shop file", java.util.Arrays.equals(
                        context.get("shopBytes"), Files.readAllBytes(context.get("shopPath"))));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            context.check("no injected command executed", context.player().totalExperience == 0);
        });

        scenario.server("permission level two cannot author elevated commands", context -> {
            var player = context.player();
            context.server().getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
            context.check("fixture has level two but not owner permission", player.hasPermissions(2) && !player.hasPermissions(4));
            var project = new CompoundTag();
            project.putString("fileName", QUEST);
            project.put("graph", new CompoundTag());
            C2SPayload.uploadProjectFile(RPCSender.ofClient(player), project);
            var quest = new CompoundTag();
            quest.putString("fileName", QUEST);
            quest.put("quest", new CompoundTag());
            C2SPayload.uploadQuestFile(RPCSender.ofClient(player), quest);
            EditorUploadC2SPackets.receiveEditorUpload(RPCSender.ofClient(player), uploadRequest());
            ShopEditorUploads.receiveShopUpload(RPCSender.ofClient(player), uploadRequest());
            try {
                context.check("level two did not replace quest", java.util.Arrays.equals(context.get("questBytes"),
                        Files.readAllBytes(context.get("questPath"))));
                context.check("level two did not replace shop", java.util.Arrays.equals(context.get("shopBytes"),
                        Files.readAllBytes(context.get("shopPath"))));
                context.check("level two did not create project", !Files.exists(
                        QuestFileHelper.projectDirectory().resolve(QUEST + QuestFileHelper.PROJECT_SUFFIX)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            context.server().getPlayerList().deop(player.getGameProfile());
        });

        scenario.server("reject invalid shopping lists without any settlement", context -> {
            var player = context.player();
            var sender = RPCSender.ofClient(player);
            for (int quantity : new int[]{0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 4}) {
                BuyMerchantPayload.buyMerchant(sender, SHOP, entries(quantity), ItemOutputTargets.PLAYER_INVENTORY);
            }
            BuyMerchantPayload.buyMerchant(sender, "missing", entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            BuyMerchantPayload.buyMerchant(sender, SHOP.toUpperCase(java.util.Locale.ROOT), entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            BuyMerchantPayload.buyMerchant(sender, "../" + SHOP, entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            BuyMerchantPayload.buyMerchant(sender, SHOP, new PurchaseRequest(List.of(entry(1), entry(1))), ItemOutputTargets.PLAYER_INVENTORY);
            BuyMerchantPayload.buyMerchant(sender, SHOP, new PurchaseRequest(List.of(entry(1),
                    new AggregatedResources.PurchaseEntry(CATEGORY, "fake", 1))), ItemOutputTargets.PLAYER_INVENTORY);
            BuyMerchantPayload.buyMerchant(sender, SHOP, new PurchaseRequest(List.of(
                    new AggregatedResources.PurchaseEntry("fake", MERCHANT, 1))), ItemOutputTargets.PLAYER_INVENTORY);
            assertUnchanged(context);
            var shop = context.<ShopInfo>get("shop");
            var category = shop.getCategoryInfos().getFirst();
            var merchant = category.getMerchants().getFirst();
            var lock = new MerchantFlagGroup();
            lock.getFlags().add("missing_security_flag");
            merchant.setStageRestrictionEnabled(true);
            merchant.getFlagGroups().add(lock);
            BuyMerchantPayload.buyMerchant(sender, SHOP, entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            merchant.setStageRestrictionEnabled(false);
            category.setStageRestrictionEnabled(true);
            category.getFlagGroups().add(lock);
            BuyMerchantPayload.buyMerchant(sender, SHOP, entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            category.setStageRestrictionEnabled(false);
            assertUnchanged(context);
            merchant.setMoney(101);
            BuyMerchantPayload.buyMerchant(sender, SHOP, entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            merchant.setMoney(10);
            category.setShopType(CategoryInfo.ShopType.ITEM_FOR_ITEM);
            merchant.setItemA(new ItemStack(Items.EMERALD, 3));
            BuyMerchantPayload.buyMerchant(sender, SHOP, entries(1), ItemOutputTargets.PLAYER_INVENTORY);
            category.setShopType(CategoryInfo.ShopType.CURRENCY);
            assertUnchanged(context);
        });

        scenario.step("purchase two units over the v2 connection", context ->
                RPCPacketDistributor.rpcToServer(BuyMerchantPayload.BUY_MERCHANT, SHOP,
                        entries(2), ItemOutputTargets.PLAYER_INVENTORY))
                .waitUntilServer("server completed purchase", context -> context.player().totalExperience == 11)
                .server("only authoritative costs gains and command were used", context -> {
                    context.check("server charged 20", ViScriptShopServerUtil.getMoney(context.player()) == 80);
                    context.check("server gave four diamonds", context.player().getInventory().countItem(Items.DIAMOND) == 4);
                    context.check("stock is one", stock(context.player()) == 1);
                });
        scenario.step("path alias cannot reset stock", context -> RPCPacketDistributor.rpcToServer(
                BuyMerchantPayload.BUY_MERCHANT, "./" + SHOP, entries(2), ItemOutputTargets.PLAYER_INVENTORY))
                .ticks(5).server("alias request refused", context -> {
                    context.check("no repeated command", context.player().totalExperience == 11);
                    context.check("money unchanged", ViScriptShopServerUtil.getMoney(context.player()) == 80);
                    context.check("stock unchanged", stock(context.player()) == 1);
                });

        scenario.step("submit an unaccepted quest with injected reward fields", context ->
                RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, submitTag(STEP, 0)))
                .ticks(5).server("unaccepted quest gives no reward", context -> {
                    context.check("no quest reward", context.player().totalExperience == 11);
                    context.check("accept fixture", QuestManager.grant(context.player(), QUEST));
                });
        scenario.step("submit incomplete wrong locked and invalid objectives", context -> {
            for (var tag : List.of(submitTag(STEP, 0), submitTag("locked", 0), submitTag("fake", 0),
                    submitTag(STEP, -1), submitTag(STEP, Integer.MAX_VALUE))) {
                RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, tag);
            }
        }).ticks(5).server("server completion conditions prevent reward", context -> {
            context.check("no quest command", context.player().totalExperience == 11);
            context.check("quest stays active", state(context.player()).status == QuestStatus.ACTIVE);
            context.player().getInventory().add(new ItemStack(Items.EMERALD, 2));
        });
        scenario.step("complete legitimate objective over the connection", context ->
                RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, submitTag(STEP, 0)))
                .waitUntilServer("quest completed", context -> state(context.player()).status == QuestStatus.COMPLETED)
                .server("server file grants step and quest commands once", context -> {
                    context.check("only server commands ran", context.player().totalExperience == 47);
                    context.check("quest required actual emerald payment", context.player().getInventory().countItem(Items.EMERALD) == 0);
                    context.check("step reward recorded", state(context.player()).rewardedSteps.contains(STEP));
                });
        scenario.step("replay reward request", context -> {
            for (int i = 0; i < 3; i++) RPCPacketDistributor.rpcToServer(C2SPayload.SUBMIT_QUEST_TASK, submitTag(STEP, 0));
        }).ticks(5).server("completed quest cannot grant rewards twice", context ->
                context.check("no duplicate command execution", context.player().totalExperience == 47));

        scenario.teardownServer("restore fixture files and permissions", context -> {
            context.server().setSingleplayerProfile(context.get("owner"));
            ViscriptShop.getShopSavedData().resetShopInfo(SHOP);
            QuestSavedData.get(context.server()).getPlayer(context.player().getUUID()).removeQuest(QUEST);
            try {
                if (context.get("questPath") != null) Files.deleteIfExists(context.get("questPath"));
                if (context.get("shopPath") != null) Files.deleteIfExists(context.get("shopPath"));
                if (context.get("categories") != null) QuestCategoryFileHelper.saveCategories(context.get("categories"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            QuestFileHelper.clearCache();
        });
    }

    private static CompoundTag uploadRequest() {
        var tag = new CompoundTag();
        tag.putString(EditorServerUploads.TAG_TARGET, EditorServerUploads.TARGET_RUNTIME);
        tag.putString(EditorServerUploads.TAG_MOD_ID, Shop.FORMAT.modId());
        tag.putString(EditorServerUploads.TAG_DOMAIN, Shop.FORMAT.domain());
        tag.putString(EditorServerUploads.TAG_SUFFIX, Shop.SUFFIX);
        tag.putString(EditorServerUploads.TAG_FILE_NAME, SHOP);
        tag.putBoolean(EditorServerUploads.TAG_COMPRESSED, true);
        tag.put(EditorServerUploads.TAG_DATA, new CompoundTag());
        return tag;
    }

    private static CompoundTag submitTag(String step, int index) {
        var tag = new CompoundTag();
        tag.putString("questId", QUEST);
        tag.putString("stepId", step);
        tag.putInt("objectiveIndex", index);
        tag.putBoolean("completed", true);
        tag.putString("command", "experience add @s 999 points");
        return tag;
    }

    private static QuestFile questFixture() {
        var file = new QuestFile();
        file.quest.questId = QUEST;
        var task = new ItemTask();
        task.stepId = STEP;
        task.objectiveId = "emeralds";
        task.itemStack = new ItemStack(Items.EMERALD);
        task.itemCount = 2;
        task.submitMode = QuestSubmitMode.MANUAL;
        file.tasks.add(task);
        var locked = new ItemTask();
        locked.stepId = "locked";
        locked.itemStack = new ItemStack(Items.NETHER_STAR);
        locked.submitMode = QuestSubmitMode.MANUAL;
        file.tasks.add(locked);
        var stepReward = new CommandReward();
        stepReward.stepId = STEP;
        stepReward.command = "experience add @s 17 points";
        file.rewards.add(stepReward);
        var completionReward = new CommandReward();
        completionReward.command = "experience add @s 19 points";
        file.rewards.add(completionReward);
        file.flowNodes.add(QuestBlueprintFlowTypes.createStart());
        file.flowNodes.add(QuestBlueprintFlowTypes.createSubQuest(STEP));
        file.flowNodes.add(QuestBlueprintFlowTypes.createEnd("end", true));
        var start = new QuestFlowEdge();
        start.toNodeId = STEP;
        file.flowEdges.add(start);
        var end = new QuestFlowEdge();
        end.fromNodeId = STEP;
        end.toNodeId = "end";
        file.flowEdges.add(end);
        return file;
    }

    private static AggregatedResources.PurchaseEntry entry(int quantity) {
        return new AggregatedResources.PurchaseEntry(CATEGORY, MERCHANT, quantity);
    }

    private static PurchaseRequest entries(int quantity) {
        return new PurchaseRequest(List.of(entry(quantity)));
    }

    private static PlayerQuestState state(ServerPlayer player) {
        return QuestSavedData.get(player.getServer()).getPlayer(player.getUUID()).findQuest(QUEST).orElseThrow();
    }

    private static int stock(ServerPlayer player) {
        var merchant = ViScriptShopServerUtil.getShopInfo(SHOP).getCategoryInfos().getFirst().getMerchants().getFirst();
        return ViScriptShopServerUtil.getEffectiveMerchantStock(player, SHOP, CATEGORY, merchant);
    }

    private static void assertUnchanged(ServerContext context) {
        context.check("no command executed", context.player().totalExperience == 0);
        context.check("no money spent", ViScriptShopServerUtil.getMoney(context.player()) == 100);
        context.check("no items gained", context.player().getInventory().countItem(Items.DIAMOND) == 0);
        context.check("no stock spent", stock(context.player()) == 3);
    }
}
