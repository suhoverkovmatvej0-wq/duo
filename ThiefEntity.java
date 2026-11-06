package com.duochaos.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** Воришка: крадёт предмет из инвентаря и убегает. Убей его, чтобы вернуть вещь и получить награду. */
public class ThiefEntity extends MonsterEntity {
    private ItemStack stolen = ItemStack.EMPTY;
    private int fleeTicks = 0;

    public ThiefEntity(EntityType<? extends ThiefEntity> type, World world) {
        super(type, world);
    }

    private boolean hasLoot() {
        return !stolen.isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, PlayerEntity.class, 20.0F, 1.1D, 1.5D, e -> hasLoot()));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, e -> !hasLoot()));
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!this.world.isRemote && target instanceof PlayerEntity && stolen.isEmpty()) {
            PlayerEntity p = (PlayerEntity) target;
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
                if (!p.inventory.mainInventory.get(i).isEmpty()) slots.add(i);
            }
            if (!slots.isEmpty()) {
                int s = slots.get(this.rand.nextInt(slots.size()));
                stolen = p.inventory.mainInventory.get(s).copy();
                p.inventory.mainInventory.set(s, ItemStack.EMPTY);
                this.setItemStackToSlot(EquipmentSlotType.MAINHAND, stolen.copy());
                this.setDropChance(EquipmentSlotType.MAINHAND, 0.0F);
                this.setAttackTarget(null);
                p.sendStatusMessage(new StringTextComponent("Воришка украл: ").mergeStyle(TextFormatting.RED)
                        .appendSibling(stolen.getDisplayName()), false);
                this.playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.2F);
            }
            return true;
        }
        return false;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (!this.world.isRemote && hasLoot()) {
            fleeTicks++;
            if (fleeTicks > 600) { // 30 секунд — и он скрылся с добычей
                ((ServerWorld) this.world).spawnParticle(ParticleTypes.POOF, getPosX(), getPosY() + 1, getPosZ(), 20, 0.3, 0.5, 0.3, 0.05);
                this.remove();
            }
        }
    }

    @Override
    public void onDeath(DamageSource src) {
        if (!this.world.isRemote) {
            if (hasLoot()) {
                this.entityDropItem(stolen.copy());
                stolen = ItemStack.EMPTY;
            }
            this.entityDropItem(new ItemStack(Items.EMERALD, 1 + this.rand.nextInt(3)));
            if (this.rand.nextInt(4) == 0) this.entityDropItem(new ItemStack(Items.GOLDEN_APPLE));
        }
        super.onDeath(src);
    }

    @Override
    public void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);
        tag.put("Stolen", stolen.write(new CompoundNBT()));
    }

    @Override
    public void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        stolen = ItemStack.read(tag.getCompound("Stolen"));
    }
}
