package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.plan.cognitive.AdaptivePlanningConfig;
import com.simiacryptus.cognotik.plan.cognitive.CodingMode.CodingModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeType;
import com.simiacryptus.cognotik.plan.cognitive.WaterfallMode.WaterfallModeConfig;
import com.simiacryptus.cognotik.plan.tools.TaskType;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.plan.tools.run.AutoFixTask;
import com.simiacryptus.cognotik.platform.model.ApiChatModel;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.simiacryptus.CognotikUtils.getChatModel;

@SuppressWarnings("unused")
public class SoftwareProjectGenerator {

    @BeforeAll
    public static void setup() {
        UnifiedHarness.Companion.configurePlatform();
    }

    @TestFactory
    public List<DynamicTest> tests() {
        List<CognitiveModeConfig> cognitiveSettingsList = Arrays.asList(
                new WaterfallModeConfig(),
                new AdaptivePlanningConfig(),
                new CognitiveModeConfig(CognitiveModeType.Companion.getHierarchical()),
                new CodingModeConfig()
        );
        ChatModel model = GeminiModels.INSTANCE.getGeminiFlash_30_Preview();
        return cognitiveSettingsList.stream()
                .flatMap(cognitiveSettings2 -> getTestStream(cognitiveSettings2, model))
                .collect(Collectors.toList());
    }

    @NotNull
    public Stream<DynamicTest> getTestStream(CognitiveModeConfig cognitiveSettings2, ChatModel model) {
        String typeName = cognitiveSettings2.getType() != null ? cognitiveSettings2.getType().getName() : "null";
        var fileModification = FileModificationTask.Companion.getFileModification();
        ApiChatModel chatModel = getChatModel(model);
        return Stream.of(DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_blind", () -> {
            test_blind(cognitiveSettings2, model, fileModification, chatModel, typeName);
        }), DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_spec", () -> {
            test_spec(cognitiveSettings2, model, fileModification, chatModel, typeName);
        }), DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_fixed", () -> {
            test_autofix(cognitiveSettings2, model, fileModification, chatModel, typeName);
        }));
    }

    public static void test_autofix(CognitiveModeConfig cognitiveSettings2,
                                    ChatModel model,
                                    TaskType<FileModificationTask.FileModificationTaskExecutionConfigData, TaskTypeConfig> fileModification,
                                    ApiChatModel chatModel,
                                    String typeName) {
        Map<String, TaskTypeConfig> tasks = new HashMap<>();
        tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
        tasks.put(AutoFixTask.Companion.getAutoFix().getName(), new AutoFixTask.AutoFixTaskTypeConfig());
        new ProjectGenerator(
                new WaterfallModeConfig(),
                cognitiveSettings2,
                tasks,
                "SoftwareProjectGenerator_" + typeName + "_fixed",
                "Build a fun and unique browser-based game.", model
        ).run();
    }

    public static void test_spec(CognitiveModeConfig cognitiveSettings2,
                                 ChatModel model,
                                 TaskType<FileModificationTask.FileModificationTaskExecutionConfigData, TaskTypeConfig> fileModification,
                                 ApiChatModel chatModel,
                                 String typeName) {
        Map<String, TaskTypeConfig> tasks = new HashMap<>();
        tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
        new ProjectGenerator(
                new WaterfallModeConfig(),
                cognitiveSettings2,
                tasks,
                "SoftwareProjectGenerator_" + typeName + "_spec",
                "Build a fun and unique browser-based game. As the first phase of implementation, create detailed plan and design documents.", model
        ).run();
    }

    public static void test_blind(CognitiveModeConfig cognitiveSettings2,
                                  ChatModel model,
                                  TaskType<FileModificationTask.FileModificationTaskExecutionConfigData, TaskTypeConfig> fileModification,
                                  ApiChatModel chatModel,
                                  String typeName) {
        Map<String, TaskTypeConfig> tasks = new HashMap<>();
        tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
        new ProjectGenerator(
                new WaterfallModeConfig(),
                cognitiveSettings2,
                tasks,
                "SoftwareProjectGenerator_" + typeName + "_blind",
                "Build a fun and unique browser-based game.", model
        ).run();
    }
}

