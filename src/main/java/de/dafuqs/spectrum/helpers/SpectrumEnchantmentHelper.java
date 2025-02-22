package de.dafuqs.spectrum.helpers;

import de.dafuqs.spectrum.api.item.*;
import de.dafuqs.spectrum.enchantments.*;
import de.dafuqs.spectrum.registries.*;
import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class SpectrumEnchantmentHelper {
	
	/**
	 * Adds an enchantment to an ItemStack. If the stack already has that enchantment, it gets upgraded instead
	 *
	 * @param stack                     the stack that receives the enchantments
	 * @param enchantment               the enchantment to add
	 * @param level                     the level of the enchantment
	 * @param forceEvenIfNotApplicable  add enchantments to the item, even if the item does usually not support that enchantment
	 * @param allowEnchantmentConflicts add enchantments to the item, even if there are enchantment conflicts
	 * @return the enchanted stack and a boolean if the enchanting was successful
	 */
	public static Pair<Boolean, ItemStack> addOrUpgradeEnchantment(ItemStack stack, Enchantment enchantment, int level, boolean forceEvenIfNotApplicable, boolean allowEnchantmentConflicts) {
		// can this enchant even go on that tool?
		if (!enchantment.isAcceptableItem(stack)
				&& !stack.isOf(Items.ENCHANTED_BOOK)
				&& !SpectrumEnchantmentHelper.isEnchantableBook(stack)) {
			
			return new Pair<>(false, stack);
		}
		
		// if not forced check if the stack already has enchantments
		// that conflict with the new one
		if (!allowEnchantmentConflicts && hasEnchantmentThatConflictsWith(stack, enchantment)) {
			return new Pair<>(false, stack);
		}

		// If it's in the tag, there's nothing more to check here. Enchant away!
		if (!stack.isOf(Items.ENCHANTED_BOOK)) {
			if (isEnchantableBook(stack)) {
				ItemStack enchantedBookStack = new ItemStack(Items.ENCHANTED_BOOK, stack.getCount());
				enchantedBookStack.setNbt(stack.getNbt());
				stack = enchantedBookStack;
			} else if (!forceEvenIfNotApplicable && !enchantment.isAcceptableItem(stack)) {
				if (stack.getItem() instanceof ExtendedEnchantable extendedEnchantable) {
					// ExtendedEnchantable explicitly states this enchantment is acceptable
					if (!extendedEnchantable.acceptsEnchantment(enchantment)) {
						return new Pair<>(false, stack);
					}
				}
			}
		}
		
		NbtCompound nbtCompound = stack.getOrCreateNbt();
		String nbtString;
		if (stack.isOf(Items.ENCHANTED_BOOK) || stack.isOf(SpectrumItems.ENCHANTMENT_CANVAS)) {
			nbtString = EnchantedBookItem.STORED_ENCHANTMENTS_KEY;
		} else {
			nbtString = ItemStack.ENCHANTMENTS_KEY;
		}
		if (!nbtCompound.contains(nbtString, NbtElement.LIST_TYPE)) {
			nbtCompound.put(nbtString, new NbtList());
		}
		
		Identifier enchantmentIdentifier = Registries.ENCHANTMENT.getId(enchantment);
		NbtList nbtList = nbtCompound.getList(nbtString, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < nbtList.size(); i++) {
			NbtCompound enchantmentCompound = nbtList.getCompound(i);
			if (enchantmentCompound.contains("id", NbtElement.STRING_TYPE) && Identifier.tryParse(enchantmentCompound.getString("id")).equals(enchantmentIdentifier)) {
				boolean isEqualOrDowngrade = enchantmentCompound.contains("lvl", NbtElement.SHORT_TYPE) && enchantmentCompound.getInt("lvl") >= level;
				if (isEqualOrDowngrade) {
					return new Pair<>(false, stack);
				}

				nbtList.remove(i);
				i--;
			}
		}
		
		nbtList.add(EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(enchantment), (byte) level));
		nbtCompound.put(nbtString, nbtList);
		stack.setNbt(nbtCompound);
		
		return new Pair<>(true, stack);
	}
	
	public static void setStoredEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
		stack.removeSubNbt(EnchantedBookItem.STORED_ENCHANTMENTS_KEY); // clear existing enchantments
		for (Map.Entry<Enchantment, Integer> enchantmentIntegerEntry : enchantments.entrySet()) {
			Enchantment enchantment = enchantmentIntegerEntry.getKey();
			if (enchantment != null) {
				EnchantedBookItem.addEnchantment(stack, new EnchantmentLevelEntry(enchantment, enchantmentIntegerEntry.getValue()));
			}
		}
	}
	
	/**
	 * Clears all enchantments of receiverStack and replaces them with the ones present in sourceStacks
	 * The enchantments are applied in order, so if there are conflicts, the first enchantment in sourceStacks gets chosen
	 *
	 * @param receiverStack             the stack that receives the enchantments
	 * @param forceEvenIfNotApplicable  add enchantments to the item, even if the item does usually not support that enchantment
	 * @param allowEnchantmentConflicts add enchantments to the item, even if there are enchantment conflicts
	 * @param sourceStacks   sourceStacks the stacks that supply the enchantments
	 * @return the resulting stack
	 */
	public static ItemStack clearAndCombineEnchantments(ItemStack receiverStack, boolean forceEvenIfNotApplicable, boolean allowEnchantmentConflicts, ItemStack... sourceStacks) {
		EnchantmentHelper.set(Map.of(), receiverStack); // clear current ones
		for (ItemStack stack : sourceStacks) {
			for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.get(stack).entrySet()) {
				receiverStack = SpectrumEnchantmentHelper.addOrUpgradeEnchantment(receiverStack, entry.getKey(), entry.getValue(), forceEvenIfNotApplicable, allowEnchantmentConflicts).getRight();
			}
		}
		return receiverStack;
	}
	
	/**
	 * Checks if an itemstack can be used as the source to create an enchanted book
	 *
	 * @param stack The itemstack to check
	 * @return true if it is a book that can be turned into an enchanted book by enchanting
	 */
	public static boolean isEnchantableBook(@NotNull ItemStack stack) {
		return stack.isIn(SpectrumItemTags.ENCHANTABLE_BOOKS) || stack.getItem() instanceof BookItem;
	}
	
	public static boolean hasEnchantmentThatConflictsWith(ItemStack itemStack, Enchantment enchantment) {
		Map<Enchantment, Integer> existingEnchantments = EnchantmentHelper.get(itemStack);
		for (Enchantment existingEnchantment : existingEnchantments.keySet()) {
			if (!existingEnchantment.equals(enchantment)) {
				if (!existingEnchantment.canCombine(enchantment)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static Map<Enchantment, Integer> collectHighestEnchantments(List<ItemStack> itemStacks) {
		Map<Enchantment, Integer> enchantmentLevelMap = new LinkedHashMap<>();
		
		for (ItemStack itemStack : itemStacks) {
			Map<Enchantment, Integer> itemStackEnchantments = EnchantmentHelper.get(itemStack);
			for (Enchantment enchantment : itemStackEnchantments.keySet()) {
				int level = itemStackEnchantments.get(enchantment);
				if (enchantmentLevelMap.containsKey(enchantment)) {
					int storedLevel = enchantmentLevelMap.get(enchantment);
					if (level > storedLevel) {
						enchantmentLevelMap.put(enchantment, level);
					}
				} else {
					enchantmentLevelMap.put(enchantment, level);
				}
			}
		}
		
		return enchantmentLevelMap;
	}
	
	public static boolean canCombineAny(Map<Enchantment, Integer> existingEnchantments, Map<Enchantment, Integer> newEnchantments) {
		if (existingEnchantments.isEmpty()) {
			return true;
		} else {
			for (Enchantment existingEnchantment : existingEnchantments.keySet()) {
				for (Enchantment newEnchantment : newEnchantments.keySet()) {
					boolean canCurrentCombine = existingEnchantment.canCombine(newEnchantment);
					if (canCurrentCombine) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Removes the enchantments on a stack of items / enchanted book
	 * @param itemStack    the stack
	 * @param enchantments the enchantments to remove
	 * @return The resulting stack & the count of enchants that were removed
	 */
	public static Pair<ItemStack, Integer> removeEnchantments(@NotNull ItemStack itemStack, Enchantment... enchantments) {
		NbtCompound compound = itemStack.getNbt();
		if (compound == null) {
			return new Pair<>(itemStack, 0);
		}

		NbtList enchantmentList;
		if (itemStack.isOf(Items.ENCHANTED_BOOK)) {
			enchantmentList = compound.getList(EnchantedBookItem.STORED_ENCHANTMENTS_KEY, 10);
		} else {
			enchantmentList = compound.getList(ItemStack.ENCHANTMENTS_KEY, 10);
		}

		List<Identifier> enchantIDs = new ArrayList<>();
		for(Enchantment enchantment : enchantments) {
			enchantIDs.add(Registries.ENCHANTMENT.getId(enchantment));
		}

		int removals = 0;
		for (int i = 0; i < enchantmentList.size(); i++) {
			NbtCompound currentCompound = enchantmentList.getCompound(i);
			if (currentCompound.contains("id", NbtElement.STRING_TYPE)) {
				Identifier currentID = new Identifier(currentCompound.getString("id"));
				if(enchantIDs.contains(currentID)) {
					enchantmentList.remove(i);
					removals++;
					break;
				}
			}
		}
		
		if (itemStack.isOf(Items.ENCHANTED_BOOK)) {
			if(enchantmentList.isEmpty()) {
				ItemStack newStack = new ItemStack(Items.BOOK);
				newStack.setCount(itemStack.getCount());
				return new Pair<>(newStack, removals);
			}
			compound.put(EnchantedBookItem.STORED_ENCHANTMENTS_KEY, enchantmentList);
		} else {
			compound.put(ItemStack.ENCHANTMENTS_KEY, enchantmentList);
		}
		itemStack.setNbt(compound);

		return new Pair<>(itemStack, removals);
	}
	
	public static <T extends Item & ExtendedEnchantable> ItemStack getMaxEnchantedStack(@NotNull T item) {
		ItemStack itemStack = item.getDefaultStack();
		for (Enchantment enchantment : Registries.ENCHANTMENT.stream().toList()) {
			if (item.acceptsEnchantment(enchantment)) {
				int maxLevel = enchantment.getMaxLevel();
				itemStack = addOrUpgradeEnchantment(itemStack, enchantment, maxLevel, true, true).getRight();
			}
		}
		return itemStack;
	}
	
	public static int getUsableLevel(SpectrumEnchantment enchantment, ItemStack itemStack, Entity entity) {
		int level = EnchantmentHelper.getLevel(enchantment, itemStack);
		if (level > 0 && !enchantment.canEntityUse(entity)) {
			level = 0;
		}
		return level;
	}

}
