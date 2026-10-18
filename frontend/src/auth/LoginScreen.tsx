import { useState } from "react";
import { useMsal } from "@azure/msal-react";
import { authSettings } from "./config";

export function LoginScreen() {
  const { instance } = useMsal();
  const [error, setError] = useState<string | null>(null);

  async function signIn() {
    setError(null);
    try {
      await instance.loginRedirect(authSettings.loginRequest);
    } catch {
      setError("No fue posible iniciar sesión. Inténtalo de nuevo.");
    }
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <div className="login-brand"><span className="brand-mark">F</span><span>FleetOps</span></div>
        <p className="eyebrow">Panel de despacho</p>
        <h1 id="login-title">Acceso a FleetOps</h1>
        <p>El acceso está protegido por Microsoft Entra ID.</p>
        {error && <p role="alert">{error}</p>}
        <button className="primary-button" type="button" onClick={() => void signIn()}>
          Iniciar sesión con Microsoft
        </button>
      </section>
    </main>
  );
}
