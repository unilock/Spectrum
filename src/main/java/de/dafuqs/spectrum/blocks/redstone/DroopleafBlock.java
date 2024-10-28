package de.dafuqs.spectrum.blocks.redstone;

import com.google.common.collect.ImmutableMap;
import de.dafuqs.spectrum.blocks.FluidLogging;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static de.dafuqs.spectrum.blocks.FluidLogging.State.getForFluidState;

@SuppressWarnings("deprecation")
public class DroopleafBlock extends HorizontalFacingBlock implements Fertilizable, FluidLogging.SpectrumFluidLoggable {
    public static final EnumProperty<FluidLogging.State> LOGGED = FluidLogging.ANY_INCLUDING_NONE;
    private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);
    private static final Map<Direction, VoxelShape> SHAPES_FOR_DIRECTION = ImmutableMap.of(Direction.NORTH, VoxelShapes.combine(DroopleafStemBlock.NORTH_SHAPE, BASE_SHAPE, BooleanBiFunction.ONLY_FIRST), Direction.SOUTH, VoxelShapes.combine(DroopleafStemBlock.SOUTH_SHAPE, BASE_SHAPE, BooleanBiFunction.ONLY_FIRST), Direction.EAST, VoxelShapes.combine(DroopleafStemBlock.EAST_SHAPE, BASE_SHAPE, BooleanBiFunction.ONLY_FIRST), Direction.WEST, VoxelShapes.combine(DroopleafStemBlock.WEST_SHAPE, BASE_SHAPE, BooleanBiFunction.ONLY_FIRST));
    private final Map<BlockState, VoxelShape> shapes;


    public DroopleafBlock(Settings settings) {
        super(settings);
        this.setDefaultState((this.stateManager.getDefaultState().with(LOGGED, FluidLogging.State.NOT_LOGGED).with(FACING, Direction.NORTH).with(Properties.INVERTED, false)));
        this.shapes = this.getShapesForStates(DroopleafBlock::getShapeForState);
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOGGED, Properties.INVERTED);
    }
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        if(blockState.isOf(this) || blockState.isOf(SpectrumBlocks.DROOPLEAF_STEM))
        {
            if(blockState.isOf(this))
            {
                ctx.getWorld().setBlockState(ctx.getBlockPos().down(), blockState.with(Properties.INVERTED, true), Block.NOTIFY_LISTENERS);
                ctx.getWorld().scheduleBlockTick(ctx.getBlockPos().down(), blockState.getBlock(), 100);
            }
            return SpectrumBlocks.DROOPLEAF_STEM.getStateWithProperties(blockState);
        }
        else
        {
            return this.getDefaultState().with(FluidLogging.ANY_INCLUDING_NONE, getForFluidState(fluidState)).with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(Properties.INVERTED, false);
        }
    }
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.isOf(this) || blockState.isOf(SpectrumBlocks.DROOPLEAF_STEM) || blockState.isIn(BlockTags.BIG_DRIPLEAF_PLACEABLE);
    }
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return context.getStack().isOf(this.asItem());
    }
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(FACING) == direction.getOpposite() || direction == Direction.UP ? 15 : 0;
    }

    private static VoxelShape getShapeForState(BlockState state) {
        return VoxelShapes.union(Block.createCuboidShape(0.0, 11.0, 0.0, 16.0, 15.0, 16.0),SHAPES_FOR_DIRECTION.get(state.get(FACING)));
    }
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        BlockState blockState = world.getBlockState(pos.up());
        return canGrowInto(blockState);
    }
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        BlockPos blockPos = pos.up();
        BlockState blockState = world.getBlockState(blockPos);
        if (canGrowInto(world, blockPos, blockState)) {
            Direction direction = state.get(FACING);
            DroopleafStemBlock.placeStemAt(world, pos, getForFluidState(state.getFluidState()), direction);
            placeDroopleafAt(world, blockPos, getForFluidState(blockState.getFluidState()), direction);
        }

    }
    public static void grow(WorldAccess world, Random random, BlockPos pos, Direction direction) {
        int i = MathHelper.nextInt(random, 2, 5);
        BlockPos.Mutable mutable = pos.mutableCopy();
        int j = 0;

        while(j < i && canGrowInto(world, mutable, world.getBlockState(mutable))) {
            ++j;
            mutable.move(Direction.UP);
        }

        int k = pos.getY() + j - 1;
        mutable.setY(pos.getY());

        while(mutable.getY() < k) {
            DroopleafStemBlock.placeStemAt(world, mutable, getForFluidState(world.getFluidState(mutable)), direction);
            mutable.move(Direction.UP);
        }

        placeDroopleafAt(world, mutable, getForFluidState(world.getFluidState(mutable)), direction);
    }

    private static boolean canGrowInto(BlockState state) {
        return state.isOf(SpectrumBlocks.DROOPLEAF_STEM);
    }
    protected static boolean canGrowInto(HeightLimitView world, BlockPos pos, BlockState state) {
        return !world.isOutOfHeightLimit(pos) && canGrowInto(state);
    }
    protected static boolean placeDroopleafAt(WorldAccess world, BlockPos pos, FluidLogging.State fluidState, Direction direction) {
        BlockState blockState = SpectrumBlocks.DROOPLEAF.getDefaultState().with(FluidLogging.ANY_INCLUDING_NONE, fluidState).with(FACING, direction).with(Properties.INVERTED, false);
        return world.setBlockState(pos, blockState, 3);
    }
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        } else {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
    }
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(LOGGED).getFluidState();
    }
    private static boolean isEntityAbove(BlockPos pos, Entity entity) {
        return entity.isOnGround() && entity.getPos().y > (double)((float)pos.getY() + 0.6875F);
    }
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient) {
            if (isEntityAbove(pos, entity)) {
                preDropLeaf(state, world, pos);
            }

        }
    }
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        BlockPos pos = hit.getBlockPos();
        world.setBlockState(pos, state.with(Properties.INVERTED, false), 2);
        if(world.getBlockState(pos.down()).getBlock() == SpectrumBlocks.DROOPLEAF_STEM)
        {
            dropLeaf(state, world, pos, SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
        }
    }
    private void preDropLeaf(BlockState state, World world, BlockPos pos) {
        world.setBlockState(pos, state.with(Properties.INVERTED, false), 2);
        //TODO: Increase delay if planted in Mud?
        world.scheduleBlockTick(pos, this, 20);
    }
    private static void dropLeaf(BlockState state, World world, BlockPos pos, @Nullable SoundEvent sound) {
        if (sound != null) {
            playDropSound(world, pos, sound);
        }
        world.setBlockState(pos, SpectrumBlocks.DROOPLEAF_STEM.getStateWithProperties(state), Block.NOTIFY_LISTENERS);
        world.setBlockState(pos.down(), state.with(Properties.INVERTED, true), Block.NOTIFY_LISTENERS);
        world.updateNeighbors(pos, SpectrumBlocks.DROOPLEAF_STEM);
        world.updateNeighbors(pos.down(), SpectrumBlocks.DROOPLEAF);
        world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos.down());
        world.scheduleBlockTick(pos.down(), state.getBlock(), 100);
    }
    private static void riseLeaf(BlockState state, World world, BlockPos pos, @Nullable SoundEvent sound)
    {
        if (sound != null) {
            playDropSound(world, pos, sound);
        }
        world.setBlockState(pos, SpectrumBlocks.DROOPLEAF_STEM.getStateWithProperties(state), Block.NOTIFY_LISTENERS);
        world.setBlockState(pos.up(), state, Block.NOTIFY_LISTENERS);
        world.updateNeighbors(pos, SpectrumBlocks.DROOPLEAF_STEM);
        world.updateNeighbors(pos.up(), SpectrumBlocks.DROOPLEAF);
        world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos.up());
        world.scheduleBlockTick(pos.up(), state.getBlock(), 100);
    }
    private static void playDropSound(World world, BlockPos pos, SoundEvent soundEvent) {
        float f = MathHelper.nextBetween(world.random, 0.8F, 1.2F);
        world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, f);
    }
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(state.get(Properties.INVERTED))
        {
            if(world.getBlockState(pos.up()).getBlock() == SpectrumBlocks.DROOPLEAF_STEM)
            {
                world.scheduleBlockTick(pos.up(), this, 100);
                riseLeaf(state, world, pos,  SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_UP);
            }
            else{
                world.setBlockState(pos, state.with(Properties.INVERTED, false), Block.NOTIFY_LISTENERS);
            }
        }
        else
        {
            if(world.getBlockState(pos.down()).getBlock() == SpectrumBlocks.DROOPLEAF_STEM)
            {
                dropLeaf(state, world, pos,  SoundEvents.BLOCK_BIG_DRIPLEAF_TILT_DOWN);
            }
            else{
                world.setBlockState(pos, state.with(Properties.INVERTED, true), Block.NOTIFY_LISTENERS);
                world.scheduleBlockTick(pos, state.getBlock(), 100);
            }
        }
    }
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(0.0, 11.0, 0.0, 16.0, 15.0, 16.0);
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes.get(state);
    }
}
