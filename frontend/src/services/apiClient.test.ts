import { describe, expect, it } from "vitest";
import { buildApiUrl, validateApiBaseUrl } from "./apiClient";

describe("apiClient", () => {
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
