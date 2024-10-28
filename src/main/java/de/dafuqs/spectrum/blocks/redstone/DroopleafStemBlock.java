package de.dafuqs.spectrum.blocks.redstone;

import de.dafuqs.spectrum.blocks.FluidLogging;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import java.util.Optional;
import static de.dafuqs.spectrum.blocks.FluidLogging.State.getForFluidState;

@SuppressWarnings("deprecation")
public class DroopleafStemBlock  extends HorizontalFacingBlock implements Fertilizable, FluidLogging.SpectrumFluidLoggable {
    public static final EnumProperty<FluidLogging.State> LOGGED = FluidLogging.ANY_INCLUDING_NONE;
    protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(5.0, 0.0, 9.0, 11.0, 16.0, 15.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(5.0, 0.0, 1.0, 11.0, 16.0, 7.0);
    protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(1.0, 0.0, 5.0, 7.0, 16.0, 11.0);
    protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(9.0, 0.0, 5.0, 15.0, 16.0, 11.0);
    public DroopleafStemBlock(Settings settings) {
        super(settings);
        this.setDefaultState((this.stateManager.getDefaultState().with(LOGGED, FluidLogging.State.NOT_LOGGED).with(FACING, Direction.NORTH)));
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOGGED);
    }
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACING)) {
            case SOUTH:
                return SOUTH_SHAPE;
            case NORTH:
            default:
                return NORTH_SHAPE;
            case WEST:
                return WEST_SHAPE;
            case EAST:
                return EAST_SHAPE;
        }
    }
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        Optional<BlockPos> optional = BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.UP, SpectrumBlocks.DROOPLEAF);
        if (optional.isEmpty()) {
            return false;
        } else {
            BlockPos blockPos = optional.get().up();
            BlockState blockState = world.getBlockState(blockPos);
            return DroopleafBlock.canGrowInto(world, blockPos, blockState);
        }
    }
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return (blockState.isOf(this) || blockState.isIn(BlockTags.BIG_DRIPLEAF_PLACEABLE) || blockState.isOf(SpectrumBlocks.DROOPLEAF)) && (BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.DOWN, SpectrumBlocks.DROOPLEAF).isPresent() || BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.UP, SpectrumBlocks.DROOPLEAF).isPresent());
    }
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if ((direction == Direction.DOWN || direction == Direction.UP) && !state.canPlaceAt(world, pos)) {
            world.scheduleBlockTick(pos, this, 1);
        }
        else if (BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.DOWN, SpectrumBlocks.DROOPLEAF).isEmpty())
        {
            world.scheduleBlockTick(pos, this, 1);
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
        }

    }
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        Optional<BlockPos> optional = BlockLocating.findColumnEnd(world, pos, state.getBlock(), Direction.UP, SpectrumBlocks.DROOPLEAF);
        if (optional.isPresent()) {
            BlockPos blockPos = optional.get();
            BlockPos blockPos2 = blockPos.up();
            Direction direction = state.get(FACING);
            placeStemAt(world, blockPos, getForFluidState(world.getFluidState(blockPos)), direction);
            DroopleafBlock.placeDroopleafAt(world, blockPos2, getForFluidState(world.getFluidState(blockPos2)), direction);
        }
    }
    protected static boolean placeStemAt(WorldAccess world, BlockPos pos, FluidLogging.State fluidState, Direction direction) {
        BlockState blockState = SpectrumBlocks.DROOPLEAF_STEM.getDefaultState().with(FluidLogging.ANY_INCLUDING_NONE, fluidState).with(FACING, direction);
        return world.setBlockState(pos, blockState, 3);
    }
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(SpectrumBlocks.DROOPLEAF);
    }
}
