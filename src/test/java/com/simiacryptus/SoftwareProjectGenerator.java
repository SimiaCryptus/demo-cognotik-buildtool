package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.plan.tools.TaskType;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.cognitive.AdaptivePlanningConfig;
import com.simiacryptus.cognotik.plan.cognitive.CodingMode.CodingModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeConfig;
import com.simiacryptus.cognotik.plan.cognitive.CognitiveModeType;
import com.simiacryptus.cognotik.plan.cognitive.WaterfallMode.WaterfallModeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.plan.tools.run.AutoFixTask;
import com.simiacryptus.cognotik.platform.model.*;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class SoftwareProjectGenerator {
    static {
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

