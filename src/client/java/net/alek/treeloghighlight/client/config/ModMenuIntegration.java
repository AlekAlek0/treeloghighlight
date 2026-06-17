package net.alek.treeloghighlight.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.alek.treeloghighlight.client.TreeLogHighlightClient;
import net.alek.treeloghighlight.client.TreeLogHighlightConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Tree Log Highlight Config"));

            TreeLogHighlightConfig config = TreeLogHighlightClient.getConfig();
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General Visuals"));
            ConfigCategory colors = builder.getOrCreateCategory(Component.literal("Colors"));
            ConfigCategory hud = builder.getOrCreateCategory(Component.literal("HUD Settings"));
            ConfigCategory woodTypes = builder.getOrCreateCategory(Component.literal("Wood Types"));

            // --- HUD ---
            hud.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show HUD Counter"), config.showHud)
                    .setDefaultValue(true).setSaveConsumer(v -> config.showHud = v).build());

            hud.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Status Message"), config.showStatusMessage)
                    .setDefaultValue(true).setSaveConsumer(v -> config.showStatusMessage = v).build());

            // --- General ---
            general.addEntry(entryBuilder.startEnumSelector(Component.literal("Render Mode"), TreeLogHighlightConfig.RenderMode.class, config.renderMode)
                    .setDefaultValue(TreeLogHighlightConfig.RenderMode.BOTH).setSaveConsumer(v -> config.renderMode = v).build());
            general.addEntry(entryBuilder.startIntSlider(Component.literal("Fill Opacity (%)"), (int)(config.alpha * 100), 0, 100)
                    .setDefaultValue(30).setSaveConsumer(v -> config.alpha = v / 100.0f).build());
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Through Walls"), config.showThroughWalls)
                    .setDefaultValue(true).setSaveConsumer(v -> config.showThroughWalls = v).build());
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Pulsing"), config.pulsing)
                    .setDefaultValue(true).setSaveConsumer(v -> config.pulsing = v).build());
            general.addEntry(entryBuilder.startIntSlider(Component.literal("Pulse Speed"), (int)(config.pulseSpeed * 10), 1, 50)
                    .setDefaultValue(10).setSaveConsumer(v -> config.pulseSpeed = v / 10.0f).build());

            // --- Colors ---
            colors.addEntry(entryBuilder.startBooleanToggle(Component.literal("Sync All Colors"), config.syncColors)
                    .setDefaultValue(true).setSaveConsumer(v -> config.syncColors = v).build());
            colors.addEntry(entryBuilder.startColorField(Component.literal("Main Color"), config.mainColor)
                    .setDefaultValue(0xFF8000).setSaveConsumer(v -> config.mainColor = v).build());
            colors.addEntry(entryBuilder.startColorField(Component.literal("Outline Color"), config.outlineColor)
                    .setDefaultValue(0xFF8000).setSaveConsumer(v -> config.outlineColor = v).build());
            colors.addEntry(entryBuilder.startColorField(Component.literal("Fill Color"), config.fillColor)
                    .setDefaultValue(0xFF8000).setSaveConsumer(v -> config.fillColor = v).build());
            colors.addEntry(entryBuilder.startColorField(Component.literal("Text Color"), config.textColor)
                    .setDefaultValue(0xFF8000).setSaveConsumer(v -> config.textColor = v).build());

            // --- Wood Types ---
            woodTypes.addEntry(entryBuilder.startTextDescription(Component.literal("§eNote: Wood types appear in the order they are discovered."))
                    .build());

            // Discovery Order: Preserved by LinkedHashMap. Oldest at top, newest at bottom.
            for (Map.Entry<String, Boolean> entry : config.woodTypeToggles.entrySet()) {
                String blockId = entry.getKey();
                ResourceLocation rl = ResourceLocation.parse(blockId);
                Block block = BuiltInRegistries.BLOCK.get(rl);
                
                String displayName = block.getName().getString();
                String modNamespace = rl.getNamespace();
                String modName = modNamespace.substring(0, 1).toUpperCase() + modNamespace.substring(1);
                
                woodTypes.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal(displayName + " (" + modName + ")"), 
                        entry.getValue())
                    .setDefaultValue(true)
                    .setSaveConsumer(v -> config.setWoodEnabled(blockId, v))
                    .build());
            }

            builder.setSavingRunnable(config::save);
            return builder.build();
        };
    }
}