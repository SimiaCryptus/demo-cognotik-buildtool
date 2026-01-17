package com.simiacryptus;

import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.chat.model.GeminiModels;
import com.simiacryptus.cognotik.models.APIProvider;
import com.simiacryptus.cognotik.plan.tools.TaskTypeConfig;
import com.simiacryptus.cognotik.plan.tools.file.FileModificationTask;
import com.simiacryptus.cognotik.platform.ApplicationServices;
import com.simiacryptus.cognotik.platform.file.UserSettingsManager;
import com.simiacryptus.cognotik.platform.model.*;
import com.simiacryptus.cognotik.util.SecureString;
import com.simiacryptus.cognotik.util.TaskHarness;
import com.simiacryptus.cognotik.util.UnifiedHarness;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import static com.simiacryptus.CognotikUtils.getChatModel;
import static com.simiacryptus.CognotikUtils.getInterface;

@SuppressWarnings("unused")
public class CodeFixer {
    public static final String PROMPT = "Fix the build errors reported in build.log";
    public static final int PORT = 8030;

    public static void main(String[] args) {
        configureEnvironmentalKeys();
        UnifiedHarness.configurePlatform();
        ChatModel chatModel = GeminiModels.getGeminiFlash_30_Preview();
        var fileModification = FileModificationTask.getFileModification();
        FileModificationTask.FileModificationTaskExecutionConfigData  config = new FileModificationTask.FileModificationTaskExecutionConfigData();
        config.setTask_description(args.length > 0 ? args[0] : PROMPT);
        config.setRelated_files(List.of(
                args.length > 1 ? args[1] : "build.log"
        ));
        new TaskHarness<>(
                fileModification,
                new TaskTypeConfig(fileModification.getName(), fileModification.getName(), getChatModel(chatModel)),
                config,
                (model, session) -> getInterface(getChatModel(model.getModel()), session),
                PORT,
                true,
                false,
                30,
                chatModel,
                chatModel,
                chatModel,
                new File("."),
                0.0
        ) {
            @NotNull
            @Override
            public File createWorkspace() {
                File workspace = new File(".", "workspaces/" + "CodeFixer" + "/test-" + System.currentTimeMillis());
                //noinspection ResultOfMethodCallIgnored
                workspace.mkdirs();
                return workspace;
            }
        }.run();
    }

    public static void configureEnvironmentalKeys() {
        UserSettingsInterface userSettingsManager = ApplicationServices.fileApplicationServices(ApplicationServicesConfig.getDataStorageRoot()).getUserSettingsManager();
        User user = UserSettingsManager.getDefaultUser();
        UserSettings userSettings = userSettingsManager.getUserSettings(user);
        boolean anythingChanged = false;
        anythingChanged |= setProvider(userSettings, "GOOGLE_API_KEY", APIProvider.Companion.getGemini());
        anythingChanged |= setProvider(userSettings, "OPENAI_API_KEY", APIProvider.Companion.getOpenAI());
        anythingChanged |= setProvider(userSettings, "ANTHROPIC_API_KEY", APIProvider.Companion.getAnthropic());
        anythingChanged |= setProvider(userSettings, "GROQ_API_KEY", APIProvider.Companion.getGroq());
        if(anythingChanged) userSettingsManager.updateUserSettings(user, userSettings);
    }

    public static boolean setProvider(UserSettings userSettings, String keyName, APIProvider provider) {
        if(System.getenv(keyName) != null) {
            List<ApiData> apis = userSettings.getApis();
            // find any existing entry for this provider and remove it
            apis.removeIf(apiData -> apiData.getProvider().getName().equals(provider.getName()));
            // add new entry
            apis.add(new ApiData(
                    provider.getName(),
                    new SecureString(System.getenv(keyName)),
                    provider.getBase(),
                    provider
            ));
            return true;
        } else {
            return false;
        }
    }
}