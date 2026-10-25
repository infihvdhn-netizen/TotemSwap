package ru.totemswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SwapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("totemswap.json");
    private static double delaySeconds;

    private SwapConfig() {
    }

    static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                JsonObject config = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
                if (config != null && config.has("delaySeconds")) {
                    delaySeconds = clamp(config.get("delaySeconds").getAsDouble());
                }
            } else {
                save();
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("TotemSwap: could not read config: " + exception.getMessage());
            delaySeconds = 0;
        }
    }

    static long delayMillis() {
        return Math.round(delaySeconds * 1000);
    }

    private static void save() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        JsonObject config = new JsonObject();
        config.addProperty("delaySeconds", delaySeconds);
        Files.writeString(CONFIG_PATH, GSON.toJson(config) + System.lineSeparator());
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(2, value));
    }
}