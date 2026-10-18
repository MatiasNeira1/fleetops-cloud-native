import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "leaflet/dist/leaflet.css";
import "./styles.css";
import { MsalProvider } from "@azure/msal-react";

async function start() {
  const [{ default: App }, { msalInstance }] = await Promise.all([
    import("./App"),
    import("./auth/msal"),
  ]);
  await msalInstance.initialize();
  const result = await msalInstance.handleRedirectPromise();
  if (result?.account) {
    msalInstance.setActiveAccount(result.account);
  }
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <MsalProvider instance={msalInstance}><App /></MsalProvider>
    </StrictMode>
  );
}

void start().catch(() => {
  const root = document.getElementById("root");
  if (root) root.textContent = "No fue posible iniciar FleetOps. Revisa la configuración de autenticación.";
});
