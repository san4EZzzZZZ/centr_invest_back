package interview_prep.profile;

import interview_prep.auth.AuthSessionRepository;
import interview_prep.auth.CurrentUser;
import interview_prep.auth.UserAccount;
import interview_prep.auth.UserAccountRepository;
import interview_prep.auth.VerificationCodeService;
import interview_prep.auth.VerificationPurpose;
import interview_prep.auth.VerificationStartResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private final UserAccountRepository users;
    private final AuthSessionRepository sessions;
    private final VerificationCodeService verificationCodes;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AvatarStorageService avatars;

    public ProfileService(UserAccountRepository users, AuthSessionRepository sessions,
                          VerificationCodeService verificationCodes, BCryptPasswordEncoder passwordEncoder,
                          AvatarStorageService avatars) {
        this.users = users;
        this.sessions = sessions;
        this.verificationCodes = verificationCodes;
        this.passwordEncoder = passwordEncoder;
        this.avatars = avatars;
    }

    @Transactional
    public CurrentUser updateUsername(UserAccount user, ProfileRequests.UpdateUsernameRequest request) {
        user.setUsername(request.username().trim());
        return CurrentUser.from(users.save(user));
    }

    @Transactional
    public VerificationStartResponse requestEmailChange(UserAccount user, ProfileRequests.RequestEmailChangeRequest request) {
        String newEmail = verificationCodes.normalizeEmail(request.newEmail());
        if (users.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        return verificationCodes.createForUser(
                VerificationPurpose.EMAIL_CHANGE,
                user,
                newEmail,
                "Подтверждение новой почты"
        );
    }

    @Transactional
    public CurrentUser confirmEmailChange(UserAccount user, ProfileRequests.ConfirmEmailChangeRequest request) {
        String newEmail = verificationCodes.normalizeEmail(request.newEmail());
        if (users.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        var code = verificationCodes.consumeUserCode(VerificationPurpose.EMAIL_CHANGE, user.getId(), request.code());
        if (!newEmail.equals(code.getNewEmail())) {
            throw new IllegalArgumentException("Confirmation code was requested for another email");
        }
        user.setEmail(newEmail);
        return CurrentUser.from(users.save(user));
    }

    @Transactional
    public VerificationStartResponse requestPasswordChange(UserAccount user) {
        return verificationCodes.createForUser(
                VerificationPurpose.PASSWORD_CHANGE,
                user,
                null,
                "Подтверждение смены пароля"
        );
    }

    @Transactional
    public void confirmPasswordChange(UserAccount user, ProfileRequests.ConfirmPasswordChangeRequest request) {
        verificationCodes.consumeUserCode(VerificationPurpose.PASSWORD_CHANGE, user.getId(), request.code());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        sessions.deleteByUserId(user.getId());
    }

    @Transactional
    public CurrentUser updateAvatar(UserAccount user, MultipartFile file) {
        String savedFile = avatars.save(file, user.getAvatarPath());
        user.setAvatarPath(savedFile);
        return CurrentUser.from(users.save(user));
    }

    @Transactional
    public CurrentUser deleteAvatar(UserAccount user) {
        avatars.delete(user.getAvatarPath());
        user.setAvatarPath(null);
        return CurrentUser.from(users.save(user));
    }
}
