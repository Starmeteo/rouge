package com.phantomcorridor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileTest {

    @Test
    void normalizesNicknameAndKeepsOnlyPasswordDigest() {
        PlayerProfile profile = new PlayerProfile();
        profile.updateCredentials("  行者  ", "secret");

        assertEquals("行者", profile.getNickname());
        assertTrue(profile.hasPassword());
        assertTrue(profile.passwordMatches("secret"));
        assertFalse(profile.passwordMatches("wrong"));
    }

    @Test
    void passwordMayBeEmpty() {
        PlayerProfile profile = new PlayerProfile();
        profile.updateCredentials("旅者", "");

        assertFalse(profile.hasPassword());
        assertTrue(profile.passwordMatches(null));
    }
}
