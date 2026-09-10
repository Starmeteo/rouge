package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
import com.phantomcorridor.model.Difficulty;
import com.phantomcorridor.model.WorldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 敌人随层数变强：生命值按层增长，防御按层提高且每次至少掉 1 点血。 */
class EnemyFloorScalingTest {

    @Test
    void hitPointsGrowWithTheFloor() {
        int first = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 1).getMaxHp();
        int last = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, GameConfig.TOTAL_FLOORS).getMaxHp();

        assertEquals(EnemyKind.GOLEM.hitPoints(), first, "第一层用基础生命值");
        assertTrue(last > first, "高层敌人的生命值必须更高：" + first + " -> " + last);
        for (int floor = 1; floor <= GameConfig.TOTAL_FLOORS; floor++) {
            Enemy enemy = new Enemy(EnemyKind.WOLF, WorldType.SHADOW, 0, 0, floor);
            assertEquals(enemy.getMaxHp(), enemy.getHp(), "出生时应当是满血");
        }
    }

    @Test
    void bossesScaleWithTheFloorTooAndStillSwitchAtHalfHealth() {
        Enemy firstFloor = new Enemy(EnemyKind.WATCHER, WorldType.LIGHT, 0, 0, 1);
        Enemy lastFloor = new Enemy(EnemyKind.WATCHER, WorldType.LIGHT, 0, 0, GameConfig.TOTAL_FLOORS);

        assertTrue(lastFloor.getMaxHp() > firstFloor.getMaxHp());
        lastFloor.damage(lastFloor.getMaxHp() / 2);
        assertTrue(lastFloor.getHp() * 2 <= lastFloor.getMaxHp(), "半血判定要跟着放大的生命值一起用");
    }

    @Test
    void defenseRisesWithTheFloorAndIsCapped() {
        assertEquals(0, new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 1).getDefense());
        assertEquals(GameConfig.ENEMY_DEFENSE_PER_FLOOR,
                new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 2).getDefense());
        assertEquals(GameConfig.ENEMY_DEFENSE_MAX,
                new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 99).getDefense(), "防御要封顶");
    }

    @Test
    void aHitAlwaysTakesAtLeastOnePointEvenThroughDefense() {
        Enemy armoured = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, GameConfig.TOTAL_FLOORS);
        assertTrue(armoured.getDefense() > 0);

        int dealt = armoured.takeHit(1);

        assertEquals(1, dealt, "攻击力不如防御时也只能打到 1 点，不能完全免疫");
        assertEquals(armoured.getMaxHp() - 1, armoured.getHp());
    }

    @Test
    void defenseReducesButDoesNotNullifyStrongerHits() {
        Enemy armoured = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 2);
        int defense = armoured.getDefense();

        assertEquals(3 - defense, armoured.takeHit(3));
    }

    @Test
    void difficultyMultipliesTheBaseEnemyStats() {
        int easy = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 1, Difficulty.EASY).getMaxHp();
        int normal = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 1, Difficulty.NORMAL).getMaxHp();
        int hard = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 1, Difficulty.HARD).getMaxHp();
        int insane = new Enemy(EnemyKind.GOLEM, WorldType.LIGHT, 0, 0, 1, Difficulty.INSANE).getMaxHp();

        // 傀儡基础生命 5：简单 50% → 3，标准 5，困难 150% → 8，屌炸天 200% → 10
        assertEquals(3, easy);
        assertEquals(EnemyKind.GOLEM.hitPoints(), normal);
        assertEquals(8, hard);
        assertEquals(10, insane);
        assertEquals(0.5, Difficulty.EASY.enemyStatMultiplier());
        assertEquals(2.0, Difficulty.INSANE.enemyStatMultiplier());
        assertEquals("150%", Difficulty.HARD.percentText());
    }

    @Test
    void difficultyStacksOnTopOfTheFloorGrowth() {
        // 第 3 层：层数成长 1.5 倍，再乘难度倍率
        int normal = new Enemy(EnemyKind.WOLF, WorldType.LIGHT, 0, 0, 3, Difficulty.NORMAL).getMaxHp();
        int easy = new Enemy(EnemyKind.WOLF, WorldType.LIGHT, 0, 0, 3, Difficulty.EASY).getMaxHp();
        int insane = new Enemy(EnemyKind.WOLF, WorldType.LIGHT, 0, 0, 3, Difficulty.INSANE).getMaxHp();

        assertEquals(5, normal);   // 3 × 1.5
        assertEquals(2, easy);     // 3 × 1.5 × 0.5 = 2.25 → 2
        assertEquals(9, insane);   // 3 × 1.5 × 2.0 = 9
    }

    @Test
    void easyDifficultyDelaysEnemyDefenceAndInsaneStacksItFaster() {
        assertEquals(0, new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 2, Difficulty.EASY).getDefense(),
                "简单难度下第二层还没有防御");
        assertEquals(1, new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 2, Difficulty.NORMAL).getDefense());
        assertEquals(1, new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 2, Difficulty.HARD).getDefense(),
                "困难 2 层：(2-1) × 1 × 1.5 = 1.5 → 1");
        assertEquals(2, new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 2, Difficulty.INSANE).getDefense(),
                "屌炸天 2 层：(2-1) × 1 × 2 = 2");
        assertTrue(new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 5, Difficulty.INSANE).getDefense()
                        > new Enemy(EnemyKind.LANTERN, WorldType.LIGHT, 0, 0, 5, Difficulty.NORMAL).getDefense(),
                "高层数下屌炸天的防御上限应当更高");
    }

    @Test
    void enemiesWithoutAnExplicitDifficultyStayOnNormal() {
        Enemy enemy = new Enemy(EnemyKind.MAGE, WorldType.SHADOW, 0, 0, 1);
        assertEquals(Difficulty.NORMAL, enemy.getDifficulty());
        assertEquals(EnemyKind.MAGE.hitPoints(), enemy.getMaxHp());
    }
}
