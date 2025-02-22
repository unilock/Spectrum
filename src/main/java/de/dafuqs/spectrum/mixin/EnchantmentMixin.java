package de.dafuqs.spectrum.mixin;

import de.dafuqs.spectrum.api.item.*;
import net.minecraft.enchantment.*;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	
	@Shadow
	@Final
	public EnchantmentTarget target;

	@Inject(method = "isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
	public void isAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		var accepted = cir.getReturnValue();
		
		if (stack.getItem() instanceof TranstargetItem spoofer) {
			accepted = this.target == spoofer.getRealTarget();
		}
		
		if (!accepted) {
			if ((stack.getItem() instanceof ExtendedEnchantable extendedEnchantable && extendedEnchantable.acceptsEnchantment((Enchantment) (Object) this))) {
				cir.setReturnValue(true);
			}
		}
	}
	
}
