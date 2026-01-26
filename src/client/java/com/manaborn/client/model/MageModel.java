package com.manaborn.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class MageModel extends EntityModel<LivingEntityRenderState> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of("manaborn", "mage"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MageModel(ModelPart root) {
        super(root);
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("head", ModelPartBuilder.create()
                .uv(0, 11).cuboid(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-5.0F, -33.0F, -5.0F, 10.0F, 1.0F, 10.0F, new Dilation(0.0F))
                .uv(0, 27).cuboid(-4.0F, -37.0F, -4.0F, 8.0F, 4.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 39).cuboid(-3.0F, -41.0F, -3.0F, 6.0F, 4.0F, 6.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("body", ModelPartBuilder.create()
                .uv(32, 31).cuboid(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.0F))
                .uv(54, 16).cuboid(0.0F, -24.0F, -2.5F, 4.0F, 3.0F, 5.0F, new Dilation(0.0F))
                .uv(57, 24).cuboid(-4.0F, -24.0F, -2.5F, 4.0F, 3.0F, 5.0F, new Dilation(0.0F))
                .uv(32, 11).cuboid(-5.0F, -24.0F, 3.0F, 10.0F, 19.0F, 1.0F, new Dilation(0.0F))
                .uv(16, 49).cuboid(5.0F, -23.0F, 2.0F, 1.0F, 12.0F, 1.0F, new Dilation(0.0F))
                .uv(20, 49).cuboid(-6.0F, -23.0F, 2.0F, 1.0F, 12.0F, 1.0F, new Dilation(0.0F))
                .uv(56, 32).cuboid(-5.0F, -24.0F, 2.0F, 10.0F, 2.0F, 1.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("left_arm", ModelPartBuilder.create()
                .uv(24, 47).cuboid(-8.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("right_arm", ModelPartBuilder.create()
                .uv(40, 47).cuboid(4.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("left_leg", ModelPartBuilder.create()
                .uv(0, 49).cuboid(-4.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        root.addChild("right_leg", ModelPartBuilder.create()
                .uv(54, 0).cuboid(0.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
            ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(LivingEntityRenderState state) {
        float limbAngle = state.limbSwingAnimationProgress * 0.4F;
        float limbDistance = state.limbSwingAmplitude * 0.15F;

        rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.8F * limbDistance;
        leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 0.8F * limbDistance;
        rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 0.8F * limbDistance;
        leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.8F * limbDistance;

        rightArm.yaw = 0.0F;
        leftArm.yaw = 0.0F;
        rightLeg.yaw = 0.0F;
        leftLeg.yaw = 0.0F;
    }

}
