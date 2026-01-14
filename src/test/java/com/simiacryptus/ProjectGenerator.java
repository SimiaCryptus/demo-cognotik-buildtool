package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.plan.OrchestrationConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.reasoning.BrainstormingTask;
import com.simiacryptus.cognotik.plan.tools.run.SubPlanTask;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.util.PlanHarness;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

import static com.simiacryptus.CognotikUtils.getChatModel;
import static com.simiacryptus.CognotikUtils.getInterface;

class ProjectGenerator extends PlanHarness {

    private final CognitiveModeConfig cognitiveSettings2;
    private final Map<String, TaskTypeConfig> tasks;
    private final String testName;

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
                5,
                chatModel,
                chatModel,
                chatModel,
                new File(".")
        );
        this.cognitiveSettings2 = cognitiveSettings2;
        this.tasks = tasks;
        this.testName = testName;
    }

    @NotNull
    @Override
    public OrchestrationConfig newConfig(@NotNull Session session, @NotNull File tempDir) {
        OrchestrationConfig config = super.newConfig(session, tempDir);
        var brainstorming = BrainstormingTask.Companion.getBrainstorming();
        config.getTaskSettings().put(brainstorming.getName(), new TaskTypeConfig(
                brainstorming.getName(),
                brainstorming.getName(),
                getChatModel(getFastModel())
        ));
        SubPlanTask.SubPlanTaskTypeConfig subPlanTaskTypeConfig = new SubPlanTask.SubPlanTaskTypeConfig();
        subPlanTaskTypeConfig.setCognitiveSettings(cognitiveSettings2);
        subPlanTaskTypeConfig.setTaskSettings(tasks);
        config.getTaskSettings().put(SubPlanTask.Companion.getSubPlan().getName(), subPlanTaskTypeConfig);
        return config;
    }

    @NotNull
    @Override
    public File createWorkspace() {
        File workspace = new File(".").toPath().resolve("workspaces/" + testName + "/test-" + System.currentTimeMillis()).toFile();
        //noinspection ResultOfMethodCallIgnored
        workspace.mkdirs();
        return workspace;
    }
}
