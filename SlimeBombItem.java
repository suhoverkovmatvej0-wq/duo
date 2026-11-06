package com.duochaos.item;

import com.duochaos.entity.SlimeBombEntity;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class SlimeBombItem extends Item {
    public SlimeBombItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.6F, 0.7F);
        if (!world.isRemote) {
            SlimeBombEntity bomb = new SlimeBombEntity(world, player);
            bomb.setItem(stack);
            Vector3d look = player.getLookVec();
            bomb.shoot(look.x, look.y, look.z, 1.3F, 0.5F);
            world.addEntity(bomb);
        }
        if (!player.abilities.isCreativeMode) stack.shrink(1);
        player.getCooldownTracker().setCooldown(this, 10);
        return ActionResult.func_233538_a_(stack, world.isRemote());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Взрывается и высыпает слизней").mergeStyle(TextFormatting.GREEN));
    }
}
