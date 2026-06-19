package net.alek.treeloghighlight.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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

    // Filled quads with depth test — reuse the built-in DEBUG_QUADS pipeline
    // (POSITION_COLOR, QUADS, translucent blend, depth test on, no cull)
    private static final RenderType HIGHLIGHT_FILL = RenderType.create(
            "treeloghighlight_fill",
            256,
            RenderPipelines.DEBUG_QUADS,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    // Filled quads, NO depth test — custom pipeline based on DEBUG_FILLED_SNIPPET
    private static final RenderPipeline PIPELINE_FILL_NODEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("treeloghighlight", "fill_nodepth"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .build()
    );
    private static final RenderType HIGHLIGHT_FILL_NODEPTH = RenderType.create(
            "treeloghighlight_fill_nodepth",
            256,
            PIPELINE_FILL_NODEPTH,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    // Lines with depth test — reuse the built-in LINES pipeline
    // (POSITION_COLOR_NORMAL, LINES, translucent blend, depth test on)
    private static final RenderType HIGHLIGHT_LINES = RenderType.create(
            "treeloghighlight_lines",
            256,
            RenderPipelines.LINES,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    // Lines, NO depth test — custom pipeline based on LINES_SNIPPET
    private static final RenderPipeline PIPELINE_LINES_NODEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("treeloghighlight", "lines_nodepth"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );
    private static final RenderType HIGHLIGHT_LINES_NODEPTH = RenderType.create(
            "treeloghighlight_lines_nodepth",
            256,
            PIPELINE_LINES_NODEPTH,
            RenderType.CompositeState.builder().createCompositeState(false)
    );

    @Override
    public void onInitializeClient() {
        config = TreeLogHighlightConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.treeloghighlight.toggle",
                GLFW.GLFW_KEY_H,
                "category.treeloghighlight"
        ));


        HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(
                        "treeloghighlight",
                        "hud"),
                (guiGraphics, tickCounter) -> {
                    Minecraft client = Minecraft.getInstance();

                    if (client.options.hideGui) {
                        return;
                    }

                    if (config.modEnabled && config.showHud) {
                        Set<BlockPos> logs = TreeLogHighlightManager.getHighlightedLogs();

                        if (!logs.isEmpty()) {
                            guiGraphics.drawString(
                                    client.font,
                                    "Logs Remaining: " + logs.size(),
                                    config.hudX,
                                    config.hudY,
                                    config.getTextColor()
                            );
                        }
                    }

                    if (config.showStatusMessage &&
                            System.currentTimeMillis() - statusMessageTime < 3000) {

                        guiGraphics.drawString(
                                client.font,
                                statusMessageText,
                                config.statusHudX,
                                config.statusHudY,
                                0xFFFFFF
                        );
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

            Set<BlockPos> logs = TreeLogHighlightManager.getHighlightedLogs();
            if (logs.isEmpty()) return;

            float pulse = 1.0f;
            if (config.pulsing) {
                double time = System.currentTimeMillis() / (400.0 / config.pulseSpeed);
                pulse = (float) (Math.sin(time) * (config.pulseIntensity * 0.5) + (1.0 - (config.pulseIntensity * 0.5)));
            }

            RenderType fillLayer  = config.showThroughWalls ? HIGHLIGHT_FILL_NODEPTH  : HIGHLIGHT_FILL;
            RenderType linesLayer = config.showThroughWalls ? HIGHLIGHT_LINES_NODEPTH : HIGHLIGHT_LINES;

            // Batch Render Full Blocks
            if (config.renderMode == TreeLogHighlightConfig.RenderMode.FULL
                    || config.renderMode == TreeLogHighlightConfig.RenderMode.BOTH) {
                ByteBufferBuilder byteBuffer = new ByteBufferBuilder(fillLayer.bufferSize());
                BufferBuilder bufferBuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                boolean hasData = false;
                for (BlockPos pos : logs) {
                    assert matrixStack != null;
                    matrixStack.pushPose();
                    matrixStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                    if (drawBox(client.level, pos, bufferBuilder, matrixStack.last().pose(),
                            0, 0, 0, 1, 1, 1,
                            config.getFillR(), config.getFillG(), config.getFillB(), config.alpha * pulse)) {
                        hasData = true;
                    }
                    matrixStack.popPose();
                }
                if (hasData) {
                    MeshData meshData = bufferBuilder.build();
                    if (meshData != null) fillLayer.draw(meshData);
                }
                byteBuffer.close();
            }

            // Batch Render Outlines
            // LINES pipeline requires POSITION_COLOR_NORMAL vertex format
            if (config.renderMode == TreeLogHighlightConfig.RenderMode.OUTLINE
                    || config.renderMode == TreeLogHighlightConfig.RenderMode.BOTH) {
                ByteBufferBuilder byteBuffer = new ByteBufferBuilder(linesLayer.bufferSize());
                BufferBuilder lineBuilder = new BufferBuilder(byteBuffer, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                for (BlockPos pos : logs) {
                    assert matrixStack != null;
                    matrixStack.pushPose();
                    matrixStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                    renderLineBox(matrixStack, lineBuilder, 0, 0, 0, 1, 1, 1,
                            config.getOutlineR(), config.getOutlineG(), config.getOutlineB(), 0.5f * pulse);
                    matrixStack.popPose();
                }
                MeshData meshData = lineBuilder.build();
                if (meshData != null) linesLayer.draw(meshData);
                byteBuffer.close();
            }
        });
    }

    public static TreeLogHighlightConfig getConfig() {
        return config;
    }

    /**
     * Renders a line-box outline. Uses POSITION_COLOR_NORMAL format required by the LINES pipeline.
     * Each line segment needs two vertices; normals are set per-segment direction.
     */
    private static void renderLineBox(PoseStack poseStack, VertexConsumer buffer,
                                      double x1, double y1, double z1, double x2, double y2, double z2,
                                      float r, float g, float b, float a) {
        Matrix4f m = poseStack.last().pose();
        float fx1 = (float)x1, fy1 = (float)y1, fz1 = (float)z1;
        float fx2 = (float)x2, fy2 = (float)y2, fz2 = (float)z2;

        // Bottom face
        line(buffer, m, fx1,fy1,fz1, fx2,fy1,fz1, r,g,b,a, 1,0,0);
        line(buffer, m, fx2,fy1,fz1, fx2,fy1,fz2, r,g,b,a, 0,0,1);
        line(buffer, m, fx2,fy1,fz2, fx1,fy1,fz2, r,g,b,a, -1,0,0);
        line(buffer, m, fx1,fy1,fz2, fx1,fy1,fz1, r,g,b,a, 0,0,-1);
        // Top face
        line(buffer, m, fx1,fy2,fz1, fx2,fy2,fz1, r,g,b,a, 1,0,0);
        line(buffer, m, fx2,fy2,fz1, fx2,fy2,fz2, r,g,b,a, 0,0,1);
        line(buffer, m, fx2,fy2,fz2, fx1,fy2,fz2, r,g,b,a, -1,0,0);
        line(buffer, m, fx1,fy2,fz2, fx1,fy2,fz1, r,g,b,a, 0,0,-1);
        // Verticals
        line(buffer, m, fx1,fy1,fz1, fx1,fy2,fz1, r,g,b,a, 0,1,0);
        line(buffer, m, fx2,fy1,fz1, fx2,fy2,fz1, r,g,b,a, 0,1,0);
        line(buffer, m, fx2,fy1,fz2, fx2,fy2,fz2, r,g,b,a, 0,1,0);
        line(buffer, m, fx1,fy1,fz2, fx1,fy2,fz2, r,g,b,a, 0,1,0);
    }

    private static void line(VertexConsumer buffer, Matrix4f m,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b, float a,
                             float nx, float ny, float nz) {
        buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }

    private boolean drawBox(Level world, BlockPos pos, VertexConsumer buffer, Matrix4f matrix,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float r, float g, float b, float a) {
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