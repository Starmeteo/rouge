package com.phantomcorridor.controller;

import com.phantomcorridor.model.PlayerProfile;
import com.phantomcorridor.view.LoginView;

/** 登录页输入校验与本地档案更新。 */
public final class LoginController {

    private static final int MAX_NICKNAME_LENGTH = 16;

    private final PlayerProfile profile;
    private final LoginView view;
    private final Runnable onLoggedIn;

    public LoginController(PlayerProfile profile, Runnable onLoggedIn) {
        this.profile = profile;
        this.onLoggedIn = onLoggedIn;
        this.view = new LoginView(this::submit);
        this.view.setNickname(profile.getNickname());
    }

    private void submit(String nickname, String password) {
        String normalized = PlayerProfile.normalizeNickname(nickname);
        if (normalized.isEmpty()) {
            view.showError("请输入旅者昵称");
            return;
        }
        if (normalized.length() > MAX_NICKNAME_LENGTH) {
            view.showError("昵称最多 16 个字符");
            return;
        }
        profile.updateCredentials(normalized, password);
        view.clearError();
        onLoggedIn.run();
    }

    public LoginView getView() {
        return view;
    }
}
