package com.viscriptquests.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.viscriptquests.ViScriptQuests;
import com.viscriptquests.quest.data.QuestFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class QuestFileHelper {
    public static final String QUEST_SUFFIX = ".quest";
    public static final String PROJECT_SUFFIX = ".questproj";
    private static final String PROJECT_NAME = "viscript_quests.editor.quest.add";
    private static final Map<String, QuestFile> CACHE = new LinkedHashMap<>();

    public static Path questDirectory() {
        return LDLib2.getAssetsDir().toPath()
                .resolve(ViScriptQuests.MOD_ID)
                .resolve("quest");
    }

    // 项目文件目录，存放 .questproj 编辑器项目文件
    public static Path projectDirectory() {
        return LDLib2.getAssetsDir().toPath()
                .resolve(ViScriptQuests.MOD_ID)
                .resolve("project");
    }

    public static String normalizeQuestId(String questId) {
        String normalized = questId.trim().replace('\\', '/');
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(QUEST_SUFFIX)) {
            normalized = normalized.substring(0, normalized.length() - QUEST_SUFFIX.length());
        }
        return normalized;
    }

    public static int reload(HolderLookup.Provider provider) throws IOException {
        CACHE.clear();
        Path directory = questDirectory();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            return 0;
        }
        try (var stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(QUEST_SUFFIX))
                    .sorted()
                    .forEach(path -> loadPath(directory, path, provider));
        }
        return CACHE.size();
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static Collection<String> cachedQuestIds() {
        return CACHE.keySet();
    }

    public static Optional<QuestFile> getQuest(String questId, HolderLookup.Provider provider) {
        String normalized = normalizeQuestId(questId);
        QuestFile cached = CACHE.get(normalized);
        if (cached != null) {
            return Optional.of(cached);
        }
        Path path;
        try {
            path = resolveQuestPath(normalized);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            QuestFile questFile = read(path, provider);
            questFile.quest.questId = normalized;
            CACHE.put(normalized, questFile);
            return Optional.of(questFile);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public static Path writeQuest(String questId, QuestFile questFile, HolderLookup.Provider provider) throws IOException {
        String normalized = normalizeQuestId(questId);
        questFile.quest.questId = normalized;
        Path path = resolveQuestPath(normalized);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(questFile.serializeNBT(provider), path);
        CACHE.put(normalized, questFile);
        return path;
    }

    private static void loadPath(Path directory, Path path, HolderLookup.Provider provider) {
        try {
            QuestFile questFile = read(path, provider);
            String logicalId = directory.relativize(path).toString().replace('\\', '/');
            logicalId = normalizeQuestId(logicalId);
            questFile.quest.questId = logicalId;
            CACHE.put(logicalId, questFile);
        } catch (IOException ignored) {
        }
    }

    private static QuestFile read(Path path, HolderLookup.Provider provider) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        QuestFile questFile = new QuestFile();
        questFile.deserializeNBT(provider, tag);
        return questFile;
    }

    private static Path resolveQuestPath(String questId) {
        Path directory = questDirectory().toAbsolutePath().normalize();
        Path path = directory.resolve(normalizeQuestId(questId) + QUEST_SUFFIX).normalize();
        if (!path.startsWith(directory)) {
            throw new IllegalArgumentException("Quest path escapes quest directory: " + questId);
        }
        return path;
    }

    // ========== 项目文件 (.questproj) 方法 ==========

    public static String normalizeProjectId(String projectId) {
        String normalized = projectId.trim().replace('\\', '/');
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(PROJECT_SUFFIX)) {
            normalized = normalized.substring(0, normalized.length() - PROJECT_SUFFIX.length());
        }
        return normalized;
    }

    // 将编辑器图数据写入项目文件
    public static Path writeProject(String projectId, CompoundTag graphTag) throws IOException {
        String normalized = normalizeProjectId(projectId);
        Path path = resolveProjectPath(normalized);
        Files.createDirectories(path.getParent());
        NbtIo.write(createProjectFileTag(graphTag), path);
        return path;
    }

    // 从服务端读取项目文件，返回图数据 CompoundTag
    public static Optional<CompoundTag> readProject(String projectId) {
        String normalized = normalizeProjectId(projectId);
        Path path;
        try {
            path = resolveProjectPath(normalized);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return readProjectGraph(path);
    }

    public static Optional<CompoundTag> readProjectGraph(Path path) {
        try {
            return Optional.of(extractProjectGraph(readProjectFileTag(path)));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public static CompoundTag createProjectFileTag(CompoundTag graphTag) {
        CompoundTag root = new CompoundTag();
        root.putString("version", "1.0");

        CompoundTag data = new CompoundTag();
        data.put("graph", graphTag.copy());
        root.put("data", data);
        return root;
    }

    public static CompoundTag extractProjectGraph(CompoundTag projectOrGraphTag) {
        if (projectOrGraphTag.contains("data", Tag.TAG_COMPOUND)) {
            CompoundTag data = projectOrGraphTag.getCompound("data");
            if (data.contains("graph", Tag.TAG_COMPOUND)) {
                return data.getCompound("graph").copy();
            }
        }
        if (projectOrGraphTag.contains("graph", Tag.TAG_COMPOUND)) {
            return projectOrGraphTag.getCompound("graph").copy();
        }
        return projectOrGraphTag.copy();
    }

    private static CompoundTag readProjectFileTag(Path path) throws IOException {
        try {
            CompoundTag tag = NbtIo.read(path);
            if (tag != null) {
                return tag;
            }
        } catch (IOException | RuntimeException ignored) {
            // 兼容旧版上传项目：旧文件是压缩的裸 graphTag，而 LDLib2 项目文件是不压缩的包装 NBT。
        }
        return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
    }

    // 扫描项目目录下的所有 .questproj 文件，用于命令 tab 补全
    public static List<String> getServerProjectFiles() {
        List<String> projectFiles = new ArrayList<>();
        Path directory = projectDirectory();
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    String path = file.toString();
                    if (path.endsWith(PROJECT_SUFFIX)) {
                        String relative = directory.relativize(file).toString()
                                .replace('\\', '/')
                                .replace(PROJECT_SUFFIX, "");
                        projectFiles.add("\"" + relative + "\"");
                    }
                });
            } catch (IOException ignored) {
            }
        }
        return projectFiles;
    }

    private static Path resolveProjectPath(String projectId) {
        Path directory = projectDirectory().toAbsolutePath().normalize();
        Path path = directory.resolve(normalizeProjectId(projectId) + PROJECT_SUFFIX).normalize();
        if (!path.startsWith(directory)) {
            throw new IllegalArgumentException("Project path escapes project directory: " + projectId);
        }
        return path;
    }
}
