package com.example.autohunter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class AutoHunterClient implements ClientModInitializer {

    private static KeyBinding keyBinding;
    private boolean active = false;
    private LivingEntity target = null;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autohunter.toggle", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_BACKSLASH, 
                "category.autohunter"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (keyBinding.wasPressed()) {
                active = !active;
                client.player.sendMessage(new LiteralText("AutoHunter: " + (active ? "§aENABLED" : "§cDISABLED")), true);
                
                if (!active) {
                    resetControls(client);
                    target = null;
                }
            }

            if (active) {
                updateAutoHunter(client);
            }
        });
    }

    private void updateAutoHunter(MinecraftClient client) {
        PlayerEntity player = client.player;
        
        if (target == null || !target.isAlive() || player.squaredDistanceTo(target) > 24 * 24) {
            target = findNearestTarget(client);
        }

        if (target != null) {
            lookAt(player, target);

            double distanceSq = player.squaredDistanceTo(target);

            if (distanceSq > 2.2 * 2.2) {
                client.options.keyForward.setPressed(true);
                
                if (player.horizontalCollision && player.isOnGround()) {
                    client.options.keyJump.setPressed(true);
                } else {
                    client.options.keyJump.setPressed(false);
                }
            } else {
                client.options.keyForward.setPressed(false);
                client.options.keyJump.setPressed(false);
            }

            if (distanceSq <= 3.8 * 3.8) {
                if (player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                    client.interactionManager.attackEntity(player, target);
                    player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else {
            resetControls(client);
        }
    }

    private LivingEntity findNearestTarget(MinecraftClient client) {
        Vec3d pos = client.player.getPos();
        List<Entity> entities = client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(20), e -> e != client.player && e.isAlive());
        
        return entities.stream()
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(pos)))
                .orElse(null);
    }

    private void lookAt(PlayerEntity player, Entity target) {
        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getEyeHeight(target.getPose())) - (player.getY() + player.getEyeHeight(player.getPose()));
        double dz = target.getZ() - player.getZ();

        double distanceXZ = MathHelper.sqrt(dx * dx + dz * dz);

        float yaw = (float) (MathHelper.atan2(dz, dx) * (180 / Math.PI)) - 90;
        float pitch = (float) (-(MathHelper.atan2(dy, distanceXZ) * (180 / Math.PI)));

        player.yaw = yaw;
        player.pitch = pitch;
    }

    private void resetControls(MinecraftClient client) {
        client.options.keyForward.setPressed(false);
        client.options.keyJump.setPressed(false);
    }
}
