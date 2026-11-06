package com.duochaos;

import com.duochaos.entity.ChaosKingEntity;
import com.duochaos.entity.KillerDuckEntity;
import com.duochaos.entity.SlimeBombEntity;
import com.duochaos.entity.ThiefEntity;
import com.duochaos.event.ChaosCommands;
import com.duochaos.event.ChaosEvents;
import com.duochaos.event.Missions;
import com.duochaos.item.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.item.Rarity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(DuoChaosMod.MODID)
public class DuoChaosMod {
    public static final String MODID = "duo_chaos";

    /** С какого дня Печать Хаоса открывает путь к Королю (в творчестве работает всегда). */
    public static final int SIGIL_MIN_DAY = 25;

    public static final RegistryKey<World> CHAOS_REALM =
            RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(MODID, "chaos_realm"));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);

    public static final ItemGroup TAB = new ItemGroup("duo_chaos") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(RAINBOW_SWORD.get());
        }
    };

    // ---------- Предметы ----------
    public static final RegistryObject<Item> RAINBOW_SWORD = ITEMS.register("rainbow_sword",
            () -> new RainbowSwordItem(new Item.Properties().group(TAB).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> TELEPORT_STAFF = ITEMS.register("teleport_staff",
            () -> new TeleportStaffItem(new Item.Properties().group(TAB).maxStackSize(1).maxDamage(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SLIME_BOMB = ITEMS.register("slime_bomb",
            () -> new SlimeBombItem(new Item.Properties().group(TAB).maxStackSize(16)));
    public static final RegistryObject<Item> GRAPPLING_HOOK = ITEMS.register("grappling_hook",
            () -> new GrapplingHookItem(new Item.Properties().group(TAB).maxStackSize(1).maxDamage(256)));
    public static final RegistryObject<Item> THUNDER_HAMMER = ITEMS.register("thunder_hammer",
            () -> new ThunderHammerItem(ItemTier.IRON, 6, -3.1F, 5.0F, new Item.Properties().group(TAB).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> KING_HAMMER = ITEMS.register("king_hammer",
            () -> new ThunderHammerItem(ItemTier.NETHERITE, 8, -3.0F, 9.0F, new Item.Properties().group(TAB).rarity(Rarity.EPIC).isImmuneToFire()));
    public static final RegistryObject<Item> CHAOS_CROWN = ITEMS.register("chaos_crown",
            () -> new ChaosCrownItem(new Item.Properties().group(TAB).rarity(Rarity.EPIC).isImmuneToFire()));
    public static final RegistryObject<Item> CHAOS_SIGIL = ITEMS.register("chaos_sigil",
            () -> new ChaosSigilItem(new Item.Properties().group(TAB).maxStackSize(4).rarity(Rarity.EPIC)));

    // ---------- Существа ----------
    public static final RegistryObject<EntityType<ThiefEntity>> THIEF = ENTITIES.register("thief",
            () -> EntityType.Builder.<ThiefEntity>create(ThiefEntity::new, EntityClassification.MONSTER)
                    .size(0.6F, 1.8F).build(MODID + ":thief"));
    public static final RegistryObject<EntityType<KillerDuckEntity>> KILLER_DUCK = ENTITIES.register("killer_duck",
            () -> EntityType.Builder.<KillerDuckEntity>create(KillerDuckEntity::new, EntityClassification.MONSTER)
                    .size(0.5F, 0.7F).build(MODID + ":killer_duck"));
    public static final RegistryObject<EntityType<ChaosKingEntity>> CHAOS_KING = ENTITIES.register("chaos_king",
            () -> EntityType.Builder.<ChaosKingEntity>create(ChaosKingEntity::new, EntityClassification.MONSTER)
                    .size(1.2F, 3.4F).immuneToFire().build(MODID + ":chaos_king"));
    public static final RegistryObject<EntityType<SlimeBombEntity>> SLIME_BOMB_ENTITY = ENTITIES.register("slime_bomb",
            () -> EntityType.Builder.<SlimeBombEntity>create(SlimeBombEntity::new, EntityClassification.MISC)
                    .size(0.25F, 0.25F).build(MODID + ":slime_bomb"));

    public DuoChaosMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::onAttributes);

        MinecraftForge.EVENT_BUS.register(ChaosEvents.class);
        MinecraftForge.EVENT_BUS.register(Missions.class);
        MinecraftForge.EVENT_BUS.register(ChaosCommands.class);
    }

    private void onAttributes(EntityAttributeCreationEvent e) {
        e.put(THIEF.get(), MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 24.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.32D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 1.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 24.0D)
                .create());
        e.put(KILLER_DUCK.get(), MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 10.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.36D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 3.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 32.0D)
                .create());
        e.put(CHAOS_KING.get(), MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 400.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.30D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 9.0D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D)
                .create());
    }
}
