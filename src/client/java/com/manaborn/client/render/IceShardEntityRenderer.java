package com.manaborn.client.render;

import com.manaborn.entities.IceShardEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;

/**
 * Рендерер ледяного осколка: рисует 3D-модель предмета, повёрнутую
 * по направлению полёта (остриём вперёд), вместо билборда к камере.
 */
public class IceShardEntityRenderer extends EntityRenderer<IceShardEntity, IceShardEntityRenderer.State> {

    /** Render state с интерполированными углами полёта */
    public static class State extends FlyingItemEntityRenderState {
        public float yaw;
        public float pitch;
    }

    private final ItemModelManager itemModelManager;

    public IceShardEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(IceShardEntity entity, State state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.yaw = entity.getLerpedYaw(tickProgress);
        state.pitch = entity.getLerpedPitch(tickProgress);
        this.itemModelManager.updateForNonLivingEntity(state.itemRenderState, entity.getStack(),
                ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {
        matrices.push();
        // Модель вытянута вверх (+Y): поворачиваем так, чтобы «вверх»
        // стал направлением полёта снаряда
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.yaw - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.pitch + 90.0F));
        state.itemRenderState.render(matrices, queue, state.light, OverlayTexture.DEFAULT_UV,
                state.outlineColor);
        matrices.pop();
        super.render(state, matrices, queue, cameraState);
    }
}
