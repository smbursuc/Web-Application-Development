import { useNavigate } from "react-router-dom";
import { useAppState } from "../contexts/AppStateContext";
import API_BASE_URL from '../config';

export function useLogout(setResponseMessage) {
  const navigate = useNavigate();
  const appStateProps = useAppState();
  const setLoggedIn = appStateProps.setLoggedIn;
  const setUser = appStateProps.setUser;

  const logout = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/users/logout`, {
        method: "POST",
        credentials: "include", // Ensure cookies are sent
      });

      if (response.ok) {
        setResponseMessage("Logout successful. Redirecting...");
        setLoggedIn(false);
        setUser({ username: "", email: "" });
        setTimeout(() => {
          navigate("/home"); // Navigate to the home page
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
