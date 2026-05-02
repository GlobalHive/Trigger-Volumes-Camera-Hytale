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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CameraEffect extends TriggerEffect {

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

    private Vector3d positionData = new Vector3d();
    private Vector3d rotationData = new Vector3d();

    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            if (custom) {
                Position position = PositionUtil.toPositionPacket(new Vector3d(0, 1.6, 0).add(positionData));
                Direction rotation = new Direction(
                        (float) Math.toRadians(rotationData.y),
                        (float) Math.toRadians(rotationData.x),
                        (float) Math.toRadians(rotationData.z)
                );

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

                settings.positionLerpSpeed = 1.0F;
                settings.rotationLerpSpeed = 1.0F;

                playerRef.getPacketHandler().write(
                        new SetServerCamera(ClientCameraView.Custom, true, settings)
                );
            }

            if (!custom) {
                playerRef.getPacketHandler().write(
                        new SetServerCamera(ClientCameraView.FirstPerson, false, null)
                );
            }
        }
    }

}
