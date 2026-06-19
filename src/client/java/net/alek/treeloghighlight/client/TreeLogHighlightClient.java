package net.alek.treeloghighlight.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class TreeLogHighlightClient implements ClientModInitializer {
    private static TreeLogHighlightConfig config;
    private static KeyMapping toggleKey;
    
    private static long statusMessageTime = 0;
    private static String statusMessageText = "";

    @Override
    public void onInitializeClient() {
        config = TreeLogHighlightConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.treeloghighlight.toggle",
                GLFW.GLFW_KEY_H,
                "category.treeloghighlight"
        ));

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui) return;

            if (config.modEnabled && config.showHud) {
                Set<BlockPos> logs = TreeLogHighlightManager.getHighlightedLogs();
                if (!logs.isEmpty()) {
                    String text = "Logs Remaining: " + logs.size();
                    guiGraphics.drawString(client.font, text, config.hudX, config.hudY, config.getTextColor());
                }
            }

            if (config.showStatusMessage && System.currentTimeMillis() - statusMessageTime < 3000) {
                guiGraphics.drawString(client.font, statusMessageText, config.statusHudX, config.statusHudY, 0xFFFFFF);
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            Minecraft client = Minecraft.getInstance();

            while (toggleKey.consumeClick()) {
                config.modEnabled = !config.modEnabled;
                config.save();
                if (config.showStatusMessage) {
                    statusMessageText = "Tree Log Highlight: " + (config.modEnabled ? "§aEnabled" : "§cDisabled");
                    statusMessageTime = System.currentTimeMillis();
                }
            }

            if (!config.modEnabled || client.level == null || client.player == null) return;

            PoseStack matrixStack = context.matrixStack();
            Vec3 cameraPos = context.camera().getPosition();

            if (config.showThroughWalls) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);

            Tesselator tesselator = Tesselator.getInstance();
            Set<BlockPos> logs = TreeLogHighlightManager.getHighlightedLogs();
            if (logs.isEmpty()) {
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                return;
            }

            float pulse = 1.0f;
            if (config.pulsing) {
                double time = System.currentTimeMillis() / (400.0 / config.pulseSpeed);
                pulse = (float) (Math.sin(time) * (config.pulseIntensity * 0.5) + (1.0 - (config.pulseIntensity * 0.5)));
            }

            // Batch Render Full Blocks
            if (config.renderMode == TreeLogHighlightConfig.RenderMode.FULL || config.renderMode == TreeLogHighlightConfig.RenderMode.BOTH) {
                BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                boolean hasData = false;
                for (BlockPos pos : logs) {
                    matrixStack.pushPose();
                    matrixStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                    if (drawBox(client.level, pos, bufferBuilder, matrixStack.last().pose(), 0, 0, 0, 1, 1, 1, config.getFillR(), config.getFillG(), config.getFillB(), config.alpha * pulse)) {
                        hasData = true;
                    }
                    matrixStack.popPose();
                }
                if (hasData) {
                    MeshData meshData = bufferBuilder.build();
                    if (meshData != null) BufferUploader.drawWithShader(meshData);
                }
            }

            // Batch Render Outlines
            if (config.renderMode == TreeLogHighlightConfig.RenderMode.OUTLINE || config.renderMode == TreeLogHighlightConfig.RenderMode.BOTH) {
                BufferBuilder lineBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                for (BlockPos pos : logs) {
                    matrixStack.pushPose();
                    matrixStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                    ShapeRenderer.renderLineBox(matrixStack, lineBuilder, 0, 0, 0, 1, 1, 1, config.getOutlineR(), config.getOutlineG(), config.getOutlineB(), 0.5f * pulse);
                    matrixStack.popPose();
                }
                MeshData meshData = lineBuilder.build();
                if (meshData != null) BufferUploader.drawWithShader(meshData);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    public static TreeLogHighlightConfig getConfig() {
        return config;
    }

    private boolean drawBox(Level world, BlockPos pos, VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        boolean drawn = false;
        if (shouldRenderFace(world, pos, Direction.DOWN)) {
            buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a);
            drawn = true;
        }
        if (shouldRenderFace(world, pos, Direction.UP)) {
            buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a);
            drawn = true;
        }
        if (shouldRenderFace(world, pos, Direction.NORTH)) {
            buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a);
            drawn = true;
        }
        if (shouldRenderFace(world, pos, Direction.SOUTH)) {
            buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a);
            drawn = true;
        }
        if (shouldRenderFace(world, pos, Direction.WEST)) {
            buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a);
            drawn = true;
        }
        if (shouldRenderFace(world, pos, Direction.EAST)) {
            buffer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
            buffer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a);
            drawn = true;
        }
        return drawn;
    }

    private boolean shouldRenderFace(Level world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        if (TreeLogHighlightManager.isHighlighted(neighborPos)) return false;
        BlockState state = world.getBlockState(neighborPos);
        return !state.isSolidRender();
    }
}