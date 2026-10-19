import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { authSettings } from "./config";
import { msalInstance } from "./msal";

export async function getAccessToken(): Promise<string> {
  const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!account) {
    throw new Error("La sesión no está activa. Inicia sesión para continuar.");
  }
  try {
    const result = await msalInstance.acquireTokenSilent({
      ...authSettings.loginRequest,
      account,
    });
    return result.accessToken;
  } catch (error) {
    if (error instanceof InteractionRequiredAuthError || (
      typeof error === "object" && error !== null && "errorCode" in error &&
      ["interaction_required", "consent_required", "login_required"].includes(String(error.errorCode))
    )) {
      throw new Error("Tu sesión necesita renovarse. Selecciona 'Renovar sesión' para continuar.", { cause: error });
    }
    throw error;
  }
}
