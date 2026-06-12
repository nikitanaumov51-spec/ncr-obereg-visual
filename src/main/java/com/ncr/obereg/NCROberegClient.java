package com.ncr.obereg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class NCROberegClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Подключаемся к низкоуровневой шине рендеринга Fabric LAST
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            // Нативный Yarn-перебор сущностей в мире клиента
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof ArmorStandEntity stand && stand.getScoreboardTags().contains("ncr_obereg")) {
                    
                    int radius = 15;
                    for (String tag : stand.getScoreboardTags()) {
                        if (tag.startsWith("radius_")) {
                            try {
                                radius = Integer.parseInt(tag.replace("radius_", ""));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    // Считаем позицию относительно камеры
                    Vec3d cameraPos = context.camera().getPos();
                    double x = stand.getX() - cameraPos.x;
                    double y = stand.getY() - cameraPos.y + 0.1;
                    double z = stand.getZ() - cameraPos.z;

                    PoseStack poseStack = context.matrixStack();
                    VertexConsumerProvider bufferSource = context.consumers();
                    if (bufferSource == null) return;
                    
                    // Используем буфер полупрозрачных слоев (translucent), совместимый с Sodium
                    VertexConsumer buffer = bufferSource.getBuffer(RenderLayer.getTranslucent());

                    // Подбираем цвет купол (RGBA)
                    int r = 255, g = 200, b = 0, a = 45; // Золотой для Латуни (40)
                    if (radius == 15)  { r = 0; g = 255; b = 50; a = 45; }   // Зеленый для Дерева
                    if (radius == 70)  { r = 0; g = 150; b = 255; a = 45; }  // Голубой для Изумруда
                    if (radius == 100) { r = 255; g = 0; b = 0; a = 60; }    // Алый для Крови

                    poseStack.pushPose();
                    Matrix4f matrix = poseStack.peek().getPositionMatrix();
                    
                    // Алгоритм 3D-сферы из 32 сегментов
                    int segments = 32;
                    for (int i = 0; i <= segments / 2; i++) {
                        double lat0 = Math.PI * (-0.5 + (double) (i - 1) / segments);
                        double r0 = radius * Math.cos(lat0);
                        double y0 = radius * Math.sin(lat0);

                        double lat1 = Math.PI * (-0.5 + (double) i / segments);
                        double r1 = radius * Math.cos(lat1);
                        double y1 = radius * Math.sin(lat1);

                        if (y0 < 0 && y1 < 0) continue; 

                        for (int j = 0; j <= segments; j++) {
                            double lng0 = 2 * Math.PI * (double) (j - 1) / segments;
                            double x0 = Math.cos(lng0);
                            double z0 = Math.sin(lng0);

                            double lng1 = 2 * Math.PI * (double) j / segments;
                            double x1 = Math.cos(lng1);
                            double z1 = Math.sin(lng1);

                            // Отрисовка 3D полигонов сферы купола в OpenGL с явным указанием endVertex() и матрицы JOML
                            buffer.vertex(matrix, (float) (x + r0 * x0), (float) (y + y0), (float) (z + r0 * z0)).color(r, g, b, a).next();
                            buffer.vertex(matrix, (float) (x + r0 * x1), (float) (y + y0), (float) (z + r0 * z1)).color(r, g, b, a).next();
                            buffer.vertex(matrix, (float) (x + r1 * x1), (float) (y + y1), (float) (z + r1 * z1)).color(r, g, b, a).next();
                            buffer.vertex(matrix, (float) (x + r1 * x0), (float) (y + y1), (float) (z + r1 * z0)).color(r, g, b, a).next();
                        }
                    }

                    poseStack.popPose();
                }
            }
        });
    }
}
