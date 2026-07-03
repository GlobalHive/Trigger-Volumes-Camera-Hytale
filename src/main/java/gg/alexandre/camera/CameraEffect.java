package gg.alexandre.camera;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class CameraEffect extends TriggerEffect {

    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<CameraEffect> CODEC = BuilderCodec.builder(
                    CameraEffect.class, CameraEffect::new, BASE_CODEC
            )
            .append(
                    new KeyedCodec<>("Custom", Codec.BOOLEAN, false),
                    (e, v) -> e.custom = v,
                    (e) -> e.custom
            ).add()
            .append(
                    new KeyedCodec<>("Absolute", Codec.BOOLEAN, false),
                    (e, v) -> e.absolute = v,
                    (e) -> e.absolute
            ).add()
            .append(
                    new KeyedCodec<>("LookAtPlayer", Codec.BOOLEAN, false),
                    (e, v) -> e.lookPlayer = v,
                    (e) -> e.lookPlayer
            ).add()
            .append(
                    new KeyedCodec<>("Position", Vector3dUtil.CODEC, false),
                    (e, v) -> e.positionData = v,
                    (e) -> e.positionData
            ).add()
            .append(
                    new KeyedCodec<>("Rotation", Vector3dUtil.CODEC, false),
                    (e, v) -> e.rotationData = v,
                    (e) -> e.rotationData
            ).add()
            .build();

    private boolean custom = true;
    private boolean absolute = false;
    private boolean lookPlayer = false;

    private Vector3d positionData = new Vector3d();
    private Vector3d rotationData = new Vector3d();

    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        try {
            if (custom) {
                Vector3d cameraOffset = new Vector3d(0, 1.6, 0).add(positionData);
                Position position = PositionUtil.toPositionPacket(cameraOffset);
                Direction rotation = new Direction(
                        (float) Math.toRadians(rotationData.y),
                        (float) Math.toRadians(rotationData.x),
                        (float) Math.toRadians(rotationData.z)
                );

                if (lookPlayer) {
                    TransformComponent transformComponent = store.getComponent(
                            entityRef, TransformComponent.getComponentType()
                    );

                    if (transformComponent != null) {
                        Vector3d playerPos = transformComponent.getPosition();
                        Vector3d cameraPos = absolute ? new Vector3d(positionData) : new Vector3d(playerPos).add(positionData);
                        Vector3d delta = new Vector3d(playerPos).sub(cameraPos);

                        if (delta.lengthSquared() > 1.0E-9) {
                            Vector3d directionToPlayer = delta.normalize();
                            float yaw = (float) (Math.atan2(directionToPlayer.x, directionToPlayer.z) + Math.PI);
                            float pitch = (float) Math.asin(directionToPlayer.y);
                            rotation = new Direction(yaw, pitch, rotation.roll);
                        }
                    }
                }

                ServerCameraSettings settings = new ServerCameraSettings();

                if (absolute) {
                    settings.position = position;
                    settings.rotation = rotation;
                    settings.positionType = PositionType.Custom;
                    settings.rotationType = RotationType.Custom;
                } else {
                    settings.positionOffset = position;
                    settings.rotationOffset = rotation;
                }

                settings.isFirstPerson = false;
                settings.allowPitchControls = false;
                settings.sendMouseMotion = false;
                settings.positionLerpSpeed = 0.15F;
                settings.rotationLerpSpeed = 0.15F;

                playerRef.getPacketHandler().writeNoCache(
                        new SetServerCamera(ClientCameraView.Custom, true, settings)
                );
            } else {
                // Match built-in reset behavior to reliably clear server-driven camera state.
                playerRef.getPacketHandler().writeNoCache(
                        new SetServerCamera(ClientCameraView.Custom, false, null)
                );
            }
        } catch (Exception exception) {
            LOGGER.at(Level.WARNING).withCause(exception).log("Failed to execute camera trigger effect");
        }
    }

}
