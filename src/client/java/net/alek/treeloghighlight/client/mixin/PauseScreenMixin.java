package net.alek.treeloghighlight.client.mixin;

import net.alek.treeloghighlight.client.HudPositionScreen;
import net.alek.treeloghighlight.client.TreeLogHighlightClient;
import net.alek.treeloghighlight.client.TreeLogHighlightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addEditHudButton(CallbackInfo ci) {
        TreeLogHighlightConfig config = TreeLogHighlightClient.getConfig();
        
        int x = config.editButtonX;
        if (x == -1) {
            x = this.width - 95; // Default top right
        }

        this.addRenderableWidget(Button.builder(Component.literal("Edit Tree HUD"), button -> {
            Minecraft.getInstance().setScreen(new HudPositionScreen(this));
        })
        .pos(x, config.editButtonY)
        .size(90, 20)
        .build());
    }
}