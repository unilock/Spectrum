package de.dafuqs.spectrum.compat.lootr;

import de.dafuqs.spectrum.compat.*;
import de.dafuqs.spectrum.registries.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.*;

public class LootrCompat extends SpectrumIntegrationPacks.ModIntegrationPack {
	public static final Block SLATE_NOXWOOD_LOOT_AMPHORA = new LootAmphoraBlock(AbstractBlock.Settings.copy(SpectrumBlocks.SLATE_NOXWOOD_AMPHORA));
	public static final Block EBONY_NOXWOOD_LOOT_AMPHORA = new LootAmphoraBlock(AbstractBlock.Settings.copy(SpectrumBlocks.EBONY_NOXWOOD_AMPHORA));
	public static final Block IVORY_NOXWOOD_LOOT_AMPHORA = new LootAmphoraBlock(AbstractBlock.Settings.copy(SpectrumBlocks.IVORY_NOXWOOD_AMPHORA));
	public static final Block CHESTNUT_NOXWOOD_LOOT_AMPHORA = new LootAmphoraBlock(AbstractBlock.Settings.copy(SpectrumBlocks.CHESTNUT_NOXWOOD_AMPHORA));
	public static final Block WEEPING_GALA_LOOT_AMPHORA = new LootAmphoraBlock(AbstractBlock.Settings.copy(SpectrumBlocks.WEEPING_GALA_AMPHORA));
	
	public static BlockEntityType<LootAmphoraBlockEntity> LOOT_AMPHORA;
	
	@Override
	public void register() {
		SpectrumBlocks.registerBlockWithItem("slate_noxwood_loot_amphora", SLATE_NOXWOOD_LOOT_AMPHORA, SpectrumItems.IS.of(), DyeColor.LIME);
		SpectrumBlocks.registerBlockWithItem("ebony_noxwood_loot_amphora", EBONY_NOXWOOD_LOOT_AMPHORA, SpectrumItems.IS.of(), DyeColor.LIME);
		SpectrumBlocks.registerBlockWithItem("ivory_noxwood_loot_amphora", IVORY_NOXWOOD_LOOT_AMPHORA, SpectrumItems.IS.of(), DyeColor.LIME);
		SpectrumBlocks.registerBlockWithItem("chestnut_noxwood_loot_amphora", CHESTNUT_NOXWOOD_LOOT_AMPHORA, SpectrumItems.IS.of(), DyeColor.LIME);
		SpectrumBlocks.registerBlockWithItem("weeping_gala_loot_amphora", WEEPING_GALA_LOOT_AMPHORA, SpectrumItems.IS.of(), DyeColor.LIME);
		
		LOOT_AMPHORA = SpectrumBlockEntities.register("loot_amphora", LootAmphoraBlockEntity::new, SLATE_NOXWOOD_LOOT_AMPHORA, EBONY_NOXWOOD_LOOT_AMPHORA, IVORY_NOXWOOD_LOOT_AMPHORA, CHESTNUT_NOXWOOD_LOOT_AMPHORA, WEEPING_GALA_LOOT_AMPHORA);
	}
	
	@Override
	public void registerClient() {
	
	}
}
