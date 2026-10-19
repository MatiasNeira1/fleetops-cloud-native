import { afterEach, describe, expect, it, vi } from "vitest";
import { getAccessToken } from "../auth/accessToken";
import { buildApiUrl, requestJson, sendJson, validateApiBaseUrl } from "./apiClient";

vi.mock("../auth/accessToken", () => ({ getAccessToken: vi.fn().mockResolvedValue("access-token") }));

describe("apiClient", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("obtiene access token y envía Bearer en GET", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    await requestJson("api/v1/trips");
    expect(getAccessToken).toHaveBeenCalled();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe("Bearer access-token");
  });

  it("conserva Content-Type y Bearer en POST", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    await sendJson("api/v1/incidents", "POST", { title: "Prueba" });
    expect(fetchMock.mock.calls[0][1].headers).toEqual({ "Content-Type": "application/json", Authorization: "Bearer access-token" });
  });

  it.each([[401, "Sesión inválida o expirada"], [403, "Acceso insuficiente"]])("muestra mensaje claro para %i", async (status, message) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status })));
    await expect(requestJson("api/v1/trips")).rejects.toThrow(message);
  });
  it("construye la URL de viajes con la URL base configurada", () => {
    const baseUrl = validateApiBaseUrl("https://api.example.com/fleetops", {
      isProduction: true,
    });

    expect(buildApiUrl("api/v1/trips", baseUrl)).toBe(
      "https://api.example.com/fleetops/api/v1/trips"
    );
  });

  it("elimina barras duplicadas al unir base y endpoint", () => {
    const baseUrl = validateApiBaseUrl("https://api.example.com/fleetops///", {
      isProduction: true,
    });

    expect(buildApiUrl("/api/v1/trips", baseUrl)).toBe(
      "https://api.example.com/fleetops/api/v1/trips"
    );
  });

  it("usa ruta relativa solamente cuando no hay base configurada en desarrollo", () => {
    const baseUrl = validateApiBaseUrl("", { isProduction: false });

    expect(buildApiUrl("api/v1/trips", baseUrl)).toBe("/api/v1/trips");
  });

  it("rechaza localhost durante produccion", () => {
    expect(() =>
      validateApiBaseUrl("http://localhost:8080", { isProduction: true })
    ).toThrow("localhost");
  });

  it("entrega un error claro cuando falta la URL de produccion", () => {
    expect(() => validateApiBaseUrl("", { isProduction: true })).toThrow(
      "VITE_API_BASE_URL es obligatoria"
    );
  });
});
