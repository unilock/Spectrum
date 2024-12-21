package de.dafuqs.spectrum.mixin.compat.lootr.present;

import de.dafuqs.spectrum.compat.lootr.*;
import de.dafuqs.spectrum.registries.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.*;
import net.zestyblaze.lootr.config.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

import java.util.*;

@Mixin(value = ConfigManager.class, remap = false)
public class ConfigManagerMixin {
	@Shadow
	private static Map<Block, Block> replacements;
	
	@ModifyVariable(method = "replacement", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), name = "replacement")
	private static Block modifyReplacement(Block replacement, BlockState original) {
		if (replacement == null && original.isIn(SpectrumBlockTags.AMPHORAS)) {
			Block block = original.getBlock();
			
			if (block instanceof BlockEntityProvider provider && provider.createBlockEntity(BlockPos.ORIGIN, original) instanceof LootableContainerBlockEntity) {
				if (original.isOf(SpectrumBlocks.SLATE_NOXWOOD_AMPHORA)) {
					replacements.put(block, LootrCompat.SLATE_NOXWOOD_LOOT_AMPHORA);
				}
				if (original.isOf(SpectrumBlocks.EBONY_NOXWOOD_AMPHORA)) {
					replacements.put(block, LootrCompat.EBONY_NOXWOOD_LOOT_AMPHORA);
				}
				if (original.isOf(SpectrumBlocks.IVORY_NOXWOOD_AMPHORA)) {
					replacements.put(block, LootrCompat.IVORY_NOXWOOD_LOOT_AMPHORA);
				}
				if (original.isOf(SpectrumBlocks.CHESTNUT_NOXWOOD_AMPHORA)) {
					replacements.put(block, LootrCompat.CHESTNUT_NOXWOOD_LOOT_AMPHORA);
				}
				if (original.isOf(SpectrumBlocks.WEEPING_GALA_AMPHORA)) {
					replacements.put(block, LootrCompat.WEEPING_GALA_LOOT_AMPHORA);
				}
			}
			
			return replacements.get(block);
		}
		
		return replacement;
	}
}
