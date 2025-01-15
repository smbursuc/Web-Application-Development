import * as React from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CssBaseline from "@mui/material/CssBaseline";
import Divider from "@mui/material/Divider";
import FormControlLabel from "@mui/material/FormControlLabel";
import FormLabel from "@mui/material/FormLabel";
import FormControl from "@mui/material/FormControl";
import Link from "@mui/material/Link";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import MuiCard from "@mui/material/Card";
import { styled } from "@mui/material/styles";
import AppTheme from "../shared-theme/AppTheme";
import { GoogleIcon, FacebookIcon, SitemarkIcon } from "./CustomIcons";
import ColorModeSelect from "../shared-theme/ColorModeSelect";
import { useState, useEffect } from "react";
import Alert from "@mui/material/Alert";
import { useNavigate } from "react-router-dom";
import LogoutRequired from "../logout-required/LogoutRequired";
import { checkAuth } from "../utils/checkAuth"

const Card = styled(MuiCard)(({ theme }) => ({
  display: "flex",
  flexDirection: "column",
  alignSelf: "center",
  width: "100%",
  padding: theme.spacing(4),
  gap: theme.spacing(2),
  margin: "auto",
  boxShadow:
    "hsla(220, 30%, 5%, 0.05) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.05) 0px 15px 35px -5px",
  [theme.breakpoints.up("sm")]: {
    width: "450px",
  },
  ...theme.applyStyles("dark", {
    boxShadow:
      "hsla(220, 30%, 5%, 0.5) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.08) 0px 15px 35px -5px",
  }),
}));

const SignUpContainer = styled(Stack)(({ theme }) => ({
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

export default function SignUp(props) {
  const [emailError, setEmailError] = useState(false);
  const [emailErrorMessage, setEmailErrorMessage] = useState("");
  const [passwordError, setPasswordError] = useState(false);
  const [passwordErrorMessage, setPasswordErrorMessage] = useState("");
  const [nameError, setNameError] = useState(false);
  const [nameErrorMessage, setNameErrorMessage] = useState("");
  const [responseMessage, setResponseMessage] = useState("");

  const navigate = useNavigate();
  const [loggedIn, setLoggedIn] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const validateInputs = () => {
    const email = document.getElementById("email");
    const password = document.getElementById("password");
    const name = document.getElementById("username");

    let isValid = true;

    if (!email.value || !/\S+@\S+\.\S+/.test(email.value)) {
      setEmailError(true);
      setEmailErrorMessage("Please enter a valid email address.");
      isValid = false;
    } else {
      setEmailError(false);
      setEmailErrorMessage("");
    }

    if (!password.value || password.value.length < 6) {
      setPasswordError(true);
      setPasswordErrorMessage("Password must be at least 6 characters long.");
      isValid = false;
    } else {
      setPasswordError(false);
      setPasswordErrorMessage("");
    }

    if (!name.value || name.value.length < 1) {
      setNameError(true);
      setNameErrorMessage("Name is required.");
      isValid = false;
    } else {
      setNameError(false);
      setNameErrorMessage("");
    }

    return isValid;
  };

  const [timerActive, setTimerActive] = useState(false);
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (nameError || emailError || passwordError) {
      event.preventDefault();
      console.log("Something wong ;(");
      return;
    }
    const data = new FormData(event.currentTarget);

    const user = {
      username: data.get("username"),
      password: data.get("password"),
      email: data.get("email"),
    };

    

    try {
      if (loggedIn)
      {
        setInitialized(false);
      }
      const response = await fetch("http://localhost:8081/api/users/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(user),
      });

      if (response.ok) {
        const data = await response.json();

        // For the content (data) we are expecting login request DTO that includes:
        // * the username
        // * the message after the login

        const content = data.data;

        if (data.status === "success") {
          setResponseMessage(
            `User registered successfully: ${content.username}. Redirecting...`
          );
          setTimerActive(true);
          setInitialized(true);
          setTimeout(() => {
            navigate("/signin");
          }, 2000);
        } else {
          // This should be a server caused error that is expected
          setResponseMessage(`Error: ${content.message}`);
        }
      } else {
        const errorData = await response.json();
        setResponseMessage(
          `Error: ${errorData.message || "Registration failed."}`
        );
      }
    } catch (error) {
      // To catch any other error, for example parsing the JSON
      setResponseMessage(`Error: ${error.message}`);
    }

    console.log({
      name: data.get("username"),
      lastName: data.get("lastName"),
      email: data.get("email"),
      password: data.get("password"),
    });
  };

  useEffect(() => {
      checkAuth(setLoggedIn, setResponseMessage, setInitialized);
    }, []);

  return (
    <AppTheme {...props}>
      <CssBaseline enableColorScheme />
      <ColorModeSelect sx={{ position: "fixed", top: "1rem", right: "1rem" }} />
      <SignUpContainer direction="column" justifyContent="space-between">
        {(!loggedIn || timerActive) && initialized && (<Card variant="outlined">
          <SitemarkIcon />
          <Typography
            component="h1"
            variant="h4"
            sx={{ width: "100%", fontSize: "clamp(2rem, 10vw, 2.15rem)" }}
          >
            Sign up
          </Typography>
          <Box
            component="form"
            onSubmit={handleSubmit}
            sx={{ display: "flex", flexDirection: "column", gap: 2 }}
          >
            <FormControl>
              <FormLabel htmlFor="username">Username</FormLabel>
              <TextField
                autoComplete="username"
                name="username"
                required
                fullWidth
                id="username"
                placeholder="Username"
                error={nameError}
                helperText={nameErrorMessage}
                color={nameError ? "error" : "primary"}
              />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="email">Email</FormLabel>
              <TextField
                required
                fullWidth
                id="email"
                placeholder="your@email.com"
                name="email"
                autoComplete="email"
                variant="outlined"
                error={emailError}
                helperText={emailErrorMessage}
                color={passwordError ? "error" : "primary"}
              />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="password">Password</FormLabel>
              <TextField
                required
                fullWidth
                name="password"
                placeholder="••••••"
                type="password"
                id="password"
                autoComplete="new-password"
                variant="outlined"
                error={passwordError}
                helperText={passwordErrorMessage}
                color={passwordError ? "error" : "primary"}
              />
            </FormControl>
            <FormControlLabel
              control={<Checkbox value="allowExtraEmails" color="primary" />}
              label="I want to receive updates via email."
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              onClick={validateInputs}
            >
              Sign up
            </Button>
          </Box>
          <Divider>
            <Typography sx={{ color: "text.secondary" }}>or</Typography>
          </Divider>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
            <Button
              fullWidth
              variant="outlined"
              onClick={() => alert("Sign up with Google")}
              startIcon={<GoogleIcon />}
            >
              Sign up with Google
            </Button>
            <Button
              fullWidth
              variant="outlined"
              onClick={() => alert("Sign up with Facebook")}
              startIcon={<FacebookIcon />}
            >
              Sign up with Facebook
            </Button>
            <Typography sx={{ textAlign: "center" }}>
              Already have an account?{" "}
              <Link
                href="/material-ui/getting-started/templates/sign-in/"
                variant="body2"
                sx={{ alignSelf: "center" }}
              >
                Sign in
              </Link>
            </Typography>
          </Box>
        </Card>)}
        {responseMessage && (
          <Alert
            severity={responseMessage.includes("Error") ? "error" : "success"}
          >
            {responseMessage}
          </Alert>
        )}
        {loggedIn && initialized && (
          <LogoutRequired message="You need to logout first in order to sign up another account." />
        )}
      </SignUpContainer>
    </AppTheme>
  );
}
