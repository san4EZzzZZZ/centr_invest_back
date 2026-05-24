package interview_prep.auth;

public record CurrentUser(Long id, String email, String username, UserRole role, String avatarUrl) {
    public static CurrentUser from(UserAccount user) {
        String avatarUrl = user.getAvatarPath() == null ? null : "/api/profile/avatar/" + user.getAvatarPath();
        return new CurrentUser(user.getId(), user.getEmail(), user.getUsername(), user.getRole(), avatarUrl);
    }
}
