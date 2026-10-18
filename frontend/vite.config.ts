import { defineConfig } from "vitest/config";
import { loadEnv } from "vite";
import react from "@vitejs/plugin-react";

function validateProductionApiBaseUrl(value: string | undefined): void {
  const apiBaseUrl = value?.trim() ?? "";

  if (!apiBaseUrl) {
    throw new Error("VITE_API_BASE_URL es obligatoria para builds de produccion.");
  }

  let parsedUrl: URL;
  try {
    parsedUrl = new URL(apiBaseUrl);
  } catch {
    throw new Error("VITE_API_BASE_URL debe ser una URL absoluta valida.");
  }

  const hostname = parsedUrl.hostname.toLowerCase();
  if (hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1") {
    throw new Error("VITE_API_BASE_URL no puede apuntar a localhost en produccion.");
  }

  if (parsedUrl.protocol !== "https:") {
    throw new Error("VITE_API_BASE_URL debe usar HTTPS en produccion.");
  }

  if (hostname.endsWith(".web.core.windows.net")) {
    throw new Error("VITE_API_BASE_URL no debe apuntar al dominio de Azure Storage Static Website.");
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendUrl = env.VITE_API_BASE_URL;

  if (mode === "production") {
    validateProductionApiBaseUrl(env.VITE_API_BASE_URL);
    for (const name of ["VITE_ENTRA_TENANT_ID", "VITE_ENTRA_CLIENT_ID", "VITE_ENTRA_API_CLIENT_ID", "VITE_ENTRA_REDIRECT_URI", "VITE_ENTRA_POST_LOGOUT_REDIRECT_URI", "VITE_API_SCOPE"]) {
      if (!env[name]?.trim()) throw new Error(`${name} es obligatoria para builds de produccion.`);
    }
    for (const name of ["VITE_ENTRA_REDIRECT_URI", "VITE_ENTRA_POST_LOGOUT_REDIRECT_URI"]) {
      let uri: URL;
      try { uri = new URL(env[name]); } catch { throw new Error(`${name} debe ser una URL absoluta valida.`); }
      if (uri.protocol !== "https:" || ["localhost", "127.0.0.1", "::1"].includes(uri.hostname)) {
        throw new Error(`${name} debe usar HTTPS y no apuntar a localhost en produccion.`);
      }
    }
  }

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": backendUrl,
        "/actuator": backendUrl,
        "/v3": backendUrl,
        "/swagger-ui": backendUrl,
      },
    },
    test: {
      environment: "jsdom",
      globals: true,
      setupFiles: "./src/test/setup.ts",
    },
  };
});
