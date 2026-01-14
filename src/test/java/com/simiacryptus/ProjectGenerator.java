package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatInterface;
import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.models.AIModel;
import com.simiacryptus.cognotik.models.APIProvider;
import com.simiacryptus.cognotik.models.ModelSchema;
import com.simiacryptus.cognotik.plan.OrchestrationConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.reasoning.BrainstormingTask;
import com.simiacryptus.cognotik.plan.tools.run.SubPlanTask;
import com.simiacryptus.cognotik.platform.ApplicationServices;
import com.simiacryptus.cognotik.platform.FileApplicationServices;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.platform.file.UserSettingsManager;
import com.simiacryptus.cognotik.platform.model.*;
import com.simiacryptus.cognotik.util.PlanHarness;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

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
                (model, session) -> getInterface(getChatModel(model.getModel()), session),
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
                null,
                new ArrayList<>(),
                null,
                1.0,
                null,
                (AIModel m, ModelSchema.Usage usage) -> {
                    incrementUsage(session, m, usage, fileApplicationServices, user);
                    return kotlin.Unit.INSTANCE;
                }
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
                getChatModel(getFastModel())
        ));
        SubPlanTask.SubPlanTaskTypeConfig subPlanTaskTypeConfig = new SubPlanTask.SubPlanTaskTypeConfig();
        subPlanTaskTypeConfig.setCognitiveSettings(cognitiveSettings2);
        subPlanTaskTypeConfig.setTaskSettings(tasks);
        config.getTaskSettings().put(SubPlanTask.Companion.getSubPlan().getName(), subPlanTaskTypeConfig);
        return config;
    }

    @Override
    public File createWorkspace() {
        File workspace = new File(".").toPath().resolve("workspaces/" + testName + "/test-" + System.currentTimeMillis()).toFile();
        workspace.mkdirs();
        return workspace;
    }
}
