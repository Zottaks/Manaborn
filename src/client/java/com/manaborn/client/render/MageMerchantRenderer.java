package com.manaborn.client.render;

import com.manaborn.entities.MageMerchantEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import com.manaborn.client.model.MageModel;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Рендерер для маг-торговца.
 * Использует модель жителя с кастомной текстурой.
 * В 1.21.10 VillagerResemblingModel не generic, использует VillagerEntityRenderState.
 */
public class MageMerchantRenderer extends MobEntityRenderer<MageMerchantEntity, VillagerEntityRenderState, MageModel> {

    // Кастомная текстура маг-торговца с волшебной шляпой
    // Если файл отсутствует, будет использована fallback текстура
    private static final Identifier TEXTURE = Identifier.of("manaborn", "textures/entity/mage.png");

    public MageMerchantRenderer(EntityRendererFactory.Context context) {
        super(context, new MageModel(context.getPart(MageModel.LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(VillagerEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillagerEntityRenderState createRenderState() {
        return new VillagerEntityRenderState();
    }
}
