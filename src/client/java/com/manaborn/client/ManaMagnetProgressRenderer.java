package com.manaborn.client;

import com.manaborn.blocks.entity.ManaMagnetBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Квадрат прогресса на верхней грани магнита маны.
 * Слои: каждая накопленная пылинка — полный слой (чем позже, тем светлее),
 * поверх — растущий слой текущей пылинки. Максимум 5 видимых слоёв.
 */
public class ManaMagnetProgressRenderer
        implements BlockEntityRenderer<ManaMagnetBlockEntity, ManaMagnetProgressRenderer.State> {

    private static final Identifier TEXTURE = Identifier.of("manaborn", "textures/block/mana_magnet_progress.png");
    private static final int STAGES = 45;            // стадий заполнения одного слоя
    private static final int PHASES = 4;             // фаз бегущей волны
    private static final int LAYERS = 5;             // видимых слоёв
    private static final float TOTAL_FRAMES = 1 + LAYERS * STAGES * PHASES; // 901

    public static class State extends BlockEntityRenderState {
        public int dustCount;
        public float timerProgress;
        public long time;
    }

    public ManaMagnetProgressRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(ManaMagnetBlockEntity blockEntity, State state, float tickProgress,
            Vec3d cameraPos, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderer.super.updateRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.dustCount = blockEntity.getDustCount();
        state.timerProgress = blockEntity.getTimerProgress();
        state.time = blockEntity.getWorld() == null ? 0L : blockEntity.getWorld().getTime();
    }

    private static void quad(State state, MatrixStack matrices, OrderedRenderCommandQueue queue,
            int frame, float y) {
        final float v0 = frame / TOTAL_FRAMES;
        final float v1 = (frame + 1) / TOTAL_FRAMES;
        queue.submitCustom(matrices, RenderLayer.getEntityTranslucent(TEXTURE), (entry, vc) -> {
            vc.vertex(entry, 0f, y, 0f).color(255, 255, 255, 255).texture(0f, v0)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(entry, 0f, 1f, 0f);
            vc.vertex(entry, 0f, y, 1f).color(255, 255, 255, 255).texture(0f, v1)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(entry, 0f, 1f, 0f);
            vc.vertex(entry, 1f, y, 1f).color(255, 255, 255, 255).texture(1f, v1)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(entry, 0f, 1f, 0f);
            vc.vertex(entry, 1f, y, 0f).color(255, 255, 255, 255).texture(1f, v0)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(entry, 0f, 1f, 0f);
        });
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {
        // тусклый трек — всегда
        quad(state, matrices, queue, 0, 1.002f);
        // слои накопленной пыли + растущий слой
        int phaseBase = (int) ((state.time / 3L) % PHASES);
        for (int i = 0; i < LAYERS; i++) {
            int stage;
            if (i < state.dustCount) {
                stage = STAGES - 1; // полный слой
            } else if (i == Math.min(state.dustCount, LAYERS - 1)) {
                stage = Math.min(STAGES - 1, (int) (state.timerProgress * STAGES));
                if (stage <= 0) {
                    continue; // пустой слой не рисуем
                }
            } else {
                break;
            }
            int phase = (phaseBase + i) % PHASES; // смещение фаз — волны не совпадают
            int frame = 1 + (i * STAGES + stage) * PHASES + phase;
            quad(state, matrices, queue, frame, 1.002f + (i + 1) * 0.0007f);
        }
    }
}
