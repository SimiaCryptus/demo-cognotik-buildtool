package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.plan.OrchestrationConfig;
import com.simiacryptus.cognotik.plan.cognitive.AdaptivePlanningConfig;
import com.simiacryptus.cognotik.plan.cognitive.CodingMode;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeType;
import com.simiacryptus.cognotik.plan.cognitive.WaterfallMode.WaterfallModeConfig;
import com.simiacryptus.cognotik.plan.tools.TaskType;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.plan.tools.reasoning.BrainstormingTask;
import com.simiacryptus.cognotik.plan.tools.run.AutoFixTask;
import com.simiacryptus.cognotik.plan.tools.run.SubPlanTask;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.platform.model.ApiChatModel;
import com.simiacryptus.cognotik.util.PlanHarness;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.simiacryptus.CognotikUtils.getChatModel;
import static com.simiacryptus.CognotikUtils.getInterface;

@SuppressWarnings("unused")
public class ProjectGeneratorMatrix {

    @BeforeAll
    public static void setup() {
        UnifiedHarness.configurePlatform();
    }

    @TestFactory
    public List<DynamicTest> tests() {
        List<CognitiveModeConfig> cognitiveSettingsList = Arrays.asList(
                new WaterfallModeConfig(),
                new AdaptivePlanningConfig(),
                new CognitiveModeConfig(CognitiveModeType.getHierarchical()),
                new CodingMode.CodingModeConfig()
        );
        ChatModel model = GeminiModels.getGeminiFlash_30_Preview();
        return cognitiveSettingsList.stream()
                .flatMap(cognitiveSettings2 -> getTestStream(cognitiveSettings2, model))
                .collect(Collectors.toList());
    }

    @NotNull
    public Stream<DynamicTest> getTestStream(CognitiveModeConfig cognitiveSettings2, ChatModel model) {
        String typeName = cognitiveSettings2.getType() != null ? cognitiveSettings2.getType().getName() : "null";
        var fileModification = FileModificationTask.getFileModification();
        ApiChatModel chatModel = getChatModel(model);
        return Stream.of(
                DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_blind", () -> {
                    test_blind(cognitiveSettings2, model, fileModification, chatModel, typeName);
                }),
                DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_spec", () -> {
                    test_spec(cognitiveSettings2, model, fileModification, chatModel, typeName);
                }),
                DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_fixed", () -> {
                    test_autofix(cognitiveSettings2, model, fileModification, chatModel, typeName);
                })
        );
    }

    public static void test_autofix(CognitiveModeConfig cognitiveSettings2,
                                    ChatModel model,
                                    TaskType<FileModificationTask.FileModificationTaskExecutionConfigData, TaskTypeConfig> fileModification,
                                    ApiChatModel chatModel,
                                    String typeName) {
        Map<String, TaskTypeConfig> tasks = new HashMap<>();
        tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
        tasks.put(AutoFixTask.getAutoFix().getName(), new AutoFixTask.AutoFixTaskTypeConfig());
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

    public static class ProjectGenerator extends PlanHarness {

        public static final long START = System.currentTimeMillis();
        private final CognitiveModeConfig cognitiveSettings2;
        private final Map<String, TaskTypeConfig> tasks;

        public ProjectGenerator(
                CognitiveModeConfig cognitiveSettings1,
                CognitiveModeConfig cognitiveSettings2,
                Map<String, TaskTypeConfig> tasks,
                String testName,
                String prompt,
                ChatModel chatModel
        ) {
            super(prompt,
                    cognitiveSettings1,
                    (model, session) -> getInterface(getChatModel(model.getModel()), session),
                    8030,
                    true,
                    false,
                    30,
                    chatModel,
                    chatModel,
                    chatModel,
                    new File(".", "workspaces/" + testName + "/test-" + START)
            );
            this.cognitiveSettings2 = cognitiveSettings2;
            this.tasks = tasks;
        }

        @NotNull
        @Override
        public OrchestrationConfig newConfig(@NotNull Session session, @NotNull File tempDir) {
            OrchestrationConfig config = super.newConfig(session, tempDir);
            var brainstorming = BrainstormingTask.getBrainstorming();
            config.getTaskSettings().put(brainstorming.getName(), new TaskTypeConfig(
                    brainstorming.getName(),
                    brainstorming.getName(),
                    getChatModel(getFastModel())
            ));
            SubPlanTask.SubPlanTaskTypeConfig subPlanTaskTypeConfig = new SubPlanTask.SubPlanTaskTypeConfig();
            subPlanTaskTypeConfig.setCognitiveSettings(cognitiveSettings2);
            subPlanTaskTypeConfig.setTaskSettings(tasks);
            config.getTaskSettings().put(SubPlanTask.getSubPlan().getName(), subPlanTaskTypeConfig);
            return config;
        }

        @NotNull
        @Override
        public File createTempDirectory() {
            File workspace = this.createTempDirectory();
            //noinspection ResultOfMethodCallIgnored
            workspace.mkdirs();
            return workspace;
        }
    }
}

