package de.dafuqs.spectrum.compat.lootr;

import de.dafuqs.spectrum.blocks.amphora.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.zestyblaze.lootr.api.*;
import net.zestyblaze.lootr.util.*;
import org.jetbrains.annotations.*;

public class LootAmphoraBlock extends AmphoraBlock {
	public LootAmphoraBlock(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (player.isSneaking()) {
			ChestUtil.handleLootSneak(this, world, pos, player);
		} else {
			ChestUtil.handleLootChest(this, world, pos, player);
		}
		return ActionResult.SUCCESS;
	}
	
	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new LootAmphoraBlockEntity(pos, state);
	}
	
	@Override
	public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
		return LootrAPI.getDestroyProgress(state, player, world, pos, super.calcBlockBreakingDelta(state, player, world, pos));
	}
	
	@Override
	public float getBlastResistance() {
		return LootrAPI.getExplosionResistance(this, super.getBlastResistance());
	}
	
	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return LootrAPI.getAnalogOutputSignal(state, world, pos, super.getComparatorOutput(state, world, pos));
	}
}
