import { beforeEach, describe, expect, it, vi } from "vitest";
import { getAccessToken } from "./accessToken";

const authMock = vi.hoisted(() => ({
  account: { username: "operador@example.com" },
  acquireTokenSilent: vi.fn(),
}));

vi.mock("./config", () => ({ authSettings: { loginRequest: { scopes: ["api://test/access"] } } }));
vi.mock("./msal", () => ({ msalInstance: {
  getActiveAccount: () => authMock.account,
  getAllAccounts: () => [authMock.account],
  acquireTokenSilent: authMock.acquireTokenSilent,
} }));

describe("getAccessToken", () => {
  beforeEach(() => authMock.acquireTokenSilent.mockReset());

  it("solicita silenciosamente el scope de API para la cuenta activa", async () => {
    authMock.acquireTokenSilent.mockResolvedValue({ accessToken: "access-token", idToken: "id-token" });
    await expect(getAccessToken()).resolves.toBe("access-token");
    expect(authMock.acquireTokenSilent).toHaveBeenCalledWith({
      scopes: ["api://test/access"], account: authMock.account,
    });
  });

});
