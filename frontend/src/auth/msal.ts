import { PublicClientApplication } from "@azure/msal-browser";
import { authSettings } from "./config";

export const msalInstance = new PublicClientApplication(authSettings.config);
