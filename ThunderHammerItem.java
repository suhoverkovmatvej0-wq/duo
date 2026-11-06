package com.duochaos.item;

import com.duochaos.event.ChaosUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** Молот грома: бьёт молнией и отбрасывает мобов вокруг цели. */
public class ThunderHammerItem extends SwordItem {
    private final float boltDamage;

    public ThunderHammerItem(IItemTier tier, int dmg, float speed, float boltDamage, Properties props) {
        super(tier, dmg, speed, props);
        this.boltDamage = boltDamage;
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isRemote) {
            ServerWorld w = (ServerWorld) attacker.world;
            ChaosUtil.strike(w, target.getPosX(), target.getPosY(), target.getPosZ(), boltDamage, attacker, false);
            for (LivingEntity e : w.getEntitiesWithinAABB(LivingEntity.class, target.getBoundingBox().grow(4.0D), x -> x != attacker)) {
                e.applyKnockback(1.3F, attacker.getPosX() - e.getPosX(), attacker.getPosZ() - e.getPosZ());
            }
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Удар вызывает молнию и отбрасывает врагов").mergeStyle(TextFormatting.YELLOW));
    }
}
