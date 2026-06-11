package com.ncr.obereg;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

public class NCROberegClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Подключаемся напрямую к низкоуровневой шине рендеринга кадра Fabric
        WorldRenderEvents.LAST.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;

            // Сканируем все Armor Stand в радиусе видимости клиента
            for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof ArmorStand stand && stand.getTags().contains("ncr_obereg")) {
                    
                    // Извлекаем радиус защиты из тегов маркёра
                    int radius = 15;
                    for (String tag : stand.getTags()) {
                        if (tag.startsWith("radius_")) {
                            try {
                                radius = Integer.parseInt(tag.replace("radius_", ""));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    // Рассчитываем позицию маркёра относительно камеры игрока
                    Vec3 cameraPos = context.camera().getPosition();
                    double x = stand.getX() - cameraPos.x;
                    double y = stand.getY() - cameraPos.y + 0.1; // Смещаем на 10 см выше травы
                    double z = stand.getZ() - cameraPos.z;

                    PoseStack poseStack = context.matrixStack();
                    MultiBufferSource bufferSource = client.renderBuffers().bufferSource();
                    
                    // Используем буфер TRANSLUCENT (100% совместим с Sodium, Iris и Шейдерами)
                    VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());

                    // Подбираем цвет купола под уровень Оберега (Формат RGBA, прозрачность 45 из 255)
                    int r = 255, g = 200, b = 0, a = 45; // Латунь (Золотой)
                    if (radius == 15)  { r = 0; g = 255; b = 50; a = 45; }   // Дерево (Зеленый)
                    if (radius == 70)  { r = 0; g = 150; b = 255; a = 45; }  // Изумруд (Голубой)
                    if (radius == 100) { r = 255; g = 0; b = 0; a = 60; }    // Кровь (Алый)

                    poseStack.pushPose();
                    
                    // МАТЕМАТИКА 3D-СФЕРЫ: Строим купол из 32 сегментов меридиан и параллелей
                    int segments = 32;
                    for (int i = 0; i <= segments / 2; i++) {
                        double lat0 = Math.PI * (-0.5 + (double) (i - 1) / segments);
                        double r0 = radius * Math.cos(lat0);
                        double y0 = radius * Math.sin(lat0);

                        double lat1 = Math.PI * (-0.5 + (double) i / segments);
                        double r1 = radius * Math.cos(lat1);
                        double y1 = radius * Math.sin(lat1);

                        // Пропускаем нижнюю часть сферы под землей для экономии FPS видеокарты
                        if (y0 < 0 && y1 < 0) continue; 

                        for (int j = 0; j <= segments; j++) {
                            double lng0 = 2 * Math.PI * (double) (j - 1) / segments;
                            double x0 = Math.cos(lng0);
                            double z0 = Math.sin(lng0);

                            double lng1 = 2 * Math.PI * (double) j / segments;
                            double x1 = Math.cos(lng1);
                            double z1 = Math.sin(lng1);

                            // Отрисовка плоских OpenGL 3D-полигонов (полупрозрачных стекол купола)
                            buffer.vertex(poseStack.last().pose(), (float) (x + r0 * x0), (float) (y + y0), (float) (z + r0 * z0)).color(r, g, b, a).endVertex();
                            buffer.vertex(poseStack.last().pose(), (float) (x + r0 * x1), (float) (y + y0), (float) (z + r0 * z1)).color(r, g, b, a).endVertex();
                            buffer.vertex(poseStack.last().pose(), (float) (x + r1 * x1), (float) (y + y1), (float) (z + r1 * z1)).color(r, g, b, a).endVertex();
                            buffer.vertex(poseStack.last().pose(), (float) (x + r1 * x0), (float) (y + y1), (float) (z + r1 * z0)).color(r, g, b, a).endVertex();
                        }
                    }

                    poseStack.popPose();
                }
            }
        });
    }
}