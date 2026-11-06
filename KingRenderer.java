package com.duochaos.client;

import com.duochaos.DuoChaosMod;
import com.duochaos.entity.ChaosKingEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.util.ResourceLocation;

public class KingRenderer extends BipedRenderer<ChaosKingEntity, BipedModel<ChaosKingEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation(DuoChaosMod.MODID, "textures/entity/king.png");

    public KingRenderer(EntityRendererManager manager) {
        super(manager, new BipedModel<>(0.0F), 1.2F);
    }

    @Override
    public ResourceLocation getEntityTexture(ChaosKingEntity entity) {
        return TEX;
    }

    /** В финальной фазе (<25% здоровья) король становится гигантом. */
    @Override
    protected void preRenderCallback(ChaosKingEntity e, MatrixStack ms, float partialTicks) {
        float s = (!e.isClone() && e.getHealth() / e.getMaxHealth() < 0.25F) ? 2.8F : 1.8F;
        ms.scale(s, s, s);
    }
}
