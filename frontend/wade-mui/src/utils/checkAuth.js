import API_BASE_URL from '../config';

export const checkAuth = async (
  setLoggedIn,
  setResponseMessage,
  setInitialized,
  setUser // Optional callback to set user info
) => {
  try {
    setInitialized(false);
    const response = await fetch(
      `${API_BASE_URL}/api/users/validate-token`,
      {
        method: "GET",
        credentials: "include", // Ensure cookies are sent
      }
    );

    if (response.ok) {
      const data = await response.json();
      if (data.status === "no_token") {
        setLoggedIn(false);
        setResponseMessage("");
        if (setUser) setUser({ username: "", email: "" });
      } else {
        setResponseMessage("");
        setLoggedIn(true);
        // data.data is now UserInfoDTO {username, email}
        if (setUser && data.data) {
            setUser(data.data);
        }
      }
    } else {
      const errorData = await response.json();
      setResponseMessage(`Error: ${errorData.message}`);
      setLoggedIn(false);
    }
    setInitialized(true);
  } catch (error) {
    console.error("Token validation failed:", error);
    setLoggedIn(false);
  }
};
