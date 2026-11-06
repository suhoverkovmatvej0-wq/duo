package com.duochaos.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** ПКМ — переместиться к ближайшему другу. */
public class TeleportStaffItem extends Item {
    public TeleportStaffItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) return ActionResult.func_233538_a_(stack, true);

        ServerPlayerEntity self = (ServerPlayerEntity) player;
        ServerPlayerEntity friend = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayerEntity p : self.getServer().getPlayerList().getPlayers()) {
            if (p == self || !p.isAlive()) continue;
            double d = (p.world == self.world) ? p.getDistanceSq(self) : Double.MAX_VALUE - 1;
            if (d < best) {
                best = d;
                friend = p;
            }
        }
        if (friend == null) {
            self.sendStatusMessage(new StringTextComponent("Друг не найден...").mergeStyle(TextFormatting.RED), true);
            return ActionResult.resultFail(stack);
        }
        ServerWorld from = (ServerWorld) self.world;
        from.spawnParticle(ParticleTypes.PORTAL, self.getPosX(), self.getPosY() + 1, self.getPosZ(), 40, 0.4, 0.8, 0.4, 0.3);
        ServerWorld to = (ServerWorld) friend.world;
        self.teleport(to, friend.getPosX() + 1.5, friend.getPosY(), friend.getPosZ(), self.rotationYaw, self.rotationPitch);
        self.fallDistance = 0;
        to.spawnParticle(ParticleTypes.REVERSE_PORTAL, self.getPosX(), self.getPosY() + 1, self.getPosZ(), 40, 0.4, 0.8, 0.4, 0.2);
        to.playSound(null, self.getPosX(), self.getPosY(), self.getPosZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1F, 1F);
        stack.damageItem(1, player, p -> p.sendBreakAnimation(hand));
        player.getCooldownTracker().setCooldown(this, 100);
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("ПКМ: переместиться к другу").mergeStyle(TextFormatting.GRAY));
    }
}
