package net.alek.treeloghighlight.client.mixin;

import net.alek.treeloghighlight.client.HudPositionScreen;
import net.alek.treeloghighlight.client.HudPositionResolver;
import net.alek.treeloghighlight.client.TreeLogHighlightClient;
import net.alek.treeloghighlight.client.TreeLogHighlightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    @Unique
    private static final int BUTTON_WIDTH = 90;
    @Unique
    private static final int BUTTON_HEIGHT = 20;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addEditHudButton(CallbackInfo ci) {
        TreeLogHighlightConfig config = TreeLogHighlightClient.getConfig();

        int x = HudPositionResolver.resolveX(config.editButtonPos.anchor, config.editButtonPos.xOffset, BUTTON_WIDTH, this.width);
        int y = HudPositionResolver.resolveY(config.editButtonPos.anchor, config.editButtonPos.yOffset, BUTTON_HEIGHT, this.height);

        this.addRenderableWidget(Button.builder(Component.literal("Edit Tree HUD"), button -> Minecraft.getInstance().setScreen(new HudPositionScreen(this)))
                .pos(x, y)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
}