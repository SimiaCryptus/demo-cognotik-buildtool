package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.plan.OrchestrationConfig;
import com.simiacryptus.cognotik.plan.cognitive.WaterfallMode.WaterfallModeConfig;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.plan.tools.reasoning.BrainstormingTask;
import com.simiacryptus.cognotik.plan.tools.run.AutoFixTask;
import com.simiacryptus.cognotik.plan.tools.run.SubPlanTask;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.util.PlanHarness;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.simiacryptus.CognotikUtils.getChatModel;
import static com.simiacryptus.CognotikUtils.getInterface;

@SuppressWarnings("unused")
public class CodeImplementer {
    public static final String PROMPT = "Build a fun and unique game using java and gradle.";
    public static final int PORT = 8030;

    public static void main(String[] args) {
        UnifiedHarness.configurePlatform();
        ChatModel chatModel = GeminiModels.getGeminiFlash_30_Preview();
        var fileModification = FileModificationTask.getFileModification();
        Map<String, TaskTypeConfig> tasks = new HashMap<>();
        tasks.put(fileModification.getName(), new TaskTypeConfig(fileModification.getName(), fileModification.getName(), getChatModel(chatModel)));
        AutoFixTask.AutoFixTaskTypeConfig autoFixConfig = new AutoFixTask.AutoFixTaskTypeConfig();
        autoFixConfig.setModel(getChatModel(chatModel));
        tasks.put(AutoFixTask.getAutoFix().getName(), autoFixConfig);
        new PlanHarness(PROMPT,
                new WaterfallModeConfig(),
                (model, session) -> getInterface(getChatModel(model.getModel()), session),
                PORT,
                true,
                false,
                30,
                chatModel,
                chatModel,
                chatModel,
                new File(".")) {
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
                subPlanTaskTypeConfig.setCognitiveSettings(new WaterfallModeConfig());
                subPlanTaskTypeConfig.setTaskSettings(tasks);
                config.getTaskSettings().put(SubPlanTask.getSubPlan().getName(), subPlanTaskTypeConfig);
                return config;
            }

            @NotNull
            @Override
            public File createTempDirectory() {
                File workspace = new File(".", "workspaces/" + "CodeImplementer" + "/test-" + System.currentTimeMillis());
                //noinspection ResultOfMethodCallIgnored
                workspace.mkdirs();
                return workspace;
            }
        }.run();
    }
}