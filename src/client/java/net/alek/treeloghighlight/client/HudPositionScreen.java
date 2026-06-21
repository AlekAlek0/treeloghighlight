package net.alek.treeloghighlight.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudPositionScreen extends Screen {
    private final Screen parent;
    private final TreeLogHighlightConfig config;

    private enum Dragging { NONE, LOGS, STATUS, BUTTON }
    private Dragging currentDrag = Dragging.NONE;
    private double dragOffsetX, dragOffsetY;

    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("Adjust HUD Positions"));
        this.parent = parent;
        this.config = TreeLogHighlightClient.getConfig();
    }

    @Override
    protected void init() {
        // Add Reset Button at the bottom
        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> {
            resetToDefaults();
        }).pos(this.width / 2 - 50, this.height - 30).size(100, 20).build());

    }

    private void resetToDefaults() {
        config.hudX = 20;
        config.hudY = 50;
        config.editButtonX = 10;
        config.editButtonY = 5;
        config.statusHudX = 572;
        config.statusHudY = 617;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Draw a simple dark tint instead of the blurry background
        graphics.fill(0, 0, this.width, this.height, 0x44000000);

        graphics.drawCenteredString(this.font, "Click and Drag elements to position them", this.width / 2, 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "Press ESC to Save and Close", this.width / 2, 22, 0xFFAAAAAA);

        // --- 1. Logs Remaining ---
        String logsText = "Logs Remaining: 100";
        renderElement(graphics, mouseX, mouseY, config.hudX, config.hudY, this.font.width(logsText), this.font.lineHeight, logsText, config.getTextColor(), Dragging.LOGS);

        // --- 2. Status Message ---
        String statusText = "Tree Log Highlight: §aEnabled";
        renderElement(graphics, mouseX, mouseY, config.statusHudX, config.statusHudY, this.font.width(statusText), this.font.lineHeight, statusText, 0xFFFFFFFF, Dragging.STATUS);

        // --- 3. Edit Button ---
        String buttonText = "[ Edit Tree HUD ]";
        renderElement(graphics, mouseX, mouseY, config.editButtonX, config.editButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, buttonText, 0xFFFFFFFF, Dragging.BUTTON);

        // Render widgets (the Reset Button) manually to avoid calling super.render() which causes blur
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Override with empty method to prevent the 1.21 background blur
    }

    private void renderElement(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, String text, int color, Dragging type) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (hovered || currentDrag == type) {
            graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x44FFFFFF);
        }
        if (type == Dragging.BUTTON) {
            graphics.fill(x, y, x + w, y + h, 0xFF333333);
            graphics.drawCenteredString(this.font, text, x + w / 2, y + (h - 8) / 2, color);
        } else {
            graphics.drawString(this.font, text, x, y, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY, config.hudX, config.hudY, this.font.width("Logs Remaining: 100"), this.font.lineHeight)) {
            currentDrag = Dragging.LOGS;
        } else if (isHovering(mouseX, mouseY, config.statusHudX, config.statusHudY, this.font.width("Tree Log Highlight: §aEnabled"), this.font.lineHeight)) {
            currentDrag = Dragging.STATUS;
        } else if (isHovering(mouseX, mouseY, config.editButtonX, config.editButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            currentDrag = Dragging.BUTTON;
        }

        if (currentDrag != Dragging.NONE) {
            int targetX = getTargetX();
            int targetY = getTargetY();
            dragOffsetX = mouseX - targetX;
            dragOffsetY = mouseY - targetY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getTargetX() {
        if (currentDrag == Dragging.LOGS) return config.hudX;
        if (currentDrag == Dragging.STATUS) return config.statusHudX;
        return config.editButtonX;
    }

    private int getTargetY() {
        if (currentDrag == Dragging.LOGS) return config.hudY;
        if (currentDrag == Dragging.STATUS) return config.statusHudY;
        return config.editButtonY;
    }

    private boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        currentDrag = Dragging.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (currentDrag == Dragging.LOGS) {
            config.hudX = (int) (mouseX - dragOffsetX);
            config.hudY = (int) (mouseY - dragOffsetY);
        } else if (currentDrag == Dragging.STATUS) {
            config.statusHudX = (int) (mouseX - dragOffsetX);
            config.statusHudY = (int) (mouseY - dragOffsetY);
        } else if (currentDrag == Dragging.BUTTON) {
            config.editButtonX = (int) (mouseX - dragOffsetX);
            config.editButtonY = (int) (mouseY - dragOffsetY);
        }

        config.hudX = Math.max(0, Math.min(config.hudX, this.width - 50));
        config.hudY = Math.max(0, Math.min(config.hudY, this.height - 10));
        config.statusHudX = Math.max(0, Math.min(config.statusHudX, this.width - 50));
        config.statusHudY = Math.max(0, Math.min(config.statusHudY, this.height - 10));
        config.editButtonX = Math.max(0, Math.min(config.editButtonX, this.width - BUTTON_WIDTH));
        config.editButtonY = Math.max(0, Math.min(config.editButtonY, this.height - BUTTON_HEIGHT));

        return true;
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}