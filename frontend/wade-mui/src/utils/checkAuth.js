export const checkAuth = async (
  setLoggedIn,
  setResponseMessage,
  setInitialized
) => {
  try {
    setInitialized(false);
    const response = await fetch(
      "http://localhost:8081/api/users/validate-token",
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
      } else {
        setResponseMessage("");
        setLoggedIn(true);
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
