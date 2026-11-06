package com.duochaos.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.item.SwordItem;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** Каждый удар даёт случайный эффект. */
public class RainbowSwordItem extends SwordItem {
    public RainbowSwordItem(Properties props) {
        super(ItemTier.DIAMOND, 3, -2.4F, props);
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isRemote) {
            switch (attacker.getRNG().nextInt(8)) {
                case 0: target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 100, 2)); break;
                case 1: target.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 120, 1)); break;
                case 2: target.addPotionEffect(new EffectInstance(Effects.POISON, 100, 1)); break;
                case 3: target.addPotionEffect(new EffectInstance(Effects.LEVITATION, 60, 0)); break;
                case 4: target.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 80, 0)); break;
                case 5: target.addPotionEffect(new EffectInstance(Effects.GLOWING, 200, 0)); break;
                case 6: target.setFire(6); break;
                default: attacker.addPotionEffect(new EffectInstance(Effects.REGENERATION, 80, 1)); break;
            }
            ((ServerWorld) attacker.world).spawnParticle(ParticleTypes.END_ROD, target.getPosX(), target.getPosY() + 1,
                    target.getPosZ(), 12, 0.3, 0.5, 0.3, 0.05);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Каждый удар — случайный эффект").mergeStyle(TextFormatting.LIGHT_PURPLE));
    }
}
