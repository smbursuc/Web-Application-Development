import * as React from "react";
import Button from "@mui/material/Button";
import CssBaseline from "@mui/material/CssBaseline";
import Stack from "@mui/material/Stack";
import MuiCard from "@mui/material/Card";
import { styled } from "@mui/material/styles";
import AppTheme from "../shared-theme/AppTheme";
import ColorModeSelect from "../shared-theme/ColorModeSelect";
import Alert from "@mui/material/Alert";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const Card = styled(MuiCard)(({ theme }) => ({
  display: "flex",
  flexDirection: "column",
  alignSelf: "center",
  width: "100%",
  padding: theme.spacing(4),
  gap: theme.spacing(2),
  margin: "auto",
  [theme.breakpoints.up("sm")]: {
    maxWidth: "450px",
  },
  boxShadow:
    "hsla(220, 30%, 5%, 0.05) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.05) 0px 15px 35px -5px",
  ...theme.applyStyles("dark", {
    boxShadow:
      "hsla(220, 30%, 5%, 0.5) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.08) 0px 15px 35px -5px",
  }),
}));

const Container = styled(Stack)(({ theme }) => ({
  height: "calc((1 - var(--template-frame-height, 0)) * 100dvh)",
  minHeight: "100%",
  padding: theme.spacing(2),
  [theme.breakpoints.up("sm")]: {
    padding: theme.spacing(4),
  },
  "&::before": {
    content: '""',
    display: "block",
    position: "absolute",
    zIndex: -1,
    inset: 0,
    backgroundImage:
      "radial-gradient(ellipse at 50% 50%, hsl(210, 100%, 97%), hsl(0, 0%, 100%))",
    backgroundRepeat: "no-repeat",
    ...theme.applyStyles("dark", {
      backgroundImage:
        "radial-gradient(at 50% 50%, hsla(210, 100%, 16%, 0.5), hsl(220, 30%, 5%))",
    }),
  },
}));

const LogoutRequired = ({ message }) => {
  const [responseMessage, setResponseMessage] = useState("");
  const [loggedIn, setLoggedIn] = useState(false);
  const navigate = useNavigate();

  const goBack = () => {
    navigate("/dashboard");
  };

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
            navigate("/");
          }, 2000);
      } else {
        const errorData = await response.json();
        setResponseMessage(`Error: ${errorData.message}`);
      }
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  return (
    <AppTheme>
      <CssBaseline enableColorScheme />
      {
        <Container direction="column" justifyContent="space-between">
          <ColorModeSelect
            sx={{ position: "fixed", top: "1rem", right: "1rem" }}
          />
          <Card>
            <Alert severity={"error"}>{message}</Alert>
            <Button
              fullWidth
              variant="outlined"
              onClick={logout}
            >
              Logout
            </Button>
            <Button fullWidth variant="outlined" onClick={goBack}>
              Go Back
            </Button>
          </Card>
          {responseMessage && (
            <Alert
              severity={
                responseMessage.includes("Error") && loggedIn
                  ? "error"
                  : "success"
              }
            >
              {responseMessage}
            </Alert>
          )}
        </Container>
      }
    </AppTheme>
  );
};

export default LogoutRequired;
