package com.swvague.ars_odyssey.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.swvague.ars_odyssey.block.menu.NexusConfigMenu;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ResonanceNexusCoreBlock extends Block implements EntityBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty PARTICLES = BooleanProperty.create("particles");
    public static final int LAW_TIER = 4;
    public static final int FINAL_TIER = 5;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, FINAL_TIER);

    private static final DustParticleOptions RESONANCE_DUST =
            new DustParticleOptions(new Vector3f(0.42F, 0.18F, 1.0F), 0.85F);
    private static final DustParticleOptions FINAL_SPHERE_DUST =
            new DustParticleOptions(new Vector3f(0.18F, 0.86F, 1.0F), 1.1F);
    private static final VoxelShape CENTER_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),
            Block.box(4.0D, 7.0D, 4.0D, 12.0D, 32.0D, 12.0D));
    private static final VoxelShape EDGE_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    private static final VoxelShape CORNER_SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 5.0D, 15.0D),
            Block.box(5.0D, 5.0D, 5.0D, 11.0D, 14.0D, 11.0D));

    private static boolean removingStructure;

    public ResonanceNexusCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(PART, Part.CENTER)
                .setValue(PARTICLES, true)
                .setValue(TIER, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos origin = context.getClickedPos();
        Level level = context.getLevel();
        for (Part part : Part.values()) {
            BlockPos partPos = origin.offset(part.dx, 0, part.dz);
            if (!level.getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide() || state.getValue(PART) != Part.CENTER) {
            return;
        }
        for (Part part : Part.values()) {
            if (part == Part.CENTER) {
                continue;
            }
            level.setBlock(pos.offset(part.dx, 0, part.dz), defaultBlockState()
                    .setValue(PART, part)
                    .setValue(PARTICLES, state.getValue(PARTICLES))
                    .setValue(TIER, state.getValue(TIER)), 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openConfig(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = openConfig(state, level, pos, player);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult openConfig(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos center = getCenterPos(pos, state);
        BlockState centerState = level.getBlockState(center);
        if (!centerState.is(this) || centerState.getValue(PART) != Part.CENTER) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> new NexusConfigMenu(containerId, inventory, center),
                            Component.translatable("screen.ars_odyssey.nexus_config.title")),
                    buffer -> buffer.writeBlockPos(center));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != Part.CENTER || !state.getValue(PARTICLES)) {
            return;
        }

        double cx = pos.getX() + 0.5D;
        int tier = state.getValue(TIER);
        double cy = pos.getY() + 1.25D + tier * 0.18D;
        double cz = pos.getZ() + 0.5D;
        if (tier >= FINAL_TIER) {
            spawnFinalSphereParticles(level, pos, random, cx, cz);
            return;
        }
        if (random.nextInt(Math.max(1, 3 - tier)) == 0) {
            double angle = (level.getGameTime() % 80L) / 80.0D * Math.PI * 2.0D + random.nextDouble() * 0.5D;
            double radius = 1.15D + tier * 0.18D + random.nextDouble() * 0.25D;
            level.addParticle(RESONANCE_DUST,
                    cx + Math.cos(angle) * radius,
                    cy + random.nextDouble() * (1.1D + tier * 0.25D),
                    cz + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.018D,
                    0.012D + random.nextDouble() * 0.018D,
                    Math.cos(angle) * 0.018D);
        }
        if (random.nextInt(Math.max(1, 5 - tier)) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    cx + (random.nextDouble() - 0.5D) * 0.35D,
                    pos.getY() + 1.85D + tier * 0.28D + random.nextDouble() * 0.8D,
                    cz + (random.nextDouble() - 0.5D) * 0.35D,
                    (random.nextDouble() - 0.5D) * 0.01D,
                    0.02D,
                    (random.nextDouble() - 0.5D) * 0.01D);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.ENCHANT,
                    cx,
                    pos.getY() + 1.75D,
                    cz,
                    (random.nextDouble() - 0.5D) * 2.0D,
                    -random.nextDouble() * 0.35D,
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private void spawnFinalSphereParticles(Level level, BlockPos pos, RandomSource random, double cx, double cz) {
        long time = level.getGameTime();
        double centerY = pos.getY() + 3.55D + Math.sin(time * 0.04D) * 0.10D;
        for (int i = 0; i < 3; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0D;
            double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
            double radius = 1.45D + random.nextDouble() * 0.42D;
            double sinPhi = Math.sin(phi);
            double x = Math.cos(theta + time * 0.035D) * sinPhi * radius;
            double y = Math.cos(phi) * radius * 0.78D;
            double z = Math.sin(theta + time * 0.035D) * sinPhi * radius;
            level.addParticle(FINAL_SPHERE_DUST,
                    cx + x,
                    centerY + y,
                    cz + z,
                    -z * 0.012D,
                    (random.nextDouble() - 0.5D) * 0.008D,
                    x * 0.012D);
        }
        if (random.nextInt(2) == 0) {
            double theta = random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.8D + random.nextDouble() * 0.22D;
            level.addParticle(ParticleTypes.END_ROD,
                    cx + Math.cos(theta) * radius,
                    centerY + (random.nextDouble() - 0.5D) * 2.0D,
                    cz + Math.sin(theta) * radius,
                    -Math.sin(theta) * 0.018D,
                    0.004D,
                    Math.cos(theta) * 0.018D);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Part part = state.getValue(PART);
        if (part == Part.CENTER) {
            return true;
        }
        BlockState center = level.getBlockState(pos.offset(-part.dx, 0, -part.dz));
        return center.is(this) && center.getValue(PART) == Part.CENTER;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide() && state.getValue(PART) != Part.CENTER && !player.getAbilities().instabuild) {
            popResource(level, getCenterPos(pos, state), new ItemStack(asItem()));
        }
        return result;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            removeStructure(level, getCenterPos(pos, state));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case CENTER -> CENTER_SHAPE;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> CORNER_SHAPE;
            default -> EDGE_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, PARTICLES, TIER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.CENTER ? new ResonanceNexusCoreBlockEntity(pos, state) : null;
    }

    private void setParticles(Level level, BlockPos center, boolean enabled) {
        for (Part part : Part.values()) {
            BlockPos partPos = center.offset(part.dx, 0, part.dz);
            BlockState state = level.getBlockState(partPos);
            if (state.is(this) && state.getValue(PART) == part) {
                level.setBlock(partPos, state.setValue(PARTICLES, enabled), 3);
            }
        }
    }

    public void applySettings(Level level, BlockPos center, int tier, boolean particles) {
        for (Part part : Part.values()) {
            BlockPos partPos = center.offset(part.dx, 0, part.dz);
            BlockState state = level.getBlockState(partPos);
            if (state.is(this) && state.getValue(PART) == part) {
                level.setBlock(partPos, state
                        .setValue(TIER, Math.max(0, Math.min(FINAL_TIER, tier)))
                        .setValue(PARTICLES, particles), 3);
            }
        }
    }

    private void removeStructure(LevelAccessor level, BlockPos center) {
        if (removingStructure) {
            return;
        }
        removingStructure = true;
        try {
            for (Part part : Part.values()) {
                BlockPos partPos = center.offset(part.dx, 0, part.dz);
                BlockState state = level.getBlockState(partPos);
                if (state.is(this)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        } finally {
            removingStructure = false;
        }
    }

    private BlockPos getCenterPos(BlockPos pos, BlockState state) {
        Part part = state.getValue(PART);
        return pos.offset(-part.dx, 0, -part.dz);
    }

    public enum Part implements StringRepresentable {
        NORTH_WEST("north_west", -1, -1),
        NORTH("north", 0, -1),
        NORTH_EAST("north_east", 1, -1),
        WEST("west", -1, 0),
        CENTER("center", 0, 0),
        EAST("east", 1, 0),
        SOUTH_WEST("south_west", -1, 1),
        SOUTH("south", 0, 1),
        SOUTH_EAST("south_east", 1, 1);

        private final String name;
        private final int dx;
        private final int dz;

        Part(String name, int dx, int dz) {
            this.name = name;
            this.dx = dx;
            this.dz = dz;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
