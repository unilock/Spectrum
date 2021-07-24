package de.dafuqs.pigment.blocks.chests;

import de.dafuqs.pigment.InventoryHelper;
import de.dafuqs.pigment.inventories.AutoCraftingInventory;
import de.dafuqs.pigment.inventories.RestockingChestScreenHandler;
import de.dafuqs.pigment.items.misc.CraftingTabletItem;
import de.dafuqs.pigment.registries.PigmentBlockEntityRegistry;
import de.dafuqs.pigment.registries.PigmentBlocks;
import de.dafuqs.pigment.registries.PigmentItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RestockingChestBlockEntity extends LootableContainerBlockEntity implements SidedInventory {

    private int coolDownTicks = 0;
    private DefaultedList<ItemStack> inventory;
    AutoCraftingInventory autoCraftingInventory = new AutoCraftingInventory(3, 3);


    public static final int INVENTORY_SIZE = 27+4+4; // 27 items, 4 crafting tablets, 4 result slots
    public static final int[] CHEST_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
    public static final int[] RECIPE_SLOTS = new int[]{ 27, 28, 29, 30};
    public static final int[] RESULT_SLOTS = new int[]{ 31, 32, 33, 34};

    public RestockingChestBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(PigmentBlockEntityRegistry.RESTOCKING_CHEST, blockPos, blockState);
        this.inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    protected Text getContainerName() {
        return new TranslatableText("block.pigment.restocking_chest");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new RestockingChestScreenHandler(syncId, playerInventory, this);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RestockingChestBlockEntity restockingChestBlockEntity) {
        if(tickCooldown(restockingChestBlockEntity)) {
            for(int i = 0; i < 4; i++) {
                ItemStack outputItemStack = restockingChestBlockEntity.inventory.get(RESULT_SLOTS[i]);
                ItemStack craftingTabletItemStack = restockingChestBlockEntity.inventory.get(RECIPE_SLOTS[i]);
                if(!craftingTabletItemStack.isEmpty() &&(outputItemStack.isEmpty() || outputItemStack.getCount() < outputItemStack.getMaxCount())) {
                    boolean couldCraft = restockingChestBlockEntity.tryCraft(restockingChestBlockEntity, i);
                    if(couldCraft) {
                        restockingChestBlockEntity.setCooldown(restockingChestBlockEntity, 20);
                        restockingChestBlockEntity.markDirty();
                        return;
                    }
                }
            }
        }
    }

    private void setCooldown(RestockingChestBlockEntity restockingChestBlockEntity, int cooldownTicks) {
        restockingChestBlockEntity.coolDownTicks = cooldownTicks;
    }

    private static boolean tickCooldown(RestockingChestBlockEntity restockingChestBlockEntity) {
        restockingChestBlockEntity.coolDownTicks--;
        if(restockingChestBlockEntity.coolDownTicks > 0) {
            return false;
        } else {
            restockingChestBlockEntity.coolDownTicks = 0;
        }
        return true;
    }

    private boolean tryCraft(RestockingChestBlockEntity restockingChestBlockEntity, int index) {
        ItemStack craftingTabletItemStack = restockingChestBlockEntity.inventory.get(RECIPE_SLOTS[index]);
        if(craftingTabletItemStack.isOf(PigmentItems.CRAFTING_TABLET)) {
            Recipe recipe = CraftingTabletItem.getStoredRecipe(world, craftingTabletItemStack);
            if(recipe instanceof ShapelessRecipe || recipe instanceof ShapedRecipe) {
                DefaultedList<Ingredient> ingredients = recipe.getIngredients();
                ItemStack outputItemStack = recipe.getOutput().copy();
                ItemStack currentItemStack = restockingChestBlockEntity.inventory.get(RESULT_SLOTS[index]);
                if (InventoryHelper.canCombineItemStacks(currentItemStack, outputItemStack) && InventoryHelper.removeFromInventory(ingredients, restockingChestBlockEntity, true)) {
                    InventoryHelper.removeFromInventory(ingredients, restockingChestBlockEntity, false);

                    if(currentItemStack.isEmpty()) {
                        restockingChestBlockEntity.inventory.set(RESULT_SLOTS[index], outputItemStack);
                    } else {
                        currentItemStack.setCount(currentItemStack.getCount() + outputItemStack.getCount());
                    }
                    return true;
                }

            }
        }
        return false;
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        if (!this.serializeLootTable(tag)) {
            Inventories.writeNbt(tag, this.inventory);
        }

        return tag;
    }

    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        if (!this.deserializeLootTable(tag)) {
            Inventories.readNbt(tag, this.inventory);
        }

    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if(side == Direction.DOWN) {
            return RESULT_SLOTS;
        } else {
            return CHEST_SLOTS;
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot <= CHEST_SLOTS[CHEST_SLOTS.length -1];
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }
}
