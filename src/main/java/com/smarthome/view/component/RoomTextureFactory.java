package com.smarthome.view.component;

import com.smarthome.model.room.RoomType;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * ПАТТЕРН: Factory Method
 *
 * Причина: каждый тип комнаты должен получать свою текстуру, но Room3DModel
 *          не должна знать как она создаётся (сложная логика пиксельной генерации).
 * Следствие: RoomTextureFactory инкапсулирует алгоритм генерации; Room3DModel
 *            вызывает только getFloorTexture(RoomType) — единственную точку доступа.
 *
 * Текстуры процедурные (WritableImage) — не требуют внешних файлов PNG.
 * Кэшируются через EnumMap — каждый тип генерируется один раз.
 */
public class RoomTextureFactory {

    private static final int TEX_SIZE = 128;

    // Кэш уже сгенерированных текстур по типу комнаты
    private static final Map<RoomType, WritableImage> floorCache = new EnumMap<>(RoomType.class);

    // Утилитный класс — конструктор закрыт
    private RoomTextureFactory() {}

    /**
     * Возвращает текстуру пола для указанного типа комнаты.
     * Причина: разные поверхности → дерево, плитка, ковёр, бетон.
     * Следствие: визуальное разнообразие без внешних ресурсов.
     */
    public static WritableImage getFloorTexture(RoomType type) {
        return floorCache.computeIfAbsent(type, RoomTextureFactory::generate);
    }

    // === Диспетчер генерации ===

    private static WritableImage generate(RoomType type) {
        return switch (type) {
            case LIVING_ROOM, OFFICE -> generateWood(0xFFB5803A, 0xFF8B5E1A, 0xFFC49040);
            case BEDROOM             -> generateCarpet(0xFF6080C0, 0xFF4060A0);
            case KITCHEN             -> generateTile(0xFFE0E0E0, 0xFFB0B0B0, 16);
            case BATHROOM            -> generateTile(0xFF90C0D0, 0xFF607080, 12);
            case HALLWAY             -> generateConcrete(0xFF909090, 0xFF707070);
            case GARAGE              -> generateConcrete(0xFF787878, 0xFF585858);
        };
    }

    // === Алгоритмы генерации текстур ===

    /**
     * Деревянный пол: горизонтальные доски с вариациями яркости и зернистостью.
     * Причина: LIVING_ROOM и OFFICE → уютная, тёплая поверхность.
     */
    private static WritableImage generateWood(int base, int dark, int light) {
        WritableImage img = new WritableImage(TEX_SIZE, TEX_SIZE);
        PixelWriter pw = img.getPixelWriter();
        Random rng = new Random(42);
        int plankH = 16; // высота одной доски в пикселях

        for (int y = 0; y < TEX_SIZE; y++) {
            int plankIdx = y / plankH;
            // Каждая доска чуть светлее или темнее
            double shade = 0.9 + (plankIdx % 3) * 0.07;

            for (int x = 0; x < TEX_SIZE; x++) {
                // Зернистость дерева — синусоида + шум
                double grain = Math.sin((y % plankH) * 0.6 + x * 0.15) * 0.05;
                double noise = (rng.nextDouble() - 0.5) * 0.04;
                double factor = shade + grain + noise;

                int argb = lerp(base, light, Math.max(0, Math.min(1, factor - 0.9)));
                if (factor < 0.92) argb = lerp(dark, base, (factor - 0.85) / 0.07);

                // Линия шва между досками
                if (y % plankH == 0 || y % plankH == plankH - 1) {
                    argb = darken(argb, 0.6);
                }
                pw.setArgb(x, y, argb);
            }
        }
        return img;
    }

    /**
     * Плитка: чередующиеся квадраты двух цветов с тонкой затиркой.
     * Причина: KITCHEN и BATHROOM → чистая, геометрическая поверхность.
     */
    private static WritableImage generateTile(int c1, int c2, int tileSize) {
        WritableImage img = new WritableImage(TEX_SIZE, TEX_SIZE);
        PixelWriter pw = img.getPixelWriter();
        int grout = 2; // ширина шва

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                int tx = x % tileSize;
                int ty = y % tileSize;

                // Шов затирки
                if (tx < grout || ty < grout) {
                    pw.setArgb(x, y, 0xFFD8D8D8);
                    continue;
                }

                // Чередование плиток
                int col = x / tileSize;
                int row = y / tileSize;
                int argb = ((col + row) % 2 == 0) ? c1 : c2;

                // Лёгкое 3D-ощущение: угловые пиксели темнее
                double edgeFactor = 1.0;
                if (tx < grout + 2 || ty < grout + 2) edgeFactor = 0.88;
                if (tx > tileSize - grout - 3 || ty > tileSize - grout - 3) edgeFactor = 0.92;

                pw.setArgb(x, y, darken(argb, edgeFactor));
            }
        }
        return img;
    }

    /**
     * Ковёр: мягкая текстура из случайных точек, однотонная с едва заметным ворсом.
     * Причина: BEDROOM → уютная, мягкая поверхность.
     */
    private static WritableImage generateCarpet(int baseColor, int darkColor) {
        WritableImage img = new WritableImage(TEX_SIZE, TEX_SIZE);
        PixelWriter pw = img.getPixelWriter();
        Random rng = new Random(77);

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                // Ворс: случайные вариации яркости (имитация волокон)
                double fuzz = (rng.nextDouble() - 0.5) * 0.12;
                // Мягкий градиент по диагонали
                double diag = ((x + y) % 8) / 8.0 * 0.06;

                double t = Math.max(0, Math.min(1, 0.5 + fuzz + diag));
                pw.setArgb(x, y, lerp(darkColor, baseColor, t));
            }
        }
        return img;
    }

    /**
     * Бетон: серая поверхность со случайными трещинами и пятнами.
     * Причина: HALLWAY и GARAGE → промышленная, суровая поверхность.
     */
    private static WritableImage generateConcrete(int light, int dark) {
        WritableImage img = new WritableImage(TEX_SIZE, TEX_SIZE);
        PixelWriter pw = img.getPixelWriter();
        Random rng = new Random(13);

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                double noise = (rng.nextDouble() - 0.5) * 0.2;
                // Крупные пятна через синус
                double patch = Math.sin(x * 0.15) * Math.cos(y * 0.12) * 0.08;
                double t = Math.max(0, Math.min(1, 0.5 + noise + patch));
                pw.setArgb(x, y, lerp(dark, light, t));
            }
        }
        return img;
    }

    // === Цветовые утилиты ===

    /** Линейная интерполяция двух ARGB-цветов по параметру t ∈ [0,1] */
    private static int lerp(int c1, int c2, double t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF,
            g1 = (c1 >>  8) & 0xFF, b1 =  c1        & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF,
            g2 = (c2 >>  8) & 0xFF, b2 =  c2        & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Затемнение цвета на коэффициент factor ∈ [0,1] */
    private static int darken(int argb, double factor) {
        int a = (argb >> 24) & 0xFF;
        int r = (int)(((argb >> 16) & 0xFF) * factor);
        int g = (int)(((argb >>  8) & 0xFF) * factor);
        int b = (int)((argb         & 0xFF) * factor);
        return (a << 24) | (Math.min(r, 255) << 16) | (Math.min(g, 255) << 8) | Math.min(b, 255);
    }
}
