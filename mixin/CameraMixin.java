package com.rpgcore.mixin;

import com.rpgcore.client.CameraSystem;
import com.rpgcore.data.ModAttachments;
import com.rpgcore.data.PlayerAppearanceData;
import com.rpgcore.data.RaceType;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private BlockGetter level;
    @Shadow private Entity entity;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;

    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void rpgcore$applyRdr2ThirdPersonCamera(BlockGetter level, Entity entity, boolean detached,
                                                    boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!detached || !(entity instanceof Player player) || level == null) return;

        PlayerAppearanceData data = player.getData(ModAttachments.APPEARANCE);
        float scaleW = data.race().getBaseWidth() * data.gender().getWidthScale();

        float shoulderFactor = CameraSystem.updateAndGetShoulderFactor(partialTick);
        if (thirdPersonReverse) {
            shoulderFactor = -shoulderFactor;
        }

        // 1. Вычисляем точную безопасную опорную точку внутри верхнего отдела спины/шеи игрока
        double interpX = Mth.lerp(partialTick, player.xo, player.getX());
        double interpY = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight();
        double interpZ = Mth.lerp(partialTick, player.zo, player.getZ());

        // Для Орка в стоячем положении опускаем опорную точку камеры до уровня плеч (-0.45 блока от глаз),
        // но ТОЛЬКО внутри тела игрока (до рэйкаста!), чтобы камера никогда не уходила под землю!
        boolean isStandingNormal = !player.isPassenger()
                && !player.getData(ModAttachments.CRAWLING)
                && player.getData(ModAttachments.DOWNED_TICKS) <= 0;

        if (data.race() == RaceType.ORC && isStandingNormal) {
            interpY -= 0.45D;
        }

        Vec3 eyeAnchor = new Vec3(interpX, interpY, interpZ);

        // 2. Безопасный боковой сдвиг к выбранному плечу (с проверкой стен сбоку и сверху)
        double leftShift = -shoulderFactor * 0.62D * scaleW;
        double upShift = 0.08D;

        Vec3 shoulderOffset = new Vec3(
                this.left.x() * leftShift + this.up.x() * upShift,
                this.left.y() * leftShift + this.up.y() * upShift,
                this.left.z() * leftShift + this.up.z() * upShift
        );

        Vec3 shoulderAnchor = rpgcore$clipWithMargin(eyeAnchor, eyeAnchor.add(shoulderOffset), 0.26D);

        // 3. Определяем желаемую дистанцию отдаления камеры назад (для Орка 4.7 блока, для остальных 4.0 блока)
        double maxDist = (data.race() == RaceType.ORC) ? 4.70D : 4.00D;
        Vec3 camBackDir = new Vec3(-this.forwards.x(), -this.forwards.y(), -this.forwards.z());

        // 4. Проверяем 5 лучaми (центр + 4 угла объектива камеры), чтобы края камеры не проваливались в стены и пол!
        double safeDist = rpgcore$computeSafeCameraDistance(shoulderAnchor, camBackDir, maxDist);

        Vec3 finalCameraPos = shoulderAnchor.add(camBackDir.scale(safeDist));
        this.setPosition(finalCameraPos);
    }

    /**
     * Пускает 5 лучей вокруг объектива камеры (с радиусом 0.22 блока по всем осям),
     * чтобы ближняя плоскость отсечения OpenGL (Near Plane) никогда не заглядывала внутрь стен, потолка или земли!
     */
    @Unique
    private double rpgcore$computeSafeCameraDistance(Vec3 anchor, Vec3 backDir, double maxDist) {
        double minAllowedDist = maxDist;
        double probeRadius = 0.22D;

        for (int i = 0; i < 9; i++) {
            double ox = ((i & 1) * 2 - 1) * probeRadius;
            double oy = (((i >> 1) & 1) * 2 - 1) * probeRadius;
            double oz = (((i >> 2) & 1) * 2 - 1) * probeRadius;
            if (i == 8) {
                ox = 0.0D;
                oy = 0.0D;
                oz = 0.0D;
            }

            Vec3 rayStart = anchor.add(ox, oy, oz);
            Vec3 rayEnd = rayStart.add(backDir.scale(maxDist));

            HitResult hit = this.level.clip(new ClipContext(
                    rayStart,
                    rayEnd,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    this.entity
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                // Отступаем на 0.24 блока от точки касания стены/земли, чтобы текстура блока не срезалась камерой
                double hitDist = Math.max(0.0D, hit.getLocation().distanceTo(rayStart) - 0.24D);
                if (hitDist < minAllowedDist) {
                    minAllowedDist = hitDist;
                }
            }
        }
        return minAllowedDist;
    }

    @Unique
    private Vec3 rpgcore$clipWithMargin(Vec3 from, Vec3 to, double margin) {
        if (this.level == null || this.entity == null) return to;
        Vec3 diff = to.subtract(from);
        double totalLen = diff.length();
        if (totalLen < 0.001D) return to;

        Vec3 dir = diff.normalize();
        // Проверяем луч чуть дальше целевой точки (+margin), чтобы камера не прижималась вплотную к боковой стене
        HitResult hit = this.level.clip(new ClipContext(
                from,
                from.add(dir.scale(totalLen + margin)),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                this.entity
        ));

        if (hit.getType() != HitResult.Type.MISS) {
            double hitDist = Math.max(0.0D, hit.getLocation().distanceTo(from) - margin);
            return from.add(dir.scale(Math.min(totalLen, hitDist)));
        }
        return to;
    }
}