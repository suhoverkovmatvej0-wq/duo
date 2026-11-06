package com.duochaos.client;

import com.duochaos.DuoChaosMod;
import com.duochaos.entity.ThiefEntity;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.util.ResourceLocation;

public class ThiefRenderer extends BipedRenderer<ThiefEntity, BipedModel<ThiefEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation(DuoChaosMod.MODID, "textures/entity/thief.png");

    public ThiefRenderer(EntityRendererManager manager) {
        super(manager, new BipedModel<>(0.0F), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(ThiefEntity entity) {
        return TEX;
    }
}
