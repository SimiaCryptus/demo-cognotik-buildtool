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
import com.simiacryptus.cognotik.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

class CognotikUtils {
    public static final FileApplicationServices fileApplicationServices = ApplicationServices.fileApplicationServices(
            ApplicationServicesConfig.getDataStorageRoot()
    );
    public static final User user = UserSettingsManager.getDefaultUser();
    public static final UserSettings userSettings = fileApplicationServices.getUserSettingsManager().getUserSettings(user);

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
                            user,
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
        APIProvider.Companion providers = APIProvider.Companion;
        if (providerName == providers.getGemini().getName() && System.getenv("GOOGLE_API_KEY") != null) {
            return getApiData(providers.getGemini(), System.getenv("GOOGLE_API_KEY"));
        } else if (providerName == providers.getOpenAI().getName() && System.getenv("OPENAI_API_KEY") != null) {
            return getApiData(providers.getOpenAI(), System.getenv("OPENAI_API_KEY"));
        } else if (providerName == providers.getAnthropic().getName() && System.getenv("ANTHROPIC_API_KEY") != null) {
            return getApiData(providers.getAnthropic(), System.getenv("ANTHROPIC_API_KEY"));
        } else if (providerName == providers.getGroq().getName() && System.getenv("GROQ_API_KEY") != null) {
            return getApiData(providers.getGroq(), System.getenv("GROQ_API_KEY"));
        } else {
            return userSettings.getApis().stream()
                    .filter(apiData -> {
                        if (apiData.getProvider() == null) return false;
                        return apiData.getProvider().getName().equals(providerName);
                    })
                    .findFirst().orElse(null);
        }
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
}
