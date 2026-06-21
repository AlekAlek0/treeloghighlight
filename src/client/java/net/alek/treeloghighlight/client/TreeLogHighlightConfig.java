package net.alek.treeloghighlight.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

public class TreeLogHighlightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "treeloghighlight.json");

    public enum RenderMode { OUTLINE, FULL, BOTH }

    public boolean modEnabled = true;
    public RenderMode renderMode = RenderMode.BOTH;

    // Colors
    public boolean syncColors = true;
    public int mainColor = 0xFF8000;
    public int outlineColor = 0xFF8000;
    public int fillColor = 0xFF8000;
    public int textColor = 0xFF8000;

    public float alpha = 0.3f;
    public boolean showThroughWalls = true;

    // HUD
    public boolean showHud = true;
    public int hudX = 20;
    public int hudY = 50;
    public boolean showStatusMessage = true;
    public int statusHudX = 572;
    public int statusHudY = 617;

    public int editButtonX = 10;
    public int editButtonY = 5;

    public boolean pulsing = true;
    public float pulseSpeed = 1.0f;
    public float pulseIntensity = 0.5f;

    // Use concrete LinkedHashMap to ensure GSON preserves discovery order
    public LinkedHashMap<String, Boolean> woodTypeToggles = new LinkedHashMap<>();

    public static TreeLogHighlightConfig load() {
        TreeLogHighlightConfig config = null;
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, TreeLogHighlightConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            config = new TreeLogHighlightConfig();
        }

        if (config.woodTypeToggles == null) {
            config.woodTypeToggles = new LinkedHashMap<>();
        }

        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isWoodEnabled(String blockId) {
        return woodTypeToggles.getOrDefault(blockId, true);
    }

    public void setWoodEnabled(String blockId, boolean enabled) {
        woodTypeToggles.put(blockId, enabled);
    }

    public float getFillR() { return (((syncColors ? mainColor : fillColor) >> 16) & 0xFF) / 255.0f; }
    public float getFillG() { return (((syncColors ? mainColor : fillColor) >> 8) & 0xFF) / 255.0f; }
    public float getFillB() { return ((syncColors ? mainColor : fillColor) & 0xFF) / 255.0f; }

    public float getOutlineR() { return (((syncColors ? mainColor : outlineColor) >> 16) & 0xFF) / 255.0f; }
    public float getOutlineG() { return (((syncColors ? mainColor : outlineColor) >> 8) & 0xFF) / 255.0f; }
    public float getOutlineB() { return ((syncColors ? mainColor : outlineColor) & 0xFF) / 255.0f; }

    public int getTextColor() { return 0xFF000000 | (syncColors ? mainColor : textColor);

    }
}