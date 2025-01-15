import * as React from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CssBaseline from "@mui/material/CssBaseline";
import FormControlLabel from "@mui/material/FormControlLabel";
import Divider from "@mui/material/Divider";
import FormLabel from "@mui/material/FormLabel";
import FormControl from "@mui/material/FormControl";
import Link from "@mui/material/Link";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import MuiCard from "@mui/material/Card";
import { styled } from "@mui/material/styles";
import ForgotPassword from "./ForgotPassword";
import { GoogleIcon, FacebookIcon, SitemarkIcon } from "./CustomIcons";
import AppTheme from "../shared-theme/AppTheme";
import ColorModeSelect from "../shared-theme/ColorModeSelect";
import { useState, useEffect } from "react";
import Alert from "@mui/material/Alert";
import { useNavigate } from "react-router-dom";
import LogoutRequired from "../logout-required/LogoutRequired";
import { checkAuth } from "../utils/checkAuth";

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

const SignInContainer = styled(Stack)(({ theme }) => ({
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

export default function SignIn(props) {
  const [passwordError, setPasswordError] = useState(false);
  const [passwordErrorMessage, setPasswordErrorMessage] = useState("");
  const [open, setOpen] = useState(false);
  const [responseMessage, setResponseMessage] = useState("");
  const [nameError, setNameError] = useState(false);
  const [nameErrorMessage, setNameErrorMessage] = useState("");

  const navigate = useNavigate();
  const [loggedIn, setLoggedIn] = useState(false);
  const [initialized, setInitialized] = useState(false);
  const [timerActive, setTimerActive] = useState(false);

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (nameError || passwordError) {
      event.preventDefault();
      return;
    }
    const data = new FormData(event.currentTarget);

    const user = {
      username: data.get("username"),
      password: data.get("password"),
    };

    try {
      if (loggedIn)
      {
        setInitialized(false);
      }
      const response = await fetch("http://localhost:8081/api/users/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(user),
        credentials: "include", // Sends cookies with the request
      });
      
      if (response.ok) {
        const data = await response.json();
        if (data.status === "success") {
          setResponseMessage(
            `User logged in successfully: ${data.data.username}. Redirecting...`
          );
          setLoggedIn(true);
          setTimerActive(true);
          setInitialized(true);
          setTimeout(() => {
            navigate("/dashboard");
          }, 2000);
        } else {
          // This should be a server caused error that is expected
          setResponseMessage(`Error: ${data.data.message}`);
        }
      } else {
        const errorData = await response.json();
        console.log(errorData);
        setResponseMessage(`Error: ${errorData.message || "Login failed."}`);
      }
    } catch (error) {
      // To catch any other error, for example parsing the JSON
      setResponseMessage(`Error: ${error.message}`);
    }

    // setInitialized(true);

    console.log({
      username: data.get("username"),
      password: data.get("password"),
    });
  };

  const validateInputs = () => {
    const name = document.getElementById("username");
    const password = document.getElementById("password");

    let isValid = true;

    if (!name.value || name.value.length < 1) {
      setNameError(true);
      setNameErrorMessage("Name is required.");
      isValid = false;
    } else {
      setNameError(false);
      setNameErrorMessage("");
    }

    if (!password.value || password.value.length < 6) {
      setPasswordError(true);
      setPasswordErrorMessage("Password must be at least 6 characters long.");
      isValid = false;
    } else {
      setPasswordError(false);
      setPasswordErrorMessage("");
    }

    return isValid;
  };

  useEffect(() => {
    checkAuth(setLoggedIn, setResponseMessage, setInitialized);
  }, []);

  return (
    <AppTheme {...props}>
      <CssBaseline enableColorScheme />
      {(
        <SignInContainer direction="column" justifyContent="space-between">
          <ColorModeSelect
            sx={{ position: "fixed", top: "1rem", right: "1rem" }}
          />
          {(!loggedIn || timerActive) && initialized && (
            <Card variant="outlined">
              <SitemarkIcon />
              <Typography
                component="h1"
                variant="h4"
                sx={{ width: "100%", fontSize: "clamp(2rem, 10vw, 2.15rem)" }}
              >
                Sign in
              </Typography>
              <Box
                component="form"
                onSubmit={handleSubmit}
                noValidate
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  gap: 2,
                }}
              >
                <FormControl>
                  <FormLabel htmlFor="username">Username</FormLabel>
                  <TextField
                    error={nameError}
                    helperText={nameErrorMessage}
                    id="username"
                    type="username"
                    name="username"
                    placeholder="Username"
                    autoComplete="Username"
                    autoFocus
                    required
                    fullWidth
                    variant="outlined"
                    color={nameError ? "error" : "primary"}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel htmlFor="password">Password</FormLabel>
                  <TextField
                    error={passwordError}
                    helperText={passwordErrorMessage}
                    name="password"
                    placeholder="••••••"
                    type="password"
                    id="password"
                    autoComplete="current-password"
                    autoFocus
                    required
                    fullWidth
                    variant="outlined"
                    color={passwordError ? "error" : "primary"}
                  />
                </FormControl>
                <FormControlLabel
                  control={<Checkbox value="remember" color="primary" />}
                  label="Remember me"
                />
                <ForgotPassword open={open} handleClose={handleClose} />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  onClick={validateInputs}
                >
                  Sign in
                </Button>
                <Link
                  component="button"
                  type="button"
                  onClick={handleClickOpen}
                  variant="body2"
                  sx={{ alignSelf: "center" }}
                >
                  Forgot your password?
                </Link>
              </Box>
              <Divider>or</Divider>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <Button
                  fullWidth
                  variant="outlined"
                  onClick={() => alert("Sign in with Google")}
                  startIcon={<GoogleIcon />}
                >
                  Sign in with Google
                </Button>
                <Button
                  fullWidth
                  variant="outlined"
                  onClick={() => alert("Sign in with Facebook")}
                  startIcon={<FacebookIcon />}
                >
                  Sign in with Facebook
                </Button>
                <Typography sx={{ textAlign: "center" }}>
                  Don&apos;t have an account?{" "}
                  <Link
                    href="/material-ui/getting-started/templates/sign-in/"
                    variant="body2"
                    sx={{ alignSelf: "center" }}
                  >
                    Sign up
                  </Link>
                </Typography>
              </Box>
            </Card>
          )}
          {responseMessage && timerActive && (
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
          {loggedIn && initialized && !timerActive && (
            <LogoutRequired message="You need to logout first in order to sign in to another account." />
          )}
        </SignInContainer>
      )}
    </AppTheme>
  );
}
