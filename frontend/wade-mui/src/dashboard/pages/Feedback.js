// src/dashboard/pages/Feedback.js
import * as React from "react";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import API_BASE_URL from '../../config';
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Alert from "@mui/material/Alert";
import CircularProgress from "@mui/material/CircularProgress";
import AppNavbar from "../components/AppNavbar";
import Header from "../components/Header";
import SideMenu from "../components/SideMenu";
import AppTheme from "../../shared-theme/AppTheme";
import { useAppState } from "../../contexts/AppStateContext";
import { useState, useEffect } from "react";
import { checkAuth } from "../../utils/checkAuth";
import { useNavigate } from "react-router-dom";

export default function Feedback(props) {
  const { loggedIn, setLoggedIn, user, setUser } = useAppState();
  const [responseMessage, setResponseMessage] = useState("");
  const [alertSeverity, setAlertSeverity] = useState("info");
  const [initialized, setInitialized] = useState(false);
  const navigate = useNavigate();

  // Form
  const [message, setMessage] = useState("");
  const [isSending, setIsSending] = useState(false);

  // Auth Check
  useEffect(() => {
    checkAuth(setLoggedIn, setResponseMessage, setInitialized, setUser);
  }, []);
  
  useEffect(() => {
      if (!loggedIn && initialized) {
        setTimeout(() => {
          navigate("/");
        }, 2000);
      }
    }, [loggedIn, initialized, navigate]);

  const handleSubmit = async () => {
    if (isSending) return;
    if(message.length < 30) {
      setAlertSeverity("error");
        setResponseMessage("Error: Message must be at least 30 characters.");
        return;
    }
    if(message.length > 300) {
      setAlertSeverity("error");
        setResponseMessage("Error: Message exceeds 300 characters.");
        return;
    }
    try {
      setIsSending(true);
      setAlertSeverity("info");
      setResponseMessage("Sending feedback...");
        const payload = { 
        username: user.username,
        email: user.email,
            message 
        };
        const response = await fetch(`${API_BASE_URL}/api/feedback`, {
            method: "POST",
        credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if(response.ok) {
            setAlertSeverity("success");
            setResponseMessage("Feedback sent! Thank you.");
            setMessage("");
        } else {
            let errorText = "Failed to send feedback.";
            try {
              const errorPayload = await response.json();
              if (errorPayload?.error) {
                errorText = errorPayload.error;
              } else if (errorPayload?.message) {
                errorText = errorPayload.message;
              }
            } catch (_) {
            }
            setAlertSeverity("error");
            setResponseMessage(errorText);
        }
    } catch(e) {
        setAlertSeverity("error");
        setResponseMessage("Error: " + e.message);
    } finally {
      setIsSending(false);
    }
  };

  return (
    <AppTheme {...props}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: 'flex' }}>
        <SideMenu />
        <AppNavbar />
        <Box component="main" sx={{ flexGrow: 1, overflow: 'auto' }}>
            <Stack spacing={2} sx={{ alignItems: 'center', mx: 3, pb: 10, mt: { xs: 8, md: 0 } }}>
                <Header />
                <Box sx={{ width: '100%', maxWidth: { sm: '100%', md: '1700px' } }}>
                    <Typography component="h2" variant="h6" sx={{ mb: 2 }}>
                        Feedback
                    </Typography>
                    
                    {responseMessage && <Alert severity={alertSeverity} sx={{ mb: 2 }}>{responseMessage}</Alert>}

                    <Card variant="outlined">
                        <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                            <Typography variant="body2" color="text.secondary">
                                We value your feedback. Please let us know how we can improve.
                            </Typography>
                            
                            <TextField
                                label="From"
                                value={`${user.username || "Guest"} <${user.email || ""}>`}
                                fullWidth
                                disabled
                                variant="filled"
                            />

                            <TextField
                                label="Message"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                fullWidth
                                multiline
                                rows={12}
                                variant="filled"
                                placeholder="Type your feedback here..."
                                InputLabelProps={{ shrink: true }}
                                inputProps={{ maxLength: 300 }}
                                helperText={
                                  <Box component="span" sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                    <Typography component="span" variant="caption" color={message.length > 0 && message.length < 30 ? "error" : "text.secondary"}>
                                      {message.length > 0 && message.length < 30 ? `Min. 30 chars required (${message.length}/30)` : ""}
                                    </Typography>
                                    <Typography component="span" variant="caption" sx={{ ml: 'auto' }}>
                                      {message.length} / 300
                                    </Typography>
                                  </Box>
                                }
                            />

                            <Button variant="contained" onClick={handleSubmit} disabled={isSending} sx={{ alignSelf: 'start' }}>
                                {isSending ? (
                                  <>
                                    <CircularProgress size={16} color="inherit" sx={{ mr: 1 }} />
                                    Sending...
                                  </>
                                ) : (
                                  "Send Feedback"
                                )}
                            </Button>
                        </CardContent>
                    </Card>
                </Box>
            </Stack>
        </Box>
      </Box>
    </AppTheme>
  );
}
