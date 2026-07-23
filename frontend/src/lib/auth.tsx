import { createContext, useContext, useEffect, useState } from 'react';
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
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ me: Me; loading: boolean }>({ me: ANON, loading: true });

  useEffect(() => {
    fetchMe()
      .then((me) => setState({ me, loading: false }))
      .catch(() => setState({ me: ANON, loading: false }));
  }, []);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

/** Full-page navigation, not fetch: the OIDC handshake is a redirect chain the browser must follow. */
export const signIn = () => { window.location.href = '/oauth2/authorization/authentik'; };
