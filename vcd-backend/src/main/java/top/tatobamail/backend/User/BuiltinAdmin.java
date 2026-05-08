package top.tatobamail.backend.User;

/**
 * 内置管理员账号（仅用于登录校验与 JWT 身份，可不依赖数据库中存在该用户）。
 */
public final class BuiltinAdmin {

    public static final String USERNAME = "admin";
    public static final String PASSWORD = "admin";

    private BuiltinAdmin() {
    }

    public static boolean matches(String username, String password) {
        return USERNAME.equals(username) && PASSWORD.equals(password);
    }
}
