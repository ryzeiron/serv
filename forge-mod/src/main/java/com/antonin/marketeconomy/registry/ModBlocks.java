package com.antonin.marketeconomy.registry;

import com.antonin.marketeconomy.MarketEconomyMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MarketEconomyMod.MOD_ID);

    public static final RegistryObject<OrdinateurBlock> ORDINATEUR = BLOCKS.register("ordinateur",
            () -> new OrdinateurBlock(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("ordinateur"))
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // Vrai minerai du mod (remplace le calcite reskinne du plugin Paper) : genere dans les mines
    // du metier Mineur, donne un Lingot de Lithium via sa table de butin
    public static final RegistryObject<Block> LITHIUM_ORE = BLOCKS.register("lithium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("lithium_ore"))
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.DEEPSLATE)));

    private ModBlocks() {
    }
}
