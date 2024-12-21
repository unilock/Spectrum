package de.dafuqs.spectrum.compat.lootr;

import com.google.common.collect.*;
import de.dafuqs.spectrum.blocks.amphora.*;
import net.minecraft.advancement.criterion.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.loot.*;
import net.minecraft.loot.context.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.server.network.*;
import net.minecraft.server.world.*;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.collection.*;
import net.minecraft.util.math.*;
import net.zestyblaze.lootr.api.*;
import net.zestyblaze.lootr.api.blockentity.*;
import net.zestyblaze.lootr.client.*;
import net.zestyblaze.lootr.config.*;
import net.zestyblaze.lootr.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class LootAmphoraBlockEntity extends AmphoraBlockEntity implements ILootBlockEntity {
	public Set<UUID> openers = new HashSet<>();
	protected Identifier savedLootTable = null;
	protected long seed = -1L;
	protected UUID tileId = null;
	protected boolean opened = false;
	private boolean savingToItem = false;
	
	public LootAmphoraBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state);
	}
	
	// We cannot set the BlockEntityType in the constructor, so hopefully doing it here is enough
	// (the field in BlockEntity is private anyway)
	@Override
	public BlockEntityType<?> getType() {
		return LootrCompat.LOOT_AMPHORA;
	}
	
	@Override
	protected boolean isPlayerViewing(PlayerEntity player) {
		if (player.currentScreenHandler instanceof GenericContainerScreenHandler) {
			Inventory inventory = ((GenericContainerScreenHandler) player.currentScreenHandler).getInventory();
			if (inventory instanceof SpecialChestInventory data) {
				if (data.getTileId() == null) {
					return data.getBlockEntity(this.world) == this;
				}
				
				return data.getTileId().equals(this.getTileId());
			}
		}
		
		return false;
	}
	
	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		
		if (this.savedLootTable != null) {
			nbt.putString("LootTable", this.savedLootTable.toString());
		}
		
		if (this.seed != -1L) {
			nbt.putLong("LootTableSeed", this.seed);
		}
		
		if (!LootrAPI.shouldDiscard() && !this.savingToItem) {
			nbt.putUuid("tileId", this.getTileId());
			NbtList list = new NbtList();
			
			for (UUID opener : this.openers) {
				list.add(NbtHelper.fromUuid(opener));
			}
			
			nbt.put("LootrOpeners", list);
		}
	}
	
	@Override
	public void readNbt(NbtCompound nbt) {
		if (nbt.contains("specialLootChest_table", NbtElement.STRING_TYPE)) {
			this.savedLootTable = new Identifier(nbt.getString("specialLootChest_table"));
		}
		
		if (nbt.contains("specialLootChest_seed", NbtElement.LONG_TYPE)) {
			this.seed = nbt.getLong("specialLootChest_seed");
		}
		
		if (this.savedLootTable == null && nbt.contains("LootTable", NbtElement.STRING_TYPE)) {
			this.savedLootTable = new Identifier(nbt.getString("LootTable"));
			if (nbt.contains("LootTableSeed", NbtElement.LONG_TYPE)) {
				this.seed = nbt.getLong("LootTableSeed");
			}
			
			this.setLootTable(this.savedLootTable, this.seed);
		}
		
		if (nbt.containsUuid("tileId")) {
			this.tileId = nbt.getUuid("tileId");
		}
		
		if (this.tileId == null) {
			this.getTileId();
		}
		
		if (nbt.contains("LootrOpeners")) {
			Set<UUID> newOpeners = new HashSet<>();
			
			for (NbtElement item : nbt.getList("LootrOpeners", NbtElement.INT_ARRAY_TYPE)) {
				newOpeners.add(NbtHelper.toUuid(item));
			}
			
			if (!Sets.symmetricDifference(this.openers, newOpeners).isEmpty()) {
				this.openers = newOpeners;
				if (this.getWorld() != null && this.getWorld().isClient()) {
					ClientHooks.clearCache(this.getPos());
				}
			}
		}
		
		super.readNbt(nbt);
	}
	
	@Override
	protected void setInvStackList(DefaultedList<ItemStack> list) {
	}
	
	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return null;
	}
	
	@Override
	public void setStackNbt(ItemStack stack) {
		this.savingToItem = true;
		super.setStackNbt(stack);
		this.savingToItem = false;
	}
	
	@Override
	public void checkLootInteraction(@Nullable PlayerEntity player) {
	}
	
	@Override
	public void setLootTable(Identifier id, long seed) {
		this.savedLootTable = id;
		this.seed = seed;
		super.setLootTable(id, seed);
	}
	
	@Override
	public NbtCompound toInitialChunkDataNbt() {
		NbtCompound nbt = super.toInitialChunkDataNbt();
		this.writeNbt(nbt);
		return nbt;
	}
	
	@Override
	public Set<UUID> getOpeners() {
		return this.openers;
	}
	
	@Override
	public BlockPos getPosition() {
		return this.getPos();
	}
	
	@Override
	public long getSeed() {
		return this.seed;
	}
	
	@Override
	public Identifier getTable() {
		return this.savedLootTable;
	}
	
	@Override
	public UUID getTileId() {
		if (this.tileId == null) {
			this.tileId = UUID.randomUUID();
		}
		
		return this.tileId;
	}
	
	@Override
	public void setOpened(boolean opened) {
		this.opened = opened;
	}
	
	@Override
	public void unpackLootTable(PlayerEntity playerEntity, Inventory inventory, Identifier identifier, long seed) {
		if (this.world != null && this.savedLootTable != null && this.world.getServer() != null) {
			LootTable loottable = this.world.getServer().getLootManager().getLootTable(identifier != null ? identifier : this.savedLootTable);
			if (loottable == LootTable.EMPTY) {
				LootrAPI.LOG.error("Unable to fill loot amphora in " + this.world.getRegistryKey() + " at " + this.pos + " as the loot table '" + (identifier != null ? identifier : this.savedLootTable) + "' couldn't be resolved! Please search the loot table in `latest.log` to see if there are errors in loading.");
				if (ConfigManager.get().debug.report_invalid_tables) {
					playerEntity.sendMessage(Text.translatable("lootr.message.invalid_table", (identifier != null ? identifier : this.savedLootTable).toString()).setStyle(ConfigManager.get().notifications.disable_message_styles ? Style.EMPTY : Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_RED)).withBold(true)), false);
				}
			}
			
			if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity) {
				Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger(serverPlayerEntity, identifier != null ? identifier : this.lootTableId);
			}
			
			LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) this.world)).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(this.pos));
			if (playerEntity != null) {
				builder.luck(playerEntity.getLuck()).add(LootContextParameters.THIS_ENTITY, playerEntity);
			}
			
			loottable.supplyInventory(inventory, builder.build(LootContextTypes.CHEST), LootrAPI.getLootSeed(seed == Long.MIN_VALUE ? this.seed : seed));
		}
	}
	
	@Override
	public void updatePacketViaState() {
		if (this.world != null && !this.world.isClient) {
			BlockState state = this.world.getBlockState(this.getPos());
			this.world.updateListeners(this.getPos(), state, state, 8);
		}
	}

//	@Override
//	public @Nullable Object getRenderAttachmentData() {
//		PlayerEntity player = ClientHooks.getPlayer();
//		return player == null ? null : this.getOpeners().contains(player.getUuid());
//	}
}
