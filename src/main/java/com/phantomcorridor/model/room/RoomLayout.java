package com.phantomcorridor.model.room;

import com.phantomcorridor.model.RoomType;
import com.phantomcorridor.model.WorldType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 按房间类型生成尺寸、轮廓和障碍物；不依赖渲染层。 */
public record RoomLayout(RoomShape shape, List<RoomArea> areas, List<Wall> walls) {
    private static final double CENTER_X = 640;
    private static final double CENTER_Y = 480;

    public RoomLayout {
        areas = List.copyOf(areas);
        walls = List.copyOf(walls);
    }

    public static RoomLayout generate(RoomType type, long seed) {
        Random random = new Random(seed ^ ((long) type.ordinal() << 40));
        if (type == RoomType.ENTRANCE) {
            return new RoomLayout(RoomShape.SQUARE,
                    List.of(centered(500, 500)), List.of());
        }

        RoomShape shape = chooseShape(type, random);
        List<RoomArea> areas = createAreas(type, shape, random);
        int obstacleCount = switch (type) {
            case REWARD, SHOP -> 0;
            case EVENT -> 1 + random.nextInt(3);
            case BOSS -> 3 + random.nextInt(3);
            default -> 3 + random.nextInt(5);
        };
        return new RoomLayout(shape, areas, createObstacles(areas, obstacleCount, random));
    }

    private static RoomShape chooseShape(RoomType type, Random random) {
        if (type == RoomType.BOSS) return random.nextBoolean() ? RoomShape.CROSS : RoomShape.RECTANGLE;
        if (type == RoomType.SHOP) return RoomShape.RECTANGLE;
        RoomShape[] options = {RoomShape.SQUARE, RoomShape.RECTANGLE, RoomShape.L_SHAPE, RoomShape.CROSS};
        return options[random.nextInt(options.length)];
    }

    private static List<RoomArea> createAreas(RoomType type, RoomShape shape, Random random) {
        double baseW = type == RoomType.BOSS ? 1030 : 690 + random.nextInt(220);
        double baseH = type == RoomType.BOSS ? 730 : 540 + random.nextInt(160);
        if (type == RoomType.REWARD) { baseW = 580; baseH = 470; }
        if (type == RoomType.SHOP) { baseW = 780; baseH = 520; }
        return switch (shape) {
            case SQUARE -> List.of(centered(Math.min(baseW, baseH), Math.min(baseW, baseH)));
            case RECTANGLE -> List.of(centered(baseW, baseH));
            case L_SHAPE -> {
                RoomArea main = centered(baseW, baseH);
                double cutW = baseW * (0.34 + random.nextDouble() * 0.12);
                double cutH = baseH * (0.34 + random.nextDouble() * 0.12);
                if (random.nextBoolean()) {
                    yield List.of(new RoomArea(main.x(), main.y(), baseW - cutW, baseH),
                            new RoomArea(main.x() + baseW - cutW, main.y() + baseH - cutH, cutW, cutH));
                }
                yield List.of(new RoomArea(main.x() + cutW, main.y(), baseW - cutW, baseH),
                        new RoomArea(main.x(), main.y() + baseH - cutH, cutW, cutH));
            }
            case CROSS -> {
                RoomArea main = centered(baseW, baseH);
                double armH = baseH * 0.52;
                double armW = baseW * 0.52;
                yield List.of(new RoomArea(main.x(), CENTER_Y - armH / 2, baseW, armH),
                        new RoomArea(CENTER_X - armW / 2, main.y(), armW, baseH));
            }
        };
    }

    private static List<Wall> createObstacles(List<RoomArea> areas, int count, Random random) {
        List<Wall> walls = new ArrayList<>();
        RoomArea primary = areas.getFirst();
        for (int i = 0; i < count; i++) {
            boolean pillar = random.nextBoolean();
            double width = pillar ? 32 + random.nextInt(42) : 72 + random.nextInt(95);
            double height = pillar ? 32 + random.nextInt(42) : 24 + random.nextInt(38);
            if (!pillar && random.nextBoolean()) {
                double swap = width; width = height; height = swap;
            }
            double x = primary.x() + 90 + random.nextDouble() * Math.max(1, primary.width() - width - 180);
            double y = primary.y() + 90 + random.nextDouble() * Math.max(1, primary.height() - height - 180);
            if (Math.hypot(x + width / 2 - CENTER_X, y + height / 2 - CENTER_Y) < 105) {
                x = primary.x() + 58;
            }
            WorldType world = switch (random.nextInt(3)) {
                case 0 -> WorldType.LIGHT;
                case 1 -> WorldType.SHADOW;
                default -> null;
            };
            walls.add(new Wall(snap(x), snap(y), snap(width), snap(height), world));
        }
        return walls;
    }

    private static RoomArea centered(double width, double height) {
        return new RoomArea(snap(CENTER_X - width / 2), snap(CENTER_Y - height / 2),
                snap(width), snap(height));
    }

    private static double snap(double value) { return Math.round(value / 8.0) * 8.0; }
}
