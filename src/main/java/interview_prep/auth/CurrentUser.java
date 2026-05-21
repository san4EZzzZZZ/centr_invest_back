package interview_prep.auth;

public record CurrentUser(Long id, String email, String username) {
    public static CurrentUser from(UserAccount user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getUsername());
    }
}
