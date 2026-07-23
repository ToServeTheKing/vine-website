package com.itsthevine.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Who, if anyone, is signed in.
 *
 * <p>Deliberately PUBLIC: the SPA asks on every page load, and if this required a login the site would
 * bounce anonymous visitors — every one of them — to Authentik just to render the front page.
 */
@RestController
public class MeController {

    /** @param admin true for any signed-in user; there is one level of access here */
    public record Me(boolean authenticated, boolean admin, String name) {}

    @GetMapping("/api/me")
    public Me me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        if (!signedIn) {
            return new Me(false, false, null);
        }
        String name = auth.getName();
        if (auth.getPrincipal() instanceof OidcUser user) {
            name = user.getPreferredUsername() != null ? user.getPreferredUsername() : user.getSubject();
        }
        return new Me(true, true, name);
    }
}
