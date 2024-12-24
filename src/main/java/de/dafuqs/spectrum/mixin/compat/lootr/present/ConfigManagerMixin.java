package de.dafuqs.spectrum.mixin.compat.lootr.present;

import de.dafuqs.spectrum.*;
import de.dafuqs.spectrum.compat.*;
import de.dafuqs.spectrum.compat.lootr.*;
import de.dafuqs.spectrum.registries.*;
import net.minecraft.block.*;
import net.zestyblaze.lootr.config.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import java.util.*;

@Mixin(value = ConfigManager.class, remap = false)
public class ConfigManagerMixin {
	@Shadow
	private static Map<Block, Block> replacements;
	
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void addReplacements(CallbackInfo ci) {
		if (SpectrumCommon.CONFIG.IntegrationPacksToSkipLoading.contains(SpectrumIntegrationPacks.LOOTR_ID)) return;
		
		if (replacements == null) {
			replacements = new HashMap<>();
		}
		
		replacements.put(SpectrumBlocks.SLATE_NOXWOOD_AMPHORA, LootrCompat.SLATE_NOXWOOD_LOOT_AMPHORA);
		replacements.put(SpectrumBlocks.EBONY_NOXWOOD_AMPHORA, LootrCompat.EBONY_NOXWOOD_LOOT_AMPHORA);
		replacements.put(SpectrumBlocks.IVORY_NOXWOOD_AMPHORA, LootrCompat.IVORY_NOXWOOD_LOOT_AMPHORA);
		replacements.put(SpectrumBlocks.CHESTNUT_NOXWOOD_AMPHORA, LootrCompat.CHESTNUT_NOXWOOD_LOOT_AMPHORA);
		replacements.put(SpectrumBlocks.WEEPING_GALA_AMPHORA, LootrCompat.WEEPING_GALA_LOOT_AMPHORA);
	}
}
