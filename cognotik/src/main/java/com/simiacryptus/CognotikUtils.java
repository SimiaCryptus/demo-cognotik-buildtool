package com.simiacryptus;

import com.google.common.util.concurrent.MoreExecutors;
import com.simiacryptus.cognotik.chat.model.ChatInterface;
import com.simiacryptus.cognotik.chat.model.ChatModel;
import com.simiacryptus.cognotik.models.APIProvider;
import com.simiacryptus.cognotik.platform.ApplicationServices;
import com.simiacryptus.cognotik.platform.FileApplicationServices;
import com.simiacryptus.cognotik.platform.Session;
import com.simiacryptus.cognotik.platform.file.UserSettingsManager;
import com.simiacryptus.cognotik.platform.model.*;
import com.simiacryptus.cognotik.util.PlanHarness;
import com.simiacryptus.cognotik.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CognotikUtils {
    public static final FileApplicationServices fileApplicationServices = ApplicationServices.fileApplicationServices(
            ApplicationServicesConfig.getDataStorageRoot()
    );
    public static User user(){
        return UserSettingsManager.getDefaultUser();
    }

    public static UserSettings userSettings() {
        return fileApplicationServices.getUserSettingsManager().getUserSettings(user());
    }

    public static ChatInterface getInterface(ApiChatModel model, Session session) {
        var api = getApi(getName(model));

        var resolvedModel = model.getModel();
        if (resolvedModel == null) {
            throw new IllegalArgumentException("No model found for provider: " + getName(model));
        }

        var apiKey = (api != null) ? api.getKey() : null;
        if (apiKey == null) {
            throw new IllegalArgumentException("No API key found for provider: " + getName(model));
        }

        return resolvedModel.instance(
                apiKey,
                api.getBaseUrl(),
                Level.INFO,
                new ArrayList<>(),
                Executors.newCachedThreadPool(),
                1.0,
                MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1)),
                (m, usage) -> {
                    fileApplicationServices.getUsageManager().incrementUsage(
                            session,
                            user(),
                            m,
                            usage
                    );
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    public static File relativize(File root, File file) {
        return new File(root.toURI().relativize(file.toURI()).getPath());
    }
    @Nullable
    public static String getName(ApiChatModel model) {
        return model.getProvider() != null ? model.getProvider().getName() : "null";
    }

    public static ApiData getApi(String providerName) {
        return userSettings().getApis().stream()
                .filter(apiData -> {
                    if (apiData.getProvider() == null) return false;
                    return apiData.getProvider().getName().equals(providerName);
                })
                .findFirst().orElse(null);
    }

    @NotNull
    private static ApiData getApiData(APIProvider gemini, String key) {
        return new ApiData(
                gemini.getName(),
                new SecureString(key),
                gemini.getBase(),
                gemini
        );
    }

    public static ApiChatModel getChatModel(ChatModel chatModel) {
        APIProvider provider = chatModel.getProvider();
        ApiData apiData = getApi(provider != null ? provider.getName() : null);
        return new ApiChatModel(chatModel, apiData);
    }
    private static Logger log = org.slf4j.LoggerFactory.getLogger(CognotikUtils.class);
    public static void configureEnvironmentalKeys() {
        PlanHarness.Companion.initDynamicEnums();
        if (APIProvider.values().isEmpty()) {
            throw new IllegalStateException("No API providers configured");
        }
        UserSettingsInterface userSettingsManager = ApplicationServices.fileApplicationServices(ApplicationServicesConfig.getDataStorageRoot()).getUserSettingsManager();
        User user = UserSettingsManager.getDefaultUser();
        UserSettings userSettings = userSettingsManager.getUserSettings(user);
        boolean anythingChanged = false;
        anythingChanged |= setProvider(userSettings, "GOOGLE_API_KEY", APIProvider.Companion.getGemini());
        anythingChanged |= setProvider(userSettings, "OPENAI_API_KEY", APIProvider.Companion.getOpenAI());
        anythingChanged |= setProvider(userSettings, "ANTHROPIC_API_KEY", APIProvider.Companion.getAnthropic());
        anythingChanged |= setProvider(userSettings, "GROQ_API_KEY", APIProvider.Companion.getGroq());
        if (anythingChanged) {
            log.info("Updating user settings with new API keys.");
            userSettingsManager.updateUserSettings(user, userSettings);
        } else {
            log.info("No API keys found in environment variables.");
        }
    }

    public static boolean setProvider(UserSettings userSettings, String keyName, APIProvider provider) {
        if(System.getenv(keyName) != null) {
            log.info("Configuring API key for provider: " + provider.getName());
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
