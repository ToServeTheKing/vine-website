import { createContext, useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { fetchMe, type Me } from './api';

const ANON: Me = { authenticated: false, admin: false, name: null };

const AuthContext = createContext<{ me: Me; loading: boolean }>({ me: ANON, loading: true });

/**
 * Resolves the signed-in user from the PUBLIC /api/me.
 *
 * It has to be public: this runs on every page load, and if it required a login every anonymous
 * visitor would be bounced to Authentik just to read the menu.
 */
/** Where the reader was when they clicked Sign in, so they can be put back afterwards. */
const RETURN_TO = 'vine:post-login-path';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ me: Me; loading: boolean }>({ me: ANON, loading: true });
  const navigate = useNavigate();

  useEffect(() => {
    fetchMe()
      .then((me) => {
        setState({ me, loading: false });
        if (!me.authenticated) return;
        // Spring only remembers where you were if you were BOUNCED off a protected page. Every route
        // here is public — the SPA sends you to the identity provider itself — so there is nothing
        // saved and login lands on "/". Put the reader back where they started.
        const back = sessionStorage.getItem(RETURN_TO);
        if (back) {
          sessionStorage.removeItem(RETURN_TO);
          if (back !== window.location.pathname + window.location.search) {
            navigate(back, { replace: true });
          }
        }
      })
      .catch(() => setState({ me: ANON, loading: false }));
  }, [navigate]);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

/** Full-page navigation, not fetch: the OIDC handshake is a redirect chain the browser must follow. */
export const signIn = () => {
  // sessionStorage rather than a query parameter: it survives the whole redirect chain, stays in this
  // tab, and never becomes something an attacker can point at another site.
  sessionStorage.setItem(RETURN_TO, window.location.pathname + window.location.search);
  window.location.href = '/oauth2/authorization/authentik';
};
