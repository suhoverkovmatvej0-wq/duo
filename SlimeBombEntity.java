package com.duochaos.entity;

import com.duochaos.DuoChaosMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

public class SlimeBombEntity extends ProjectileItemEntity {
    public SlimeBombEntity(EntityType<? extends SlimeBombEntity> type, World world) {
        super(type, world);
    }

    public SlimeBombEntity(World world, LivingEntity thrower) {
        super(DuoChaosMod.SLIME_BOMB_ENTITY.get(), thrower, world);
    }

    @Override
    protected Item getDefaultItem() {
        return DuoChaosMod.SLIME_BOMB.get();
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        super.onImpact(result);
        if (!this.world.isRemote) {
            ServerWorld w = (ServerWorld) this.world;
            w.createExplosion(this, getPosX(), getPosY(), getPosZ(), 1.5F, Explosion.Mode.NONE);
            for (int i = 0; i < 5; i++) {
                SlimeEntity s = EntityType.SLIME.create(w);
                if (s == null) continue;
                s.setSlimeSize(1, true);
                s.setLocationAndAngles(getPosX() + (this.rand.nextDouble() - 0.5) * 2, getPosY() + 0.3,
                        getPosZ() + (this.rand.nextDouble() - 0.5) * 2, this.rand.nextFloat() * 360F, 0F);
                w.addEntity(s);
            }
            this.remove();
        }
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
