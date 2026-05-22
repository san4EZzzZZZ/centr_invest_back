package interview_prep.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
public class BearerAuthInterceptor implements HandlerInterceptor {
    private final AuthSessionRepository sessions;

    public BearerAuthInterceptor(AuthSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return true;
        }

        sessions.findByTokenWithUser(header.substring(7))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .ifPresent(session -> {
                    UserAccount user = session.getUser();
                    CurrentUserContext.set(user);
                });
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
