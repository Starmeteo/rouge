package com.phantomcorridor.model.entity;

import com.phantomcorridor.config.GameConfig;
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
}
