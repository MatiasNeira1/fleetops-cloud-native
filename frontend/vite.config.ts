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

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  const backendUrl = env.VITE_API_BASE_URL || "http://localhost:8080";

  if (command === "build" && mode === "production") {
    validateProductionApiBaseUrl(env.VITE_API_BASE_URL);
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
