package de.dafuqs.pigment.inventories;

import de.dafuqs.pigment.blocks.altar.AltarBlockEntity;
import de.dafuqs.pigment.blocks.chests.RestockingChestBlock;
import de.dafuqs.pigment.blocks.chests.RestockingChestBlockEntity;
import de.dafuqs.pigment.inventories.slots.ExtractOnlySlot;
import de.dafuqs.pigment.inventories.slots.ReadOnlySlot;
import de.dafuqs.pigment.inventories.slots.StackFilterSlot;
import de.dafuqs.pigment.recipe.PigmentRecipeTypes;
import de.dafuqs.pigment.recipe.altar.AltarCraftingRecipe;
import de.dafuqs.pigment.registries.PigmentItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;

public class RestockingChestScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    protected final World world;

    public RestockingChestScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(PigmentScreenHandlerTypes.RESTOCKING_CHEST, syncId, playerInventory);
    }

    protected RestockingChestScreenHandler(ScreenHandlerType<?> type, int i, PlayerInventory playerInventory) {
        this(type, i, playerInventory, new SimpleInventory(RestockingChestBlockEntity.INVENTORY_SIZE));
    }

    public RestockingChestScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(PigmentScreenHandlerTypes.RESTOCKING_CHEST, syncId, playerInventory, inventory);
    }

    protected RestockingChestScreenHandler(ScreenHandlerType<?> type,  int i, PlayerInventory playerInventory, Inventory inventory) {
        super(type, i);
        this.inventory = inventory;
        this.world = playerInventory.player.world;

        checkSize(inventory, RestockingChestBlockEntity.INVENTORY_SIZE);

        // chest inventory
        int l;
        for(l = 0; l < 3; ++l) {
            for(int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inventory, l * 9 + k, 8 + k * 18, 67 + l * 18));
            }
        }

        // crafting tablet slots
        for(int j = 0; j < 4; j++) {
            int slotId = RestockingChestBlockEntity.RECIPE_SLOTS[j];
            this.addSlot(new StackFilterSlot(inventory, slotId, 26 + j * 36, 18, PigmentItems.CRAFTING_TABLET));
        }

        // crafting result slots
        for(int j = 0; j < 4; j++) {
            int slotId = RestockingChestBlockEntity.RESULT_SLOTS[j];
            this.addSlot(new ExtractOnlySlot(inventory, slotId, 26 + j * 36, 42));
        }

        // player inventory
        for(l = 0; l < 3; ++l) {
            for(int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 138 + l * 18));
            }
        }

        // player hotbar
        for(l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 196));
        }
    }

    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack clickedStackCopy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack clickedStack = slot.getStack();
            clickedStackCopy = clickedStack.copy();

            if(index < RestockingChestBlockEntity.INVENTORY_SIZE) {
                // => player inv
                if (!this.insertItem(clickedStack, 35, 71, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(index > RestockingChestBlockEntity.INVENTORY_SIZE && clickedStackCopy.isOf(PigmentItems.CRAFTING_TABLET)) {
                 if (!this.insertItem(clickedStack, RestockingChestBlockEntity.RECIPE_SLOTS[0], RestockingChestBlockEntity.RECIPE_SLOTS[RestockingChestBlockEntity.RECIPE_SLOTS.length-1]+1, false)) {
                     return ItemStack.EMPTY;
                 }
            }

            // chest => inventory
            if (!this.insertItem(clickedStack, 0, RestockingChestBlockEntity.CHEST_SLOTS.length-1, false)) {
                return ItemStack.EMPTY;
            }

            if (clickedStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (clickedStack.getCount() == clickedStackCopy.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, clickedStack);
        }


        return clickedStackCopy;
    }

}
