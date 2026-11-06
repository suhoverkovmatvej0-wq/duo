package com.duochaos.client;

import com.duochaos.DuoChaosMod;
import com.duochaos.entity.SlimeBombEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DuoChaosMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        RenderingRegistry.registerEntityRenderingHandler(DuoChaosMod.THIEF.get(), ThiefRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(DuoChaosMod.KILLER_DUCK.get(), DuckRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(DuoChaosMod.CHAOS_KING.get(), KingRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(DuoChaosMod.SLIME_BOMB_ENTITY.get(),
                m -> new SpriteRenderer<SlimeBombEntity>(m, Minecraft.getInstance().getItemRenderer()));
    }
}
