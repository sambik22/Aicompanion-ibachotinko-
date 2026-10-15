package com.example.aicompanion.pig.talk;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Налаштування AI-мозку свиней: config/aicompanion-ai.properties (створюється сам).
 * Ключ береться з файлу або зі змінної середовища ANTHROPIC_API_KEY.
 * Без ключа свині все одно розмовляють — простими правилами (OfflinePigBrain).
 */
public final class AiConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "aicompanion-ai.properties";
    private static final String DEFAULT_MODEL = "claude-opus-5-5";

    private static final String TEMPLATE = """
            # Розумні свині: розмова через Claude API.
            # Без ключа свині відповідають простими вбудованими правилами.
            # Ключ: https://console.anthropic.com -> API Keys
            # (або задай змінну середовища ANTHROPIC_API_KEY і залиш поле порожнім).
            # Ключ потрібен лише на сервері (в одиночній грі — на твоєму ПК). Не публікуй цей файл.
            api_key=
            model=%s
            # low | medium | high — глибина "роздумів" свині; low = швидші відповіді
            effort=low
            """.formatted(DEFAULT_MODEL);

    private static boolean loaded;
    private static String apiKey = "";
    private static String model = DEFAULT_MODEL;
    private static String effort = "low";

    private AiConfig() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        Properties props = new Properties();
        try {
            if (Files.notExists(file)) {
                Files.writeString(file, TEMPLATE, StandardCharsets.UTF_8);
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e) {
            LOGGER.warn("Не вдалося прочитати {}: {}", file, e.toString());
        }

        apiKey = props.getProperty("api_key", "").trim();
        if (apiKey.isEmpty()) {
            String env = System.getenv("ANTHROPIC_API_KEY");
            apiKey = env == null ? "" : env.trim();
        }
        model = props.getProperty("model", DEFAULT_MODEL).trim();
        effort = props.getProperty("effort", "low").trim();

        LOGGER.info("AI свиней: {}", hasApiKey() ? "Claude API, модель " + model : "офлайн-правила (немає api_key)");
    }

    public static synchronized boolean hasApiKey() {
        load();
        return !apiKey.isEmpty();
    }

    static synchronized String apiKey() {
        load();
        return apiKey;
    }

    static synchronized String model() {
        load();
        return model;
    }

    static synchronized String effort() {
        load();
        return effort;
    }
}
