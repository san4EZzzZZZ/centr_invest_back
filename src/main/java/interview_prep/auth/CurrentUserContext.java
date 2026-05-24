package interview_prep.auth;

public final class CurrentUserContext {
    private static final ThreadLocal<UserAccount> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UserAccount user) {
        CURRENT.set(user);
    }

    public static UserAccount getRequired() {
        UserAccount user = CURRENT.get();
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    public static UserAccount get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
