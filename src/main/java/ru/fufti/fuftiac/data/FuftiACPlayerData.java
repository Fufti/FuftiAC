package ru.fufti.fuftiac.data;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class FuftiACPlayerData {

    private final UUID uuid;
    private boolean alerts = true;
    private boolean punishing;

    private final Map<String, Integer> vl = new HashMap<>();
    private final Map<String, Long> lastFlag = new HashMap<>();

    private double lastX;
    private double lastY;
    private double lastZ;
    private float lastYaw;
    private float lastPitch;
    private double lastDeltaYaw;
    private double lastDeltaPitch;
    private double speed;
    private double deltaY;
    private double lastDeltaY;
    private boolean onGround = true;
    private boolean clientOnGround = true;
    private long lastPacketMicroHop;
    private Location lastValidLocation;

    private long lastStopMs = System.currentTimeMillis();
    private long lastMoveMs = System.currentTimeMillis();
    private long lastDamageMs;

    private ItemStack lastChest;
    private long lastVanillaElytraEquip;
    private long lastElytraSwapTime;
    private long lastElytraFlagMs;
    private long lastConsumeMs;

    private boolean guiOpen;
    private final Deque<Long> guiClicks = new ArrayDeque<>();
    private int guiMoveTicks;
    private double guiDistance;

    private final Deque<Double> yawDeltas = new ArrayDeque<>();
    private final Deque<Long> npcHits = new ArrayDeque<>();
    private int npcWindows;

    private long lastAttackMs;
    private UUID lastAttackTarget;
    private int circleTicks;
    private int lastCircleSign;
    private double lastTargetAngle;

    private long lastGhostPlaceMs;
    private Location lastGhostPlace;
    private int spiderBuffer;

    private int npcEntityId = -1;
    private UUID npcUuid;
    private double npcAngle;
    private boolean npcSpawned;

    private long lastCombatCheckMs;
    private double lastLookAngle = 180.0;
    private double lookAngle = 180.0;
    private int noWebBuffer;
    private int hitboxBuffer;
    private int wallBuffer;
    private int noSlowBuffer;
    private int scaffoldBuffer;
    private int speedBuffer;
    private int reachBuffer;
    private int flyBuffer;
    private int jesusBuffer;
    private int noFallBuffer;
    private int stepBuffer;
    private int airJumpBuffer;
    private int velocityBuffer;
    private int velocityLegitBuffer;
    private int critBuffer;
    private int boatFlyBuffer;
    private int noClipBuffer;
    private int elytraRecastBuffer;
    private int airStuckBuffer;
    private int nukerBuffer;
    private int airTicks;
    private long lastVelocityCheckMs;
    private boolean setbackPushActive;
    private ru.fufti.fuftiac.util.FuftiACTask animationTask;

    private Vector pendingVelocity;
    private Vector lastVelocityExpected;
    private Location velocityStartLoc;
    private long lastVelocityMs;
    private boolean hasPendingVelocity;

    private long lastBlockDamageMs;
    private Location lastDamageBlockLoc;
    private final Deque<Long> breakTimestamps = new ArrayDeque<>();

    private final AtomicLong timerLastNs = new AtomicLong();
    private final AtomicLong timerBalanceNs = new AtomicLong();
    private final AtomicInteger timerOverflow = new AtomicInteger();
    private int consecutiveFastPackets;
    private int consecutiveSlowPackets;
    private boolean timerBurstDetected;
    private long timerWindowStartMs = System.currentTimeMillis();
    private int flyingPacketsInWindow;

    public FuftiACPlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public boolean alerts() {
        return this.alerts;
    }

    public void setAlerts(boolean alerts) {
        this.alerts = alerts;
    }

    public boolean punishing() {
        return this.punishing;
    }

    public void setPunishing(boolean punishing) {
        this.punishing = punishing;
    }

    public int vl(String check) {
        return this.vl.getOrDefault(check, 0);
    }

    public int addVl(String check) {
        int next = vl(check) + 1;
        this.vl.put(check, next);
        this.lastFlag.put(check, System.currentTimeMillis());
        return next;
    }

    public void decay(long decayMs) {
        long now = System.currentTimeMillis();
        this.lastFlag.entrySet().removeIf(entry -> {
            if (now - entry.getValue() < decayMs) {
                return false;
            }
            int current = this.vl.getOrDefault(entry.getKey(), 0);
            if (current <= 1) {
                this.vl.remove(entry.getKey());
                return true;
            }
            this.vl.put(entry.getKey(), current - 1);
            entry.setValue(now);
            return false;
        });
    }

    public Map<String, Integer> allVl() {
        return this.vl;
    }

    public void updateMove(Location to, Location from) {
        this.lastX = from.getX();
        this.lastY = from.getY();
        this.lastZ = from.getZ();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        this.speed = Math.hypot(dx, dz);
        this.lastDeltaY = this.deltaY;
        this.deltaY = to.getY() - from.getY();
        this.lastDeltaYaw = wrap(to.getYaw() - this.lastYaw);
        this.lastDeltaPitch = to.getPitch() - this.lastPitch;
        this.lastYaw = to.getYaw();
        this.lastPitch = to.getPitch();
        boolean supported = isSupportedOnGround(to);
        this.onGround = supported;
        if (supported) {
            this.airTicks = 0;
            this.lastValidLocation = to.clone();
        } else {
            this.airTicks++;
        }
        this.lastMoveMs = System.currentTimeMillis();
        if (this.speed < 0.03) {
            this.lastStopMs = System.currentTimeMillis();
        }
        this.yawDeltas.addLast(Math.abs(this.lastDeltaYaw));
        while (this.yawDeltas.size() > 16) {
            this.yawDeltas.removeFirst();
        }
    }

    public double speed() {
        return this.speed;
    }

    public double deltaY() {
        return this.deltaY;
    }

    public double lastDeltaY() {
        return this.lastDeltaY;
    }

    public double lastX() {
        return this.lastX;
    }

    public double lastY() {
        return this.lastY;
    }

    public double lastZ() {
        return this.lastZ;
    }

    public float lastYaw() {
        return this.lastYaw;
    }

    public float lastPitch() {
        return this.lastPitch;
    }

    public double lastDeltaYaw() {
        return this.lastDeltaYaw;
    }

    public double lastDeltaPitch() {
        return this.lastDeltaPitch;
    }

    public long lastMoveMs() {
        return this.lastMoveMs;
    }

    public long lastStopMs() {
        return this.lastStopMs;
    }

    public boolean onGround() {
        return this.onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean clientOnGround() {
        return this.clientOnGround;
    }

    public void setClientOnGround(boolean clientOnGround) {
        this.clientOnGround = clientOnGround;
    }

    public Location lastValidLocation() {
        return this.lastValidLocation;
    }

    public void setLastValidLocation(Location loc) {
        this.lastValidLocation = loc != null ? loc.clone() : null;
    }

    public long lastDamageMs() {
        return this.lastDamageMs;
    }

    public void markDamage() {
        this.lastDamageMs = System.currentTimeMillis();
    }

    public ItemStack lastChest() {
        return this.lastChest;
    }

    public void setLastChest(ItemStack lastChest) {
        this.lastChest = lastChest;
    }

    public long lastVanillaElytraEquip() {
        return this.lastVanillaElytraEquip;
    }

    public void markVanillaElytraEquip() {
        this.lastVanillaElytraEquip = System.currentTimeMillis();
    }

    public long lastElytraSwapTime() {
        return this.lastElytraSwapTime;
    }

    public void markElytraSwap() {
        this.lastElytraSwapTime = System.currentTimeMillis();
    }

    public void setLastElytraSwapTime(long lastElytraSwapTime) {
        this.lastElytraSwapTime = lastElytraSwapTime;
    }

    public long lastConsumeMs() {
        return this.lastConsumeMs;
    }

    public void markConsume() {
        this.lastConsumeMs = System.currentTimeMillis();
    }

    public boolean guiOpen() {
        return this.guiOpen;
    }

    public void setGuiOpen(boolean guiOpen) {
        this.guiOpen = guiOpen;
        if (!guiOpen) {
            this.guiClicks.clear();
            this.guiMoveTicks = 0;
            this.guiDistance = 0.0;
        }
    }

    public void addGuiClick() {
        long now = System.currentTimeMillis();
        this.guiClicks.addLast(now);
        while (!this.guiClicks.isEmpty() && now - this.guiClicks.peekFirst() > 1000L) {
            this.guiClicks.removeFirst();
        }
    }

    public int guiClicksIn(long windowMs) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long time : this.guiClicks) {
            if (now - time <= windowMs) {
                count++;
            }
        }
        return count;
    }

    public int guiMoveTicks() {
        return this.guiMoveTicks;
    }

    public void setGuiMoveTicks(int ticks) {
        this.guiMoveTicks = ticks;
    }

    public double guiDistance() {
        return this.guiDistance;
    }

    public void setGuiDistance(double distance) {
        this.guiDistance = distance;
    }

    public double yawStdDev() {
        if (this.yawDeltas.size() < 6) {
            return 999.0;
        }
        double sum = 0.0;
        for (double d : this.yawDeltas) {
            sum += d;
        }
        double mean = sum / this.yawDeltas.size();
        double variance = 0.0;
        for (double d : this.yawDeltas) {
            variance += Math.pow(d - mean, 2);
        }
        return Math.sqrt(variance / this.yawDeltas.size());
    }

    public void addNpcHit() {
        long now = System.currentTimeMillis();
        this.npcHits.addLast(now);
        cleanNpcHits(now, 30000L);
    }

    public void cleanNpcHits(long now, long windowMs) {
        while (!this.npcHits.isEmpty() && now - this.npcHits.peekFirst() > windowMs) {
            this.npcHits.removeFirst();
        }
    }

    public int npcHitsIn(long windowMs) {
        long now = System.currentTimeMillis();
        cleanNpcHits(now, windowMs);
        return this.npcHits.size();
    }

    public void resetNpcHits() {
        this.npcHits.clear();
    }

    public int npcWindows() {
        return this.npcWindows;
    }

    public void setNpcWindows(int windows) {
        this.npcWindows = windows;
    }

    public void markAttack(UUID target) {
        this.lastAttackMs = System.currentTimeMillis();
        this.lastAttackTarget = target;
    }

    public long lastAttackMs() {
        return this.lastAttackMs;
    }

    public UUID lastAttackTarget() {
        return this.lastAttackTarget;
    }

    public int circleTicks() {
        return this.circleTicks;
    }

    public void setCircleTicks(int circleTicks) {
        this.circleTicks = circleTicks;
    }

    public int lastCircleSign() {
        return this.lastCircleSign;
    }

    public void setLastCircleSign(int lastCircleSign) {
        this.lastCircleSign = lastCircleSign;
    }

    public double lastTargetAngle() {
        return this.lastTargetAngle;
    }

    public void setLastTargetAngle(double lastTargetAngle) {
        this.lastTargetAngle = lastTargetAngle;
    }

    public long lastGhostPlaceMs() {
        return this.lastGhostPlaceMs;
    }

    public Location lastGhostPlace() {
        return this.lastGhostPlace;
    }

    public void markGhostPlace(Location loc) {
        this.lastGhostPlaceMs = System.currentTimeMillis();
        this.lastGhostPlace = loc;
    }

    public int spiderBuffer() {
        return this.spiderBuffer;
    }

    public void setSpiderBuffer(int spiderBuffer) {
        this.spiderBuffer = spiderBuffer;
    }

    public int npcEntityId() {
        return this.npcEntityId;
    }

    public void setNpcEntityId(int npcEntityId) {
        this.npcEntityId = npcEntityId;
    }

    public UUID npcUuid() {
        return this.npcUuid;
    }

    public void setNpcUuid(UUID npcUuid) {
        this.npcUuid = npcUuid;
    }

    public double npcAngle() {
        return this.npcAngle;
    }

    public void setNpcAngle(double npcAngle) {
        this.npcAngle = npcAngle;
    }

    public boolean npcSpawned() {
        return this.npcSpawned;
    }

    public void setNpcSpawned(boolean npcSpawned) {
        this.npcSpawned = npcSpawned;
    }

    public long lastCombatCheckMs() {
        return this.lastCombatCheckMs;
    }

    public void markCombatCheck() {
        this.lastCombatCheckMs = System.currentTimeMillis();
    }

    public double lastLookAngle() {
        return this.lastLookAngle;
    }

    public double lookAngle() {
        return this.lookAngle;
    }

    public void setLookAngle(double lookAngle) {
        this.lastLookAngle = this.lookAngle;
        this.lookAngle = lookAngle;
    }

    public int noWebBuffer() {
        return this.noWebBuffer;
    }

    public void setNoWebBuffer(int noWebBuffer) {
        this.noWebBuffer = noWebBuffer;
    }

    public int hitboxBuffer() {
        return this.hitboxBuffer;
    }

    public void setHitboxBuffer(int hitboxBuffer) {
        this.hitboxBuffer = hitboxBuffer;
    }

    public int wallBuffer() {
        return this.wallBuffer;
    }

    public void setWallBuffer(int wallBuffer) {
        this.wallBuffer = wallBuffer;
    }

    public int noSlowBuffer() {
        return this.noSlowBuffer;
    }

    public void setNoSlowBuffer(int noSlowBuffer) {
        this.noSlowBuffer = noSlowBuffer;
    }

    public int scaffoldBuffer() {
        return this.scaffoldBuffer;
    }

    public void setScaffoldBuffer(int scaffoldBuffer) {
        this.scaffoldBuffer = scaffoldBuffer;
    }

    public int speedBuffer() {
        return this.speedBuffer;
    }

    public void setSpeedBuffer(int speedBuffer) {
        this.speedBuffer = speedBuffer;
    }

    public long lastElytraFlagMs() {
        return this.lastElytraFlagMs;
    }

    public void markElytraFlag() {
        this.lastElytraFlagMs = System.currentTimeMillis();
    }

    public int airTicks() {
        return this.airTicks;
    }

    public void setAirTicks(int airTicks) {
        this.airTicks = airTicks;
    }

    public int reachBuffer() {
        return this.reachBuffer;
    }

    public void setReachBuffer(int reachBuffer) {
        this.reachBuffer = reachBuffer;
    }

    public int flyBuffer() {
        return this.flyBuffer;
    }

    public void setFlyBuffer(int flyBuffer) {
        this.flyBuffer = flyBuffer;
    }

    public int jesusBuffer() {
        return this.jesusBuffer;
    }

    public void setJesusBuffer(int jesusBuffer) {
        this.jesusBuffer = jesusBuffer;
    }

    public int noFallBuffer() {
        return this.noFallBuffer;
    }

    public void setNoFallBuffer(int noFallBuffer) {
        this.noFallBuffer = noFallBuffer;
    }

    public int stepBuffer() {
        return this.stepBuffer;
    }

    public void setStepBuffer(int stepBuffer) {
        this.stepBuffer = stepBuffer;
    }

    public int airJumpBuffer() {
        return this.airJumpBuffer;
    }

    public void setAirJumpBuffer(int airJumpBuffer) {
        this.airJumpBuffer = airJumpBuffer;
    }

    public int velocityBuffer() {
        return this.velocityBuffer;
    }

    public void setVelocityBuffer(int velocityBuffer) {
        this.velocityBuffer = velocityBuffer;
    }

    public int velocityLegitBuffer() {
        return this.velocityLegitBuffer;
    }

    public void setVelocityLegitBuffer(int velocityLegitBuffer) {
        this.velocityLegitBuffer = velocityLegitBuffer;
    }

    public int critBuffer() {
        return this.critBuffer;
    }

    public void setCritBuffer(int critBuffer) {
        this.critBuffer = critBuffer;
    }

    public int boatFlyBuffer() {
        return this.boatFlyBuffer;
    }

    public void setBoatFlyBuffer(int boatFlyBuffer) {
        this.boatFlyBuffer = boatFlyBuffer;
    }

    public int noClipBuffer() {
        return this.noClipBuffer;
    }

    public void setNoClipBuffer(int noClipBuffer) {
        this.noClipBuffer = noClipBuffer;
    }

    public int elytraRecastBuffer() {
        return this.elytraRecastBuffer;
    }

    public void setElytraRecastBuffer(int elytraRecastBuffer) {
        this.elytraRecastBuffer = elytraRecastBuffer;
    }

    public int airStuckBuffer() {
        return this.airStuckBuffer;
    }

    public void setAirStuckBuffer(int airStuckBuffer) {
        this.airStuckBuffer = airStuckBuffer;
    }

    public int nukerBuffer() {
        return this.nukerBuffer;
    }

    public void setNukerBuffer(int nukerBuffer) {
        this.nukerBuffer = nukerBuffer;
    }

    public long lastVelocityCheckMs() {
        return this.lastVelocityCheckMs;
    }

    public void setLastVelocityCheckMs(long lastVelocityCheckMs) {
        this.lastVelocityCheckMs = lastVelocityCheckMs;
    }

    public boolean setbackPushActive() {
        return this.setbackPushActive;
    }

    public void setSetbackPushActive(boolean setbackPushActive) {
        this.setbackPushActive = setbackPushActive;
    }

    public ru.fufti.fuftiac.util.FuftiACTask animationTask() {
        return this.animationTask;
    }

    public void setAnimationTask(ru.fufti.fuftiac.util.FuftiACTask animationTask) {
        if (this.animationTask != null && animationTask != this.animationTask) {
            this.animationTask.cancel();
        }
        this.animationTask = animationTask;
    }

    public Vector pendingVelocity() {
        return this.pendingVelocity;
    }

    public Vector lastVelocityExpected() {
        return this.lastVelocityExpected;
    }

    public Location velocityStartLoc() {
        return this.velocityStartLoc;
    }

    public long lastVelocityMs() {
        return this.lastVelocityMs;
    }

    public boolean hasPendingVelocity() {
        return this.hasPendingVelocity;
    }

    public void setPendingVelocity(Vector velocity, Location startLoc) {
        this.pendingVelocity = velocity != null ? velocity.clone() : null;
        this.lastVelocityExpected = velocity != null ? velocity.clone() : null;
        this.velocityStartLoc = startLoc != null ? startLoc.clone() : null;
        this.lastVelocityMs = System.currentTimeMillis();
        this.hasPendingVelocity = velocity != null;
    }

    public void clearPendingVelocity() {
        this.pendingVelocity = null;
        this.hasPendingVelocity = false;
    }

    public void markPacketMicroHop() {
        this.lastPacketMicroHop = System.currentTimeMillis();
    }

    public boolean hasRecentPacketMicroHop(long windowMs) {
        return System.currentTimeMillis() - this.lastPacketMicroHop <= windowMs;
    }

    public long lastBlockDamageMs() {
        return this.lastBlockDamageMs;
    }

    public Location lastDamageBlockLoc() {
        return this.lastDamageBlockLoc;
    }

    public void setLastBlockDamage(Location loc) {
        this.lastBlockDamageMs = System.currentTimeMillis();
        this.lastDamageBlockLoc = loc != null ? loc.clone() : null;
    }

    public void addBlockBreak() {
        long now = System.currentTimeMillis();
        this.breakTimestamps.addLast(now);
        while (!this.breakTimestamps.isEmpty() && now - this.breakTimestamps.peekFirst() > 2000L) {
            this.breakTimestamps.removeFirst();
        }
    }

    public int breaksInWindow(long windowMs) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long t : this.breakTimestamps) {
            if (now - t <= windowMs) {
                count++;
            }
        }
        return count;
    }

    public AtomicLong timerLastNs() {
        return this.timerLastNs;
    }

    public AtomicLong timerBalanceNs() {
        return this.timerBalanceNs;
    }

    public AtomicInteger timerOverflow() {
        return this.timerOverflow;
    }

    public int consecutiveFastPackets() {
        return this.consecutiveFastPackets;
    }

    public void setConsecutiveFastPackets(int count) {
        this.consecutiveFastPackets = count;
    }

    public int consecutiveSlowPackets() {
        return this.consecutiveSlowPackets;
    }

    public void setConsecutiveSlowPackets(int count) {
        this.consecutiveSlowPackets = count;
    }

    public boolean timerBurstDetected() {
        return this.timerBurstDetected;
    }

    public void setTimerBurstDetected(boolean detected) {
        this.timerBurstDetected = detected;
    }

    public long timerWindowStartMs() {
        return this.timerWindowStartMs;
    }

    public void setTimerWindowStartMs(long startMs) {
        this.timerWindowStartMs = startMs;
    }

    public int flyingPacketsInWindow() {
        return this.flyingPacketsInWindow;
    }

    public void setFlyingPacketsInWindow(int count) {
        this.flyingPacketsInWindow = count;
    }

    public int incrementFlyingPacketsInWindow() {
        return ++this.flyingPacketsInWindow;
    }

    public Vector velocityFromLast(Location to) {
        return new Vector(to.getX() - this.lastX, to.getY() - this.lastY, to.getZ() - this.lastZ);
    }

    public static boolean isSupportedOnGround(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                for (double y = -0.35; y <= 0.05; y += 0.20) {
                    org.bukkit.block.Block b = loc.clone().add(x, y, z).getBlock();
                    org.bukkit.Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("SLAB") || name.contains("STAIR") || name.contains("CARPET")
                            || name.contains("SNOW") || name.contains("PATH") || name.contains("FARMLAND")
                            || name.contains("BED") || name.contains("LADDER") || name.contains("VINE")
                            || name.contains("SCAFFOLD") || name.contains("WATER") || name.contains("LAVA")) {
                        return true;
                    }
                }
                org.bukkit.block.Block under = loc.clone().add(x, -1.0, z).getBlock();
                String uName = under.getType().name();
                if (uName.contains("FENCE") || uName.contains("WALL")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static float wrap(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) {
            angle -= 360.0f;
        }
        if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }
}
