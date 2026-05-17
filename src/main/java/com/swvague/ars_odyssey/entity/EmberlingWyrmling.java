package com.swvague.ars_odyssey.entity;

import com.swvague.ars_odyssey.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class EmberlingWyrmling extends TamableAnimal implements GeoEntity, PlayerRideableJumping {
    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(EmberlingWyrmling.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(EmberlingWyrmling.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GLIDING =
            SynchedEntityData.defineId(EmberlingWyrmling.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TIRED =
            SynchedEntityData.defineId(EmberlingWyrmling.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(EmberlingWyrmling.class, EntityDataSerializers.BOOLEAN);

    private static final Ingredient TEMPT_ITEMS =
            Ingredient.of(Items.BLAZE_POWDER, Items.MAGMA_CREAM, Items.FIRE_CHARGE);
    private static final int MAX_FLIGHT_STAMINA = 20 * 70;
    private static final int TIRED_RECOVERY_TICKS = 20 * 5;
    private static final int FORCED_LANDING_INTERVAL_TICKS = 20 * 60;
    private static final int FORCED_LANDING_TICKS = 90;
    private static final int WILD_FLUTTER_MIN_COOLDOWN_TICKS = 20 * 45;
    private static final int WILD_FLUTTER_RANDOM_COOLDOWN_TICKS = 20 * 45;
    private static final int WILD_FLUTTER_TICKS = 70;

    private static final RawAnimation WALK = RawAnimation.begin()
            .thenLoop("animation.emberling_wyrmling_v2.walk_closed_wings");
    private static final RawAnimation TAKEOFF_TO_FLY = RawAnimation.begin()
            .thenPlay("animation.emberling_wyrmling_v2.spread_wings_display")
            .thenLoop("animation.emberling_wyrmling_v2.fly_flap_clear_loop");
    private static final RawAnimation GLIDE = RawAnimation.begin()
            .thenPlayAndHold("animation.emberling_wyrmling_v2.glide_descend");
    private static final RawAnimation LAND = RawAnimation.begin()
            .thenPlay("animation.emberling_wyrmling_v2.fold_wings");
    private static final RawAnimation LOOK_AROUND = RawAnimation.begin()
            .thenLoop("animation.emberling_wyrmling_v2.look_around_loop");
    private static final RawAnimation SIT_GROUND = RawAnimation.begin()
            .thenLoop("animation.emberling_wyrmling_v2.sit_ground_idle");
    private static final RawAnimation MELEE = RawAnimation.begin()
            .thenPlay("animation.emberling_wyrmling_v2.melee_bite_swipe");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int flightStamina = MAX_FLIGHT_STAMINA;
    private int tiredTicks;
    private int attackAnimationTicks;
    private int mountedAirTicks;
    private int forcedLandingTicks;
    private int wildFlutterTicks;
    private int wildFlutterCooldownTicks;
    private int riderJumpTicks;
    private boolean riderJumpHeld;
    private boolean wasAirborne;

    public EmberlingWyrmling(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.wildFlutterCooldownTicks = nextWildFlutterCooldown();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.52D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(3, new FlyingFollowOwnerGoal(this, 1.15D, 5.0F, 2.0F));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.05D, TEMPT_ITEMS, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
        builder.define(FLYING, false);
        builder.define(GLIDING, false);
        builder.define(TIRED, false);
        builder.define(ATTACKING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("FlightStamina", this.flightStamina);
        tag.putInt("TiredTicks", this.tiredTicks);
        tag.putInt("MountedAirTicks", this.mountedAirTicks);
        tag.putInt("ForcedLandingTicks", this.forcedLandingTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSaddled(tag.getBoolean("Saddled"));
        this.flightStamina = tag.contains("FlightStamina") ? tag.getInt("FlightStamina") : MAX_FLIGHT_STAMINA;
        this.tiredTicks = tag.getInt("TiredTicks");
        this.mountedAirTicks = tag.getInt("MountedAirTicks");
        this.forcedLandingTicks = tag.getInt("ForcedLandingTicks");
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return TEMPT_ITEMS.test(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean clientSide = this.level().isClientSide();

        if (isFood(stack)) {
            if (!clientSide) {
                if (!isTame()) {
                    usePlayerItem(player, hand, stack);
                    if (this.random.nextInt(3) == 0) {
                        tame(player);
                        this.navigation.stop();
                        setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                } else if (isOwnedBy(player) && getHealth() < getMaxHealth()) {
                    usePlayerItem(player, hand, stack);
                    heal(4.0F);
                    playSound(SoundEvents.GENERIC_EAT, 0.5F, 1.15F);
                }
            }
            return InteractionResult.sidedSuccess(clientSide);
        }

        if (isTame() && isOwnedBy(player)) {
            if (!isSaddled() && stack.is(Items.SADDLE)) {
                if (!clientSide) {
                    setSaddled(true);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    playSound(SoundEvents.HORSE_SADDLE, 0.6F, 1.35F);
                    player.displayClientMessage(Component.translatable(
                            "message.ars_odyssey.emberling_wyrmling.saddled"), true);
                }
                return InteractionResult.sidedSuccess(clientSide);
            }

            if (isSaddled() && stack.is(Items.SADDLE)) {
                if (!clientSide) {
                    player.displayClientMessage(Component.translatable(
                            "message.ars_odyssey.emberling_wyrmling.already_saddled"), true);
                }
                return InteractionResult.sidedSuccess(clientSide);
            }

            if (isSaddled() && !player.isSecondaryUseActive()) {
                if (!clientSide) {
                    setOrderedToSit(false);
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(clientSide);
            }

            if (player.isSecondaryUseActive()) {
                if (!clientSide) {
                    boolean stay = !isOrderedToSit();
                    setOrderedToSit(stay);
                    this.navigation.stop();
                    if (stay) {
                        setDeltaMovement(Vec3.ZERO);
                        setFlying(false);
                        setGliding(false);
                    }
                    player.displayClientMessage(Component.translatable(stay
                            ? "message.ars_odyssey.emberling_wyrmling.stay"
                            : "message.ars_odyssey.emberling_wyrmling.follow"), true);
                }
                return InteractionResult.sidedSuccess(clientSide);
            }
        }

        if (stack.is(Items.SADDLE)) {
            if (!clientSide) {
                player.displayClientMessage(Component.translatable(isTame()
                        ? "message.ars_odyssey.emberling_wyrmling.not_owner"
                        : "message.ars_odyssey.emberling_wyrmling.needs_bond"), true);
            }
            return InteractionResult.sidedSuccess(clientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;
        }
        setAttacking(this.attackAnimationTicks > 0);

        if (!this.level().isClientSide()) {
            updateWildFlutterState();
            updateFlightState();
            displayRiderStaminaBar();
        }

        if (isVehicle() || isFlying() || isGliding()) {
            clearFlightFallDistance();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (isInWater()) {
            setSwimming(true);
            if (!isVehicle()) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, Math.max(movement.y, 0.03D), movement.z);
            }
        }
    }

    private void updateWildFlutterState() {
        if (isTame() || isVehicle() || isOrderedToSit() || getTarget() != null || isInWater()) {
            this.wildFlutterTicks = 0;
            if (this.wildFlutterCooldownTicks <= 0) {
                this.wildFlutterCooldownTicks = nextWildFlutterCooldown();
            }
            return;
        }

        if (this.wildFlutterTicks > 0) {
            this.wildFlutterTicks--;
            if (this.wildFlutterTicks > 42) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x * 0.96D, Math.max(motion.y, 0.10D), motion.z * 0.96D);
                setFlying(true);
                setGliding(false);
            } else {
                Vec3 motion = getDeltaMovement();
                if (motion.y < -0.10D) {
                    setDeltaMovement(motion.x * 0.98D, -0.10D, motion.z * 0.98D);
                }
                setFlying(false);
                setGliding(true);
            }

            if (this.onGround() && this.wildFlutterTicks < WILD_FLUTTER_TICKS - 8) {
                finishWildFlutter();
            }
            return;
        }

        if (!this.onGround()) {
            return;
        }

        this.wildFlutterCooldownTicks--;
        if (this.wildFlutterCooldownTicks <= 0) {
            startWildFlutter();
        }
    }

    private void startWildFlutter() {
        this.navigation.stop();
        this.wildFlutterTicks = WILD_FLUTTER_TICKS;
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        setDeltaMovement(Math.cos(angle) * 0.16D, 0.46D, Math.sin(angle) * 0.16D);
        setFlying(true);
        setGliding(false);
    }

    private void finishWildFlutter() {
        this.wildFlutterTicks = 0;
        this.wildFlutterCooldownTicks = nextWildFlutterCooldown();
        setFlying(false);
        setGliding(false);
    }

    private int nextWildFlutterCooldown() {
        return WILD_FLUTTER_MIN_COOLDOWN_TICKS + this.random.nextInt(WILD_FLUTTER_RANDOM_COOLDOWN_TICKS + 1);
    }

    private void updateFlightState() {
        if (this.wildFlutterTicks > 0) {
            this.wasAirborne = !this.onGround();
            return;
        }

        if (this.onGround()) {
            this.flightStamina = Math.min(MAX_FLIGHT_STAMINA, this.flightStamina + (isVehicle() ? 2 : 4));
            if (this.tiredTicks > 0) {
                this.tiredTicks--;
            }
            this.mountedAirTicks = 0;
            this.forcedLandingTicks = 0;
            setFlying(false);
            setGliding(false);
            setTired(this.tiredTicks > 0);
            this.wasAirborne = false;
            return;
        }

        boolean mounted = isVehicle() && getControllingPassenger() instanceof Player;
        if (mounted) {
            this.mountedAirTicks++;
            if (this.mountedAirTicks >= FORCED_LANDING_INTERVAL_TICKS && this.forcedLandingTicks <= 0) {
                startForcedLanding();
            }
        } else {
            this.mountedAirTicks = 0;
            this.riderJumpHeld = false;
            this.riderJumpTicks = 0;
        }

            if (this.forcedLandingTicks > 0) {
                this.forcedLandingTicks--;
                Vec3 movement = getDeltaMovement();
            if (movement.y < -0.22D) {
                setDeltaMovement(movement.x * 0.92D, -0.22D, movement.z * 0.92D);
            }
            setFlying(false);
            setGliding(true);
            setTired(true);
            this.wasAirborne = true;
            return;
        }

        boolean poweredFlight = mounted && this.riderJumpHeld && canSustainRiddenFlight();
        if (poweredFlight) {
            this.flightStamina = Math.max(0, this.flightStamina - 1);
            if (this.flightStamina == 0) {
                this.tiredTicks = TIRED_RECOVERY_TICKS;
            }
            setFlying(true);
            setGliding(false);
        } else if (mounted || this.wasAirborne) {
            Vec3 movement = getDeltaMovement();
            if (movement.y < -0.22D) {
                setDeltaMovement(movement.x, -0.22D, movement.z);
            }
            setFlying(false);
            setGliding(true);
        } else {
            setFlying(false);
            setGliding(false);
        }

        setTired(this.tiredTicks > 0);
        this.wasAirborne = true;
    }

    private void startForcedLanding() {
        this.mountedAirTicks = 0;
        this.forcedLandingTicks = FORCED_LANDING_TICKS;
        this.tiredTicks = Math.max(this.tiredTicks, TIRED_RECOVERY_TICKS);
        this.flightStamina = 0;
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x * 0.55D, 0.35D, movement.z * 0.55D);
        if (getControllingPassenger() instanceof Player player) {
            player.displayClientMessage(Component.translatable(
                    "message.ars_odyssey.emberling_wyrmling.forced_landing"), true);
        }
    }

    private void displayRiderStaminaBar() {
        if (!(getControllingPassenger() instanceof Player player) || this.tickCount % 10 != 0) {
            return;
        }
        int filled = Math.max(0, Math.min(10, (int) Math.round(this.flightStamina / (double) MAX_FLIGHT_STAMINA * 10.0D)));
        StringBuilder bar = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? '|' : '.');
        }
        Component state = this.forcedLandingTicks > 0
                ? Component.translatable("message.ars_odyssey.emberling_wyrmling.stamina.forced_landing")
                : this.tiredTicks > 0
                ? Component.translatable("message.ars_odyssey.emberling_wyrmling.stamina.tired")
                : Component.translatable("message.ars_odyssey.emberling_wyrmling.stamina.ready");
        player.displayClientMessage(Component.translatable(
                "message.ars_odyssey.emberling_wyrmling.stamina",
                bar.toString(),
                this.flightStamina,
                MAX_FLIGHT_STAMINA,
                state), true);
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity controller = getControllingPassenger();
        if (isAlive() && controller instanceof Player player && isSaddled()) {
            setYRot(player.getYRot());
            this.yRotO = getYRot();
            setXRot(player.getXRot() * 0.5F);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
            this.xxa = player.xxa * 0.5F;
            this.zza = player.zza;

            boolean wantsLift = this.riderJumpTicks > 0 && canSustainRiddenFlight();
            boolean wantsDescend = false;
            if (this.riderJumpTicks > 0) {
                this.riderJumpTicks--;
            }
            setSpeed((float) getAttributeValue(this.onGround() ? Attributes.MOVEMENT_SPEED : Attributes.FLYING_SPEED));
            super.travel(new Vec3(player.xxa * 0.5D, 0.0D, player.zza));

            Vec3 movement = getDeltaMovement();
            if (isGliding()) {
                double drag = wantsDescend ? 0.82D : 0.90D;
                double sink = wantsDescend ? 0.085D : 0.035D;
                double terminalFall = wantsDescend ? -0.42D : -0.22D;
                setDeltaMovement(
                        movement.x * drag,
                        Math.max(movement.y - sink, terminalFall),
                        movement.z * drag);
            } else if (!this.onGround() && !wantsLift) {
                double sink = wantsDescend ? 0.11D : 0.065D;
                double terminalFall = wantsDescend ? -0.48D : -0.34D;
                setDeltaMovement(
                        movement.x * 0.88D,
                        Math.max(movement.y - sink, terminalFall),
                        movement.z * 0.88D);
            }

            if (wantsLift) {
                movement = getDeltaMovement();
                double yaw = Math.toRadians(getYRot());
                Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
                double forwardPower = player.zza > 0.0F ? player.zza * 0.065D : 0.0D;
                double lift = this.onGround() ? 0.42D : 0.115D;
                double minLift = this.onGround() ? 0.42D : 0.085D;
                setDeltaMovement(
                        movement.x * 0.92D + forward.x * forwardPower,
                        Math.min(this.onGround() ? 0.44D : 0.32D, Math.max(movement.y + lift, minLift)),
                        movement.z * 0.92D + forward.z * forwardPower);
                clearFlightFallDistance();
            }

            if (isInWater()) {
                movement = getDeltaMovement();
                double swimLift = this.riderJumpHeld ? 0.08D : 0.03D;
                setDeltaMovement(movement.x, Math.max(movement.y, swimLift), movement.z);
            }
            if (!this.onGround() || isFlying() || isGliding()) {
                clearFlightFallDistance();
            }
            return;
        }

        super.travel(travelVector);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vec3(0.0D, dimensions.height() * 0.94D, 0.25D * scaleFactor);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.attackAnimationTicks = 14;
        setAttacking(true);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (isVehicle() || isFlying() || isGliding() || this.wasAirborne) {
            clearFlightFallDistance();
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        if (canSustainRiddenFlight()) {
            this.riderJumpHeld = true;
            this.riderJumpTicks = 3;
            if (this.onGround()) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, 0.42D, movement.z);
                clearFlightFallDistance();
            }
        }
    }

    @Override
    public boolean canJump() {
        return isAlive()
                && isSaddled()
                && isVehicle()
                && canSustainRiddenFlight();
    }

    @Override
    public void handleStartJump(int jumpPower) {
        if (canJump()) {
            this.riderJumpHeld = true;
            this.riderJumpTicks = 3;
            if (this.onGround()) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, 0.42D, movement.z);
                clearFlightFallDistance();
            }
        }
    }

    @Override
    public void handleStopJump() {
        this.riderJumpHeld = false;
        this.riderJumpTicks = 0;
    }

    private boolean canSustainRiddenFlight() {
        return this.flightStamina > 0
                && this.tiredTicks == 0
                && this.forcedLandingTicks <= 0;
    }

    private void clearFlightFallDistance() {
        resetFallDistance();
        for (Entity passenger : getPassengers()) {
            passenger.resetFallDistance();
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModRegistry.EMBERLING_WYRMLING.get().create(level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
    }

    private PlayState movementPredicate(AnimationState<EmberlingWyrmling> state) {
        if (isAttacking()) {
            return state.setAndContinue(MELEE);
        }
        if (isOrderedToSit()) {
            return state.setAndContinue(SIT_GROUND);
        }
        if (isGliding()) {
            return state.setAndContinue(GLIDE);
        }
        if (isFlying()) {
            return state.setAndContinue(TAKEOFF_TO_FLY);
        }
        if (state.isMoving()) {
            return state.setAndContinue(WALK);
        }
        if (this.wasAirborne && this.onGround()) {
            return state.setAndContinue(LAND);
        }
        return state.setAndContinue(LOOK_AROUND);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    private void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    public boolean isGliding() {
        return this.entityData.get(GLIDING);
    }

    private void setGliding(boolean gliding) {
        this.entityData.set(GLIDING, gliding);
    }

    public boolean isTired() {
        return this.entityData.get(TIRED);
    }

    private void setTired(boolean tired) {
        this.entityData.set(TIRED, tired);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    private void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    private static class FlyingFollowOwnerGoal extends Goal {
        private final EmberlingWyrmling wyrmling;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        private LivingEntity owner;
        private int timeToRecalcPath;

        private FlyingFollowOwnerGoal(EmberlingWyrmling wyrmling, double speedModifier, float startDistance, float stopDistance) {
            this.wyrmling = wyrmling;
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = this.wyrmling.getOwner();
            if (owner == null
                    || owner.isSpectator()
                    || this.wyrmling.isOrderedToSit()
                    || this.wyrmling.isVehicle()
                    || this.wyrmling.distanceToSqr(owner) < this.startDistance * this.startDistance) {
                return false;
            }

            this.owner = owner;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                    && this.owner.isAlive()
                    && !this.owner.isSpectator()
                    && !this.wyrmling.isOrderedToSit()
                    && !this.wyrmling.isVehicle()
                    && this.wyrmling.distanceToSqr(this.owner) > this.stopDistance * this.stopDistance;
        }

        @Override
        public void stop() {
            this.owner = null;
            this.wyrmling.getNavigation().stop();
            if (!this.wyrmling.onGround()) {
                this.wyrmling.setFlying(false);
                this.wyrmling.setGliding(true);
            }
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }

            this.wyrmling.getLookControl().setLookAt(this.owner, 10.0F, this.wyrmling.getMaxHeadXRot());
            double distanceSqr = this.wyrmling.distanceToSqr(this.owner);
            double verticalGap = this.owner.getY() - this.wyrmling.getY();

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.wyrmling.getNavigation().moveTo(this.owner, this.speedModifier);
            }

            if (distanceSqr > 64.0D || Math.abs(verticalGap) > 2.0D || !this.wyrmling.getNavigation().isInProgress()) {
                flyTowardOwner();
                return;
            }

            if (this.wyrmling.onGround()) {
                this.wyrmling.setFlying(false);
                this.wyrmling.setGliding(false);
            }
        }

        private void flyTowardOwner() {
            Vec3 target = this.owner.position().add(0.0D, Math.min(2.5D, this.owner.getBbHeight() + 1.0D), 0.0D);
            Vec3 offset = target.subtract(this.wyrmling.position());
            if (offset.lengthSqr() < 0.01D) {
                return;
            }

            Vec3 steering = offset.normalize().scale(offset.lengthSqr() > 225.0D ? 0.16D : 0.11D);
            Vec3 current = this.wyrmling.getDeltaMovement();
            double yMotion = Math.max(-0.14D, Math.min(0.24D, current.y * 0.75D + steering.y));
            this.wyrmling.setDeltaMovement(
                    current.x * 0.82D + steering.x,
                    yMotion,
                    current.z * 0.82D + steering.z);
            this.wyrmling.resetFallDistance();
            this.wyrmling.setFlying(true);
            this.wyrmling.setGliding(false);
        }
    }
}
