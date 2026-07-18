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

    private static final String LOGS_TEXT = "Logs Remaining: 100";
    private static final String STATUS_TEXT = "Tree Log Highlight: §aEnabled";
    private static final String BUTTON_TEXT = "[ Edit Tree HUD ]";

    public HudPositionScreen(Screen parent) {
        super(Component.literal("Adjust HUD Positions"));
        this.parent = parent;
        this.config = TreeLogHighlightClient.getConfig();
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> resetToDefaults()).pos(this.width / 2 - 50, this.height - 30).size(100, 20).build());
    }

    private void resetToDefaults() {
        config.hudPos = new TreeLogHighlightConfig.HudPos(TreeLogHighlightConfig.HudAnchor.TOP_LEFT, 20, 50);
        config.editButtonPos = new TreeLogHighlightConfig.HudPos(TreeLogHighlightConfig.HudAnchor.TOP_LEFT, 10, 5);
        config.statusHudPos = new TreeLogHighlightConfig.HudPos(TreeLogHighlightConfig.HudAnchor.BOTTOM_CENTER, 0, 68);
    }

    // --- Helpers to resolve current on-screen position for each element ---

    private int logsX() { return HudPositionResolver.resolveX(config.hudPos.anchor, config.hudPos.xOffset, this.font.width(LOGS_TEXT), this.width); }
    private int logsY() { return HudPositionResolver.resolveY(config.hudPos.anchor, config.hudPos.yOffset, this.font.lineHeight, this.height); }

    private int statusX() { return HudPositionResolver.resolveX(config.statusHudPos.anchor, config.statusHudPos.xOffset, this.font.width(STATUS_TEXT), this.width); }
    private int statusY() { return HudPositionResolver.resolveY(config.statusHudPos.anchor, config.statusHudPos.yOffset, this.font.lineHeight, this.height); }

    private int buttonX() { return HudPositionResolver.resolveX(config.editButtonPos.anchor, config.editButtonPos.xOffset, BUTTON_WIDTH, this.width); }
    private int buttonY() { return HudPositionResolver.resolveY(config.editButtonPos.anchor, config.editButtonPos.yOffset, BUTTON_HEIGHT, this.height); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x44000000);

        graphics.drawCenteredString(this.font, "Click and Drag elements to position them", this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Press ESC to Save and Close", this.width / 2, 22, 0xAAAAAA);

        // --- 1. Logs Remaining ---
        renderElement(graphics, mouseX, mouseY, logsX(), logsY(), this.font.width(LOGS_TEXT), this.font.lineHeight, LOGS_TEXT, config.getTextColor(), Dragging.LOGS);

        // --- 2. Status Message ---
        renderElement(graphics, mouseX, mouseY, statusX(), statusY(), this.font.width(STATUS_TEXT), this.font.lineHeight, STATUS_TEXT, 0xFFFFFF, Dragging.STATUS);

        // --- 3. Edit Button ---
        renderElement(graphics, mouseX, mouseY, buttonX(), buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_TEXT, 0xFFFFFF, Dragging.BUTTON);

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
        if (isHovering(mouseX, mouseY, logsX(), logsY(), this.font.width(LOGS_TEXT), this.font.lineHeight)) {
            currentDrag = Dragging.LOGS;
        } else if (isHovering(mouseX, mouseY, statusX(), statusY(), this.font.width(STATUS_TEXT), this.font.lineHeight)) {
            currentDrag = Dragging.STATUS;
        } else if (isHovering(mouseX, mouseY, buttonX(), buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
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
        if (currentDrag == Dragging.LOGS) return logsX();
        if (currentDrag == Dragging.STATUS) return statusX();
        return buttonX();
    }

    private int getTargetY() {
        if (currentDrag == Dragging.LOGS) return logsY();
        if (currentDrag == Dragging.STATUS) return statusY();
        return buttonY();
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
        if (currentDrag == Dragging.NONE) {
            return true;
        }

        int elementWidth;
        int elementHeight;
        switch (currentDrag) {
            case LOGS -> { elementWidth = this.font.width(LOGS_TEXT); elementHeight = this.font.lineHeight; }
            case STATUS -> { elementWidth = this.font.width(STATUS_TEXT); elementHeight = this.font.lineHeight; }
            default -> { elementWidth = BUTTON_WIDTH; elementHeight = BUTTON_HEIGHT; }
        }

        int rawX = (int) (mouseX - dragOffsetX);
        int rawY = (int) (mouseY - dragOffsetY);

        // Clamp to screen bounds so elements can't be dragged fully off-screen
        rawX = Math.clamp(rawX, 0, this.width - elementWidth);
        rawY = Math.clamp(rawY, 0, this.height - elementHeight);

        // Convert the raw drop point back into anchor + offset so the saved
        // position survives future resolution/GUI scale/monitor changes too,
        // not just the built-in defaults.
        TreeLogHighlightConfig.HudPos newPos = HudPositionResolver.fromDropPoint(
                rawX, rawY, elementWidth, elementHeight, this.width, this.height);

        switch (currentDrag) {
            case LOGS -> config.hudPos = newPos;
            case STATUS -> config.statusHudPos = newPos;
            case BUTTON -> config.editButtonPos = newPos;
            default -> {}
        }

        return true;
    }

    @Override
    public void onClose() {
        config.save();
        assert this.minecraft != null;
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}