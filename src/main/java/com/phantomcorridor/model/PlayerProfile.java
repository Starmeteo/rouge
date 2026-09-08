package com.phantomcorridor.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 本地玩家档案。档案身份与音量、灵敏度等游戏设置相互独立。 */
public final class PlayerProfile {

    private String nickname = "";
    private String passwordHash = "";

    public String getNickname() {
        return nickname;
    }

    public boolean hasPassword() {
        return !passwordHash.isEmpty();
    }

    /** 更新本地凭据；密码允许留空，且只保存摘要。 */
    public void updateCredentials(String nickname, String password) {
        this.nickname = normalizeNickname(nickname);
        this.passwordHash = password == null || password.isBlank() ? "" : hash(password);
    }

    public boolean passwordMatches(String password) {
        String candidate = password == null || password.isBlank() ? "" : hash(password);
        return MessageDigest.isEqual(passwordHash.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }

    public static String normalizeNickname(String nickname) {
        return nickname == null ? "" : nickname.trim();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }
}
