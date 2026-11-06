package com.duochaos.client;

import com.duochaos.DuoChaosMod;
import com.duochaos.entity.KillerDuckEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.ChickenModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class DuckRenderer extends MobRenderer<KillerDuckEntity, ChickenModel<KillerDuckEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation(DuoChaosMod.MODID, "textures/entity/duck.png");

    public DuckRenderer(EntityRendererManager manager) {
        super(manager, new ChickenModel<>(), 0.3F);
    }

    @Override
    public ResourceLocation getEntityTexture(KillerDuckEntity entity) {
        return TEX;
    }

    /** Крылья машут — утка в бешенстве. */
    @Override
    protected float handleRotationFloat(KillerDuckEntity e, float partialTicks) {
        return (MathHelper.sin((e.ticksExisted + partialTicks) * 1.2F) + 1.0F) * 0.5F;
    }
}
