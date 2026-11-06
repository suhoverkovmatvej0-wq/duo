package com.duochaos.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

/** Корона Хаоса: пока надета — Сила I. */
public class ChaosCrownItem extends ArmorItem {
    public ChaosCrownItem(Properties props) {
        super(ArmorMaterial.NETHERITE, EquipmentSlotType.HEAD, props);
    }

    @Override
    public void onArmorTick(ItemStack stack, World world, PlayerEntity player) {
        if (!world.isRemote && player.ticksExisted % 40 == 0) {
            player.addPotionEffect(new EffectInstance(Effects.STRENGTH, 100, 0, true, false));
        }
    }
}
