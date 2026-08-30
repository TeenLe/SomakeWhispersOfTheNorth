package com.somake.wotn.registry;

import com.somake.wotn.WhispersOfTheNorth;
import com.somake.wotn.block.BilberryBushBlock;
import com.somake.wotn.block.AlchemistCauldronBlock;
import com.somake.wotn.block.BookPileBlock;
import com.somake.wotn.block.FlammableBlocks;
import com.somake.wotn.block.PotionDisplayBlock;
import com.somake.wotn.block.FenrirSealBlock;
import com.somake.wotn.block.GolemEncounterControllerBlock;
import com.somake.wotn.item.AlchemistCauldronBlockItem;
import com.somake.wotn.worldgen.ModTreeGrowers;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WhispersOfTheNorth.MODID);

    public static final DeferredBlock<FlammableBlocks.Pillar> YGGDRASIL_LOG = registerBlock(
            "yggdrasil_log", FlammableBlocks.Pillar::new, Blocks.OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> YGGDRASIL_WOOD = registerBlock(
            "yggdrasil_wood", FlammableBlocks.Pillar::new, Blocks.OAK_WOOD);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_YGGDRASIL_LOG = registerBlock(
            "stripped_yggdrasil_log", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_YGGDRASIL_WOOD = registerBlock(
            "stripped_yggdrasil_wood", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_WOOD);

    public static final DeferredBlock<FlammableBlocks.Basic> YGGDRASIL_PLANKS = registerBlock(
            "yggdrasil_planks", FlammableBlocks.Basic::new, Blocks.OAK_PLANKS);
    public static final DeferredBlock<FlammableBlocks.Stairs> YGGDRASIL_STAIRS = registerBlock(
            "yggdrasil_stairs",
            properties -> new FlammableBlocks.Stairs(YGGDRASIL_PLANKS.get().defaultBlockState(), properties),
            Blocks.OAK_STAIRS);
    public static final DeferredBlock<FlammableBlocks.Slab> YGGDRASIL_SLAB = registerBlock(
            "yggdrasil_slab", FlammableBlocks.Slab::new, Blocks.OAK_SLAB);
    public static final DeferredBlock<FlammableBlocks.Fence> YGGDRASIL_FENCE = registerBlock(
            "yggdrasil_fence", FlammableBlocks.Fence::new, Blocks.OAK_FENCE);
    public static final DeferredBlock<FlammableBlocks.FenceGate> YGGDRASIL_FENCE_GATE = registerBlock(
            "yggdrasil_fence_gate",
            properties -> new FlammableBlocks.FenceGate(ModWoodTypes.YGGDRASIL, properties),
            Blocks.OAK_FENCE_GATE);
    public static final DeferredBlock<FlammableBlocks.Door> YGGDRASIL_DOOR = registerBlock(
            "yggdrasil_door",
            properties -> new FlammableBlocks.Door(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_DOOR);
    public static final DeferredBlock<FlammableBlocks.Trapdoor> YGGDRASIL_TRAPDOOR = registerBlock(
            "yggdrasil_trapdoor",
            properties -> new FlammableBlocks.Trapdoor(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_TRAPDOOR);
    public static final DeferredBlock<FlammableBlocks.PressurePlate> YGGDRASIL_PRESSURE_PLATE = registerBlock(
            "yggdrasil_pressure_plate",
            properties -> new FlammableBlocks.PressurePlate(ModWoodTypes.YGGDRASIL_SET, properties),
            Blocks.OAK_PRESSURE_PLATE);
    public static final DeferredBlock<FlammableBlocks.Button> YGGDRASIL_BUTTON = registerBlock(
            "yggdrasil_button",
            properties -> new FlammableBlocks.Button(ModWoodTypes.YGGDRASIL_SET, 30, properties),
            Blocks.OAK_BUTTON);

    public static final DeferredBlock<FlammableBlocks.Leaves> YGGDRASIL_LEAVES = registerBlock(
            "yggdrasil_leaves",
            properties -> new FlammableBlocks.Leaves(0.01F, ParticleTypes.PALE_OAK_LEAVES, properties),
            Blocks.PALE_OAK_LEAVES);
    public static final DeferredBlock<SaplingBlock> YGGDRASIL_SAPLING = registerBlock(
            "yggdrasil_sapling",
            properties -> new SaplingBlock(ModTreeGrowers.YGGDRASIL, properties),
            Blocks.OAK_SAPLING);
    public static final DeferredBlock<FlammableBlocks.LeafLitter> YGGDRASIL_LEAF_LITTER = registerBlock(
            "yggdrasil_leaf_litter", FlammableBlocks.LeafLitter::new, Blocks.LEAF_LITTER);
    public static final DeferredBlock<FlammableBlocks.Shelf> YGGDRASIL_SHELF = registerBlock(
            "yggdrasil_shelf", FlammableBlocks.Shelf::new, Blocks.OAK_SHELF);

    public static final DeferredBlock<FlammableBlocks.Pillar> IDUNN_LOG = registerBlock(
            "idunn_log", FlammableBlocks.Pillar::new, Blocks.OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> IDUNN_WOOD = registerBlock(
            "idunn_wood", FlammableBlocks.Pillar::new, Blocks.OAK_WOOD);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_IDUNN_LOG = registerBlock(
            "stripped_idunn_log", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_LOG);
    public static final DeferredBlock<FlammableBlocks.Pillar> STRIPPED_IDUNN_WOOD = registerBlock(
            "stripped_idunn_wood", FlammableBlocks.Pillar::new, Blocks.STRIPPED_OAK_WOOD);

    public static final DeferredBlock<FlammableBlocks.Basic> IDUNN_PLANKS = registerBlock(
            "idunn_planks", FlammableBlocks.Basic::new, Blocks.OAK_PLANKS);
    public static final DeferredBlock<FlammableBlocks.Stairs> IDUNN_STAIRS = registerBlock(
            "idunn_stairs",
            properties -> new FlammableBlocks.Stairs(IDUNN_PLANKS.get().defaultBlockState(), properties),
            Blocks.OAK_STAIRS);
    public static final DeferredBlock<FlammableBlocks.Slab> IDUNN_SLAB = registerBlock(
            "idunn_slab", FlammableBlocks.Slab::new, Blocks.OAK_SLAB);
    public static final DeferredBlock<FlammableBlocks.Fence> IDUNN_FENCE = registerBlock(
            "idunn_fence", FlammableBlocks.Fence::new, Blocks.OAK_FENCE);
    public static final DeferredBlock<FlammableBlocks.FenceGate> IDUNN_FENCE_GATE = registerBlock(
            "idunn_fence_gate",
            properties -> new FlammableBlocks.FenceGate(ModWoodTypes.IDUNN, properties),
            Blocks.OAK_FENCE_GATE);
    public static final DeferredBlock<FlammableBlocks.Door> IDUNN_DOOR = registerBlock(
            "idunn_door",
            properties -> new FlammableBlocks.Door(ModWoodTypes.IDUNN_SET, properties),
            Blocks.OAK_DOOR);
    public static final DeferredBlock<FlammableBlocks.Trapdoor> IDUNN_TRAPDOOR = registerBlock(
            "idunn_trapdoor",
            properties -> new FlammableBlocks.Trapdoor(ModWoodTypes.IDUNN_SET, properties),
            Blocks.OAK_TRAPDOOR);
    public static final DeferredBlock<FlammableBlocks.PressurePlate> IDUNN_PRESSURE_PLATE = registerBlock(
            "idunn_pressure_plate",
            properties -> new FlammableBlocks.PressurePlate(ModWoodTypes.IDUNN_SET, properties),
            Blocks.OAK_PRESSURE_PLATE);
    public static final DeferredBlock<FlammableBlocks.Button> IDUNN_BUTTON = registerBlock(
            "idunn_button",
            properties -> new FlammableBlocks.Button(ModWoodTypes.IDUNN_SET, 30, properties),
            Blocks.OAK_BUTTON);

    public static final DeferredBlock<FlammableBlocks.Leaves> IDUNN_LEAVES = registerBlock(
            "idunn_leaves",
            properties -> new FlammableBlocks.Leaves(
                    0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xE0CD00), properties),
            Blocks.PALE_OAK_LEAVES);
    public static final DeferredBlock<SaplingBlock> IDUNN_SAPLING = registerBlock(
            "idunn_sapling",
            properties -> new SaplingBlock(ModTreeGrowers.IDUNN, properties),
            Blocks.OAK_SAPLING);
    public static final DeferredBlock<FlammableBlocks.LeafLitter> IDUNN_LEAF_LITTER = registerBlock(
            "idunn_leaf_litter", FlammableBlocks.LeafLitter::new, Blocks.LEAF_LITTER);

    public static final DeferredBlock<BookPileBlock> BOOK_PILE = BLOCKS.registerBlock(
            "book_pile", BookPileBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).noOcclusion());
    public static final DeferredBlock<PotionDisplayBlock> POTION_DISPLAY = BLOCKS.registerBlock(
            "potion_display",
            PotionDisplayBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .noLootTable()
                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<BilberryBushBlock> BILBERRY_BUSH = registerBlock(
            "bilberry_bush", BilberryBushBlock::new, Blocks.SWEET_BERRY_BUSH);
    public static final DeferredBlock<AlchemistCauldronBlock> ALCHEMIST_CAULDRON = BLOCKS.registerBlock(
            "alchemist_cauldron",
            AlchemistCauldronBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
                    .strength(3.5F, 6.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<FenrirSealBlock> FENRIR_SEAL = BLOCKS.registerBlock(
            "fenrir_seal",
            FenrirSealBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .sound(net.minecraft.world.level.block.SoundType.DEEPSLATE)
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<GolemEncounterControllerBlock> GOLEM_ENCOUNTER_CONTROLLER = BLOCKS.registerBlock(
            "golem_encounter_controller",
            GolemEncounterControllerBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.NONE)
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> YGGDRASIL_LOG_ITEM = registerItem(YGGDRASIL_LOG);
    public static final DeferredItem<BlockItem> YGGDRASIL_WOOD_ITEM = registerItem(YGGDRASIL_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_YGGDRASIL_LOG_ITEM = registerItem(STRIPPED_YGGDRASIL_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_YGGDRASIL_WOOD_ITEM = registerItem(STRIPPED_YGGDRASIL_WOOD);
    public static final DeferredItem<BlockItem> YGGDRASIL_PLANKS_ITEM = registerItem(YGGDRASIL_PLANKS);
    public static final DeferredItem<BlockItem> YGGDRASIL_STAIRS_ITEM = registerItem(YGGDRASIL_STAIRS);
    public static final DeferredItem<BlockItem> YGGDRASIL_SLAB_ITEM = registerItem(YGGDRASIL_SLAB);
    public static final DeferredItem<BlockItem> YGGDRASIL_FENCE_ITEM = registerItem(YGGDRASIL_FENCE);
    public static final DeferredItem<BlockItem> YGGDRASIL_FENCE_GATE_ITEM = registerItem(YGGDRASIL_FENCE_GATE);
    public static final DeferredItem<BlockItem> YGGDRASIL_DOOR_ITEM = registerItem(YGGDRASIL_DOOR);
    public static final DeferredItem<BlockItem> YGGDRASIL_TRAPDOOR_ITEM = registerItem(YGGDRASIL_TRAPDOOR);
    public static final DeferredItem<BlockItem> YGGDRASIL_PRESSURE_PLATE_ITEM = registerItem(YGGDRASIL_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> YGGDRASIL_BUTTON_ITEM = registerItem(YGGDRASIL_BUTTON);
    public static final DeferredItem<BlockItem> YGGDRASIL_LEAVES_ITEM = registerItem(YGGDRASIL_LEAVES);
    public static final DeferredItem<BlockItem> YGGDRASIL_SAPLING_ITEM = registerItem(YGGDRASIL_SAPLING);
    public static final DeferredItem<BlockItem> YGGDRASIL_LEAF_LITTER_ITEM = registerItem(YGGDRASIL_LEAF_LITTER);
    public static final DeferredItem<BlockItem> YGGDRASIL_SHELF_ITEM = ModItems.ITEMS.registerSimpleBlockItem(
            YGGDRASIL_SHELF,
            properties -> properties.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    public static final DeferredItem<BlockItem> IDUNN_LOG_ITEM = registerItem(IDUNN_LOG);
    public static final DeferredItem<BlockItem> IDUNN_WOOD_ITEM = registerItem(IDUNN_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_IDUNN_LOG_ITEM = registerItem(STRIPPED_IDUNN_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_IDUNN_WOOD_ITEM = registerItem(STRIPPED_IDUNN_WOOD);
    public static final DeferredItem<BlockItem> IDUNN_PLANKS_ITEM = registerItem(IDUNN_PLANKS);
    public static final DeferredItem<BlockItem> IDUNN_STAIRS_ITEM = registerItem(IDUNN_STAIRS);
    public static final DeferredItem<BlockItem> IDUNN_SLAB_ITEM = registerItem(IDUNN_SLAB);
    public static final DeferredItem<BlockItem> IDUNN_FENCE_ITEM = registerItem(IDUNN_FENCE);
    public static final DeferredItem<BlockItem> IDUNN_FENCE_GATE_ITEM = registerItem(IDUNN_FENCE_GATE);
    public static final DeferredItem<BlockItem> IDUNN_DOOR_ITEM = registerItem(IDUNN_DOOR);
    public static final DeferredItem<BlockItem> IDUNN_TRAPDOOR_ITEM = registerItem(IDUNN_TRAPDOOR);
    public static final DeferredItem<BlockItem> IDUNN_PRESSURE_PLATE_ITEM = registerItem(IDUNN_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> IDUNN_BUTTON_ITEM = registerItem(IDUNN_BUTTON);
    public static final DeferredItem<BlockItem> IDUNN_LEAVES_ITEM = registerItem(IDUNN_LEAVES);
    public static final DeferredItem<BlockItem> IDUNN_SAPLING_ITEM = registerItem(IDUNN_SAPLING);
    public static final DeferredItem<BlockItem> IDUNN_LEAF_LITTER_ITEM = registerItem(IDUNN_LEAF_LITTER);
    public static final DeferredItem<BlockItem> BOOK_PILE_ITEM = registerItem(BOOK_PILE);
    public static final DeferredItem<AlchemistCauldronBlockItem> ALCHEMIST_CAULDRON_ITEM =
            ModItems.ITEMS.registerItem("alchemist_cauldron",
                    properties -> new AlchemistCauldronBlockItem(
                             ALCHEMIST_CAULDRON.get(), properties.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> FENRIR_SEAL_ITEM = registerItem(FENRIR_SEAL);

    public static void addBlockEntityTypeBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SHELF, YGGDRASIL_SHELF.get());
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name, Block vanillaBlock) {
        return BLOCKS.registerSimpleBlock(name, () -> BlockBehaviour.Properties.ofFullCopy(vanillaBlock));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name,
            java.util.function.Function<BlockBehaviour.Properties, T> factory,
            Block vanillaBlock) {
        return BLOCKS.registerBlock(name, factory, () -> BlockBehaviour.Properties.ofFullCopy(vanillaBlock));
    }

    private static DeferredItem<BlockItem> registerItem(DeferredBlock<? extends Block> block) {
        return ModItems.ITEMS.registerSimpleBlockItem(block);
    }

    private ModBlocks() {
    }
}
