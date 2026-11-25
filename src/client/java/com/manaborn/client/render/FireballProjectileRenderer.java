package com.manaborn.client.render;

import com.manaborn.entities.FireballProjectileEntity;
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
 * Рендерер огненной сферы: рисует 3D-модель предмета fire_orb,
 * повернутую по направлению полёта, плюс медленное вращение сферы
 * для эффекта кипящей магмы.
 */
public class FireballProjectileRenderer
        extends EntityRenderer<FireballProjectileEntity, FireballProjectileRenderer.State> {

    /** Render state с интерполированными углами полёта */
    public static class State extends FlyingItemEntityRenderState {
        public float yaw;
        public float pitch;
        public float scale = 1.0F;
    }

    private final ItemModelManager itemModelManager;

    public FireballProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(FireballProjectileEntity entity, State state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.yaw = entity.getLerpedYaw(tickProgress);
        state.pitch = entity.getLerpedPitch(tickProgress);
        // Размер сферы по уровню заклинания: 1 — маленькая, 2 — средняя, 3 — большая
        state.scale = switch (entity.getSpellLevel()) {
            case 1 -> 0.65F;
            case 2 -> 0.9F;
            default -> 1.2F;
        };
        this.itemModelManager.updateForNonLivingEntity(state.itemRenderState, entity.getStack(),
                ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue,
            CameraRenderState cameraState) {
        matrices.push();
        // Ориентация по направлению полёта
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.yaw - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.pitch + 90.0F));
        // Медленное вращение сферы — «живая» магма
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * 8.0F));
        // Размер по уровню заклинания
        matrices.scale(state.scale, state.scale, state.scale);
        state.itemRenderState.render(matrices, queue, state.light, OverlayTexture.DEFAULT_UV,
                state.outlineColor);
        matrices.pop();
        super.render(state, matrices, queue, cameraState);
    }
}
