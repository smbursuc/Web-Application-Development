import { useNavigate } from "react-router-dom";
import { useAppState } from "../contexts/AppStateContext";

export function useLogout(setResponseMessage) {
  const navigate = useNavigate();
  const appStateProps = useAppState();
  const setLoggedIn = appStateProps.setLoggedIn;

  const logout = async () => {
    try {
      const response = await fetch("http://localhost:8081/api/users/logout", {
        method: "POST",
        credentials: "include", // Ensure cookies are sent
      });

      if (response.ok) {
        setResponseMessage("Logout successful. Redirecting...");
        setLoggedIn(false);
        setTimeout(() => {
          navigate("/"); // Navigate to the home page
        }, 2000);
      } else {
        const errorData = await response.json();
        setResponseMessage(`Error: ${errorData.message}`);
      }
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  return logout;
}
