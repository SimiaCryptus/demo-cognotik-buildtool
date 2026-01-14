package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatInterface;
import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.models.AIModel;
import com.simiacryptus.cognotik.models.APIProvider;
import com.simiacryptus.cognotik.models.ModelSchema;
import com.simiacryptus.cognotik.plan.OrchestrationConfig;
import com.simiacryptus.cognotik.plan.tools.TaskType;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.cognitive.AdaptivePlanningConfig;
import com.simiacryptus.cognotik.plan.cognitive.CodingMode.CodingModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeType;
import com.simiacryptus.cognotik.plan.cognitive.WaterfallMode.WaterfallModeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.plan.tools.reasoning.BrainstormingTask;
import com.simiacryptus.cognotik.plan.tools.run.AutoFixTask;
import com.simiacryptus.cognotik.plan.tools.run.SubPlanTask;
import com.simiacryptus.cognotik.platform.ApplicationServices;
import com.simiacryptus.cognotik.platform.FileApplicationServices;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.platform.file.UserSettingsManager;
import com.simiacryptus.cognotik.platform.model.*;
import com.simiacryptus.cognotik.util.PlanHarness;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.junit.jupiter.api.DynamicTest;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SoftwareProjectGenerator {
    static {
        UnifiedHarness.Companion.configurePlatform();
    }

    //@TestFactory
    public List<DynamicTest> tests() {
        List<CognitiveModeConfig> cognitiveSettingsList = Arrays.asList(
                new WaterfallModeConfig(),
                new AdaptivePlanningConfig(),
                new CognitiveModeConfig(CognitiveModeType.Companion.getHierarchical()),
                new CodingModeConfig()
        );

        return cognitiveSettingsList.stream().flatMap(cognitiveSettings2 -> {
            String typeName = cognitiveSettings2.getType() != null ? cognitiveSettings2.getType().getName() : "null";

            TaskType<FileModificationTask.FileModificationTaskExecutionConfigData, TaskTypeConfig> fileModification = FileModificationTask.Companion.getFileModification();
            ApiChatModel chatModel = ProjectGenerator.getChatModel(GeminiModels.INSTANCE.getGeminiFlash_30_Preview());
            DynamicTest blindTest = DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_blind", () -> {
                Map<String, TaskTypeConfig> tasks = new HashMap<>();
                tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
                new ProjectGenerator(
                        new WaterfallModeConfig(),
                        cognitiveSettings2,
                        tasks,
                        "SoftwareProjectGenerator_" + typeName + "_blind",
                        "Build a fun and unique browser-based game."
                ).run();
            });

            DynamicTest specTest = DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_spec", () -> {
                Map<String, TaskTypeConfig> tasks = new HashMap<>();
                tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
                new ProjectGenerator(
                        new WaterfallModeConfig(),
                        cognitiveSettings2,
                        tasks,
                        "SoftwareProjectGenerator_" + typeName + "_spec",
                        "Build a fun and unique browser-based game. As the first phase of implementation, create detailed plan and design documents."
                ).run();
            });

            DynamicTest fixedTest = DynamicTest.dynamicTest("SoftwareProjectGenerator_" + typeName + "_fixed", () -> {
                Map<String, TaskTypeConfig> tasks = new HashMap<>();
                tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), chatModel));
                tasks.put(AutoFixTask.Companion.getAutoFix().getName(), new AutoFixTask.AutoFixTaskTypeConfig());
                new ProjectGenerator(
                        new WaterfallModeConfig(),
                        cognitiveSettings2,
                        tasks,
                        "SoftwareProjectGenerator_" + typeName + "_fixed",
                        "Build a fun and unique browser-based game."
                ).run();
            });

            return Stream.of(blindTest, specTest, fixedTest);
        }).collect(Collectors.toList());
    }
}

class ProjectGenerator extends PlanHarness {
    private final CognitiveModeConfig cognitiveSettings2;
    private final Map<String, TaskTypeConfig> tasks;
    private final String testName;

    static FileApplicationServices fileApplicationServices = ApplicationServices.INSTANCE.fileApplicationServices(
            ApplicationServicesConfig.INSTANCE.getDataStorageRoot()
    );
    static User user = UserSettingsManager.Companion.getDefaultUser();

    static UserSettings userSettings = fileApplicationServices.getUserSettingsManager().getUserSettings(user);
    public ProjectGenerator(
            CognitiveModeConfig cognitiveSettings1,
            CognitiveModeConfig cognitiveSettings2,
            Map<String, TaskTypeConfig> tasks,
            String testName,
            String prompt
    ) {
        super(prompt,
                cognitiveSettings1,
                (model, session) -> getInterface(model, session),
                8030,
                true,
                false,
                5,
                GeminiModels.INSTANCE.getGeminiFlash_30_Preview(),
                GeminiModels.INSTANCE.getGeminiFlash_30_Preview(),
                GeminiModels.INSTANCE.getGeminiFlash_30_Preview(),
                new File(".")
                );
        this.cognitiveSettings2 = cognitiveSettings2;
        this.tasks = tasks;
        this.testName = testName;
    }

    static ChatInterface getInterface(ApiChatModel model, Session session) {
        var api = getApi(model.getProvider() != null ? model.getProvider().getName() : null);

        var resolvedModel = model.getModel();
        if (resolvedModel == null) {
            throw new IllegalArgumentException("No model found for provider: " + (model.getProvider() != null ? model.getProvider().getName() : "null"));
        }

        var apiKey = (api != null) ? api.getKey() : null;
        if (apiKey == null) {
            throw new IllegalArgumentException("No API key found for provider: " + (model.getProvider() != null ? model.getProvider().getName() : "null"));
        }

        return resolvedModel.instance(
                apiKey,
                api.getBaseUrl(),
                (AIModel m, ModelSchema.Usage usage) -> incrementUsage(session, m, usage, fileApplicationServices, user)
        );
    }

    private static ApiData getApi(String providerName) {
        return userSettings.getApis().stream()
                .filter(apiData -> {
                    if (apiData.getProvider() == null) return false;
                    return apiData.getProvider().getName().equals(providerName);
                })
                .findFirst().orElse(null);
    }

    public static void incrementUsage(Session session, AIModel m, ModelSchema.Usage usage, FileApplicationServices fileApplicationServices, User user) {
        fileApplicationServices.getUsageManager().incrementUsage(
                session,
                user,
                m,
                usage
        );
    }

    public static ApiChatModel getChatModel(ChatModel chatModel) {
        APIProvider provider = chatModel.getProvider();
        ApiData apiData = getApi(provider != null ? provider.getName() : null);
        return new ApiChatModel(chatModel, apiData);
    }

    @Override
    public OrchestrationConfig newConfig(Session session, File tempDir) {
        OrchestrationConfig config = super.newConfig(session, tempDir);
        config.getTaskSettings().put(BrainstormingTask.Companion.getBrainstorming().getName(), new TaskTypeConfig(
                BrainstormingTask.Companion.getBrainstorming().getName(),
                BrainstormingTask.Companion.getBrainstorming().getName(),
                this.getModelInstanceFn().invoke(getFastModel(), session)
        ));
        SubPlanTask.SubPlanTaskTypeConfig subPlanTaskTypeConfig = new SubPlanTask.SubPlanTaskTypeConfig();
        subPlanTaskTypeConfig.setCognitiveSettings(cognitiveSettings2);
        subPlanTaskTypeConfig.setTaskSettings(tasks);
        config.getTaskSettings().put(SubPlanTask.Companion.getSubPlan().getName(), subPlanTaskTypeConfig);
        return config;
    }

    @Override
    public File createWorkspace() {
        File workspace = new File(".").toPath().resolve("workspaces/" + testName + "/test-" + now()).toFile();
        workspace.mkdirs();
        return workspace;
    }
}
