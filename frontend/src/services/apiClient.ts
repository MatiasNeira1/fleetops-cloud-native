import { getAccessToken } from "../auth/accessToken";

export const API_BASE_URL_ENV = "VITE_API_BASE_URL";

interface ApiBaseUrlValidationOptions {
  isProduction: boolean;
}

function isAzureStorageStaticWebsite(hostname: string): boolean {
  return hostname.endsWith(".web.core.windows.net");
}

export function validateApiBaseUrl(
  value: string | undefined,
  { isProduction }: ApiBaseUrlValidationOptions
): string {
  const apiBaseUrl = value?.trim() ?? "";

  if (!apiBaseUrl) {
    if (isProduction) {
      throw new Error(`${API_BASE_URL_ENV} es obligatoria para builds de produccion.`);
    }
    return "";
  }

  let parsedUrl: URL;
  try {
    parsedUrl = new URL(apiBaseUrl);
  } catch {
    throw new Error(`${API_BASE_URL_ENV} debe ser una URL absoluta valida.`);
  }

  const hostname = parsedUrl.hostname.toLowerCase();

  if (isProduction && (hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1")) {
    throw new Error(`${API_BASE_URL_ENV} no puede apuntar a localhost en produccion.`);
  }

  if (isProduction && parsedUrl.protocol !== "https:") {
    throw new Error(`${API_BASE_URL_ENV} debe usar HTTPS en produccion.`);
  }

  if (isProduction && isAzureStorageStaticWebsite(hostname)) {
    throw new Error(`${API_BASE_URL_ENV} no debe apuntar al dominio de Azure Storage Static Website.`);
  }

  return apiBaseUrl.replace(/\/+$/, "");
}

export function buildApiUrl(path: string, baseUrl = apiBaseUrl): string {
  const normalizedPath = path.replace(/^\/+/, "");

  if (!baseUrl) {
    return `/${normalizedPath}`;
  }

  return `${baseUrl}/${normalizedPath}`;
}

export async function requestJson<T>(path: string): Promise<T> {
  const response = await fetch(buildApiUrl(path), {
    headers: { Authorization: `Bearer ${await getAccessToken()}` },
  });
  checkResponse(response, path);
  return response.json() as Promise<T>;
}

export async function sendJson<T>(
  path: string,
  method: "POST" | "PATCH",
  body: unknown
): Promise<T> {
  const response = await fetch(buildApiUrl(path), {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${await getAccessToken()}` },
    body: JSON.stringify(body),
  });
  checkResponse(response, path);
  return response.json() as Promise<T>;
}

function checkResponse(response: Response, path: string): void {
  if (response.status === 401) throw new Error("Sesión inválida o expirada. Inicia sesión nuevamente.");
  if (response.status === 403) throw new Error("Acceso insuficiente para esta operación.");
  if (!response.ok) throw new Error(`FleetOps API error ${response.status} for ${path}`);
}

const apiBaseUrl = validateApiBaseUrl(import.meta.env.VITE_API_BASE_URL, {
  isProduction: import.meta.env.MODE === "production",
});
