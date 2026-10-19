import type { Configuration, RedirectRequest } from "@azure/msal-browser";

interface EntraEnvironment {
  VITE_ENTRA_TENANT_ID?: string;
  VITE_ENTRA_CLIENT_ID?: string;
  VITE_ENTRA_API_CLIENT_ID?: string;
  VITE_ENTRA_REDIRECT_URI?: string;
  VITE_ENTRA_POST_LOGOUT_REDIRECT_URI?: string;
  VITE_API_SCOPE?: string;
}

function required(value: string | undefined, name: string): string {
  if (!value?.trim()) {
    throw new Error(`${name} es obligatoria.`);
  }
  return value.trim();
}

function redirectUri(value: string | undefined, name: string, isProduction: boolean): string {
  const uri = required(value, name);
  let parsed: URL;
  try {
    parsed = new URL(uri);
  } catch {
    throw new Error(`${name} debe ser una URL absoluta válida.`);
  }
  if (parsed.protocol !== "https:" && !(!isProduction && parsed.protocol === "http:" && ["localhost", "127.0.0.1"].includes(parsed.hostname))) {
    throw new Error(`${name} debe usar HTTPS; HTTP solo se permite en localhost durante desarrollo.`);
  }
  if (isProduction && ["localhost", "127.0.0.1", "::1"].includes(parsed.hostname)) {
    throw new Error(`${name} no puede apuntar a localhost en producción.`);
  }
  return uri;
}

export function createAuthConfig(env: EntraEnvironment, isProduction: boolean): { config: Configuration; loginRequest: RedirectRequest } {
  const tenantId = required(env.VITE_ENTRA_TENANT_ID, "VITE_ENTRA_TENANT_ID");
  const clientId = required(env.VITE_ENTRA_CLIENT_ID, "VITE_ENTRA_CLIENT_ID");
  required(env.VITE_ENTRA_API_CLIENT_ID, "VITE_ENTRA_API_CLIENT_ID");
  const scope = required(env.VITE_API_SCOPE, "VITE_API_SCOPE");
  if (scope.split(/\s+/).length !== 1) {
    throw new Error("VITE_API_SCOPE debe contener exactamente un scope.");
  }
  return {
    config: {
      auth: {
        clientId,
        authority: `https://login.microsoftonline.com/${tenantId}`,
        redirectUri: redirectUri(env.VITE_ENTRA_REDIRECT_URI, "VITE_ENTRA_REDIRECT_URI", isProduction),
        postLogoutRedirectUri: redirectUri(env.VITE_ENTRA_POST_LOGOUT_REDIRECT_URI, "VITE_ENTRA_POST_LOGOUT_REDIRECT_URI", isProduction),
      },
      cache: { cacheLocation: "sessionStorage" },
    },
    loginRequest: { scopes: [scope] },
  };
}

export const authSettings = createAuthConfig({
  VITE_ENTRA_TENANT_ID: import.meta.env.VITE_ENTRA_TENANT_ID,
  VITE_ENTRA_CLIENT_ID: import.meta.env.VITE_ENTRA_CLIENT_ID,
  VITE_ENTRA_API_CLIENT_ID: import.meta.env.VITE_ENTRA_API_CLIENT_ID,
  VITE_ENTRA_REDIRECT_URI: import.meta.env.VITE_ENTRA_REDIRECT_URI,
  VITE_ENTRA_POST_LOGOUT_REDIRECT_URI: import.meta.env.VITE_ENTRA_POST_LOGOUT_REDIRECT_URI,
  VITE_API_SCOPE: import.meta.env.VITE_API_SCOPE,
}, import.meta.env.MODE === "production");
