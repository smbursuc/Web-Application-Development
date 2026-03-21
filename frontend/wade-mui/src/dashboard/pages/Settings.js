// src/dashboard/pages/Settings.js
import * as React from "react";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import FormControlLabel from "@mui/material/FormControlLabel";
import Switch from "@mui/material/Switch";
import Card from "@mui/material/Card";
import API_BASE_URL from '../../config';
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Alert from "@mui/material/Alert";
import AppNavbar from "../components/AppNavbar";
import Header from "../components/Header";
import SideMenu from "../components/SideMenu";
import AppTheme from "../../shared-theme/AppTheme";
import { useAppState } from "../../contexts/AppStateContext";
import { useState, useEffect } from "react";
import { checkAuth } from "../../utils/checkAuth";
import { useNavigate } from "react-router-dom";

export default function Settings(props) {
  const { loggedIn, setLoggedIn, setUser, setIsDirty, isDirty } = useAppState();
  const [responseMessage, setResponseMessage] = useState("");
  const [initialized, setInitialized] = useState(false);
  const navigate = useNavigate();
  const [settingOptions, setSettingOptions] = useState([]);
  const [settingsValues, setSettingsValues] = useState({});
  const [settingsLoading, setSettingsLoading] = useState(false);
  
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

  useEffect(() => {
    const loadSettings = async () => {
      if (!initialized || !loggedIn) {
        return;
      }
      try {
        setSettingsLoading(true);
        setResponseMessage("");
        const response = await fetch(`${API_BASE_URL}/api/settings`, {
          method: "GET",
          credentials: "include",
        });
        if (!response.ok) {
          throw new Error("Failed to load settings");
        }
        const data = await response.json();
        if (!Array.isArray(data?.options) || !data?.values) {
          throw new Error("Malformed settings response");
        }
        setSettingOptions(data.options);
        setSettingsValues(data.values);
        setIsDirty(false);
      } catch (e) {
        setResponseMessage("Error loading settings: " + e.message);
      } finally {
        setSettingsLoading(false);
      }
    };
    loadSettings();
  }, [initialized, loggedIn, setIsDirty]);


  // Handle Changes
  const handleChange = (key, value) => {
    setSettingsValues((prev) => ({ ...prev, [key]: value }));
    setIsDirty(true);
  };

  // Handle Save
  const handleSave = async () => {
    try {
        const payload = { ...settingsValues };
        const response = await fetch(`${API_BASE_URL}/api/settings`, {
            method: "POST",
          credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if(response.ok) {
            setResponseMessage("Settings saved successfully.");
            setIsDirty(false);
        } else {
          const errorText = await response.text();
          setResponseMessage("Failed to save settings: " + errorText);
        }
    } catch(e) {
        setResponseMessage("Error saving settings: " + e.message);
    }
  };

  // Warn on tab close
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (isDirty) {
        e.preventDefault();
        e.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  const renderSettingControl = (option) => {
    const key = option.key;
    if (!key) return null;

    if (option.type === "boolean") {
      return (
        <FormControlLabel
          key={key}
          control={
            <Switch
              checked={Boolean(settingsValues[key])}
              onChange={(e) => handleChange(key, e.target.checked)}
            />
          }
          label={option.label || key}
        />
      );
    }

    return (
      <TextField
        key={key}
        label={option.label || key}
        value={settingsValues[key] ?? ""}
        onChange={(e) => handleChange(key, e.target.value)}
        fullWidth
        variant="outlined"
        type={option.sensitive ? "password" : "text"}
        placeholder={option.placeholder || ""}
        helperText={option.description || ""}
      />
    );
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
                        Settings
                    </Typography>
                    
                    {responseMessage && <Alert severity={responseMessage.includes("Error") || responseMessage.includes("Failed") ? "error" : "success"} sx={{ mb: 2 }}>{responseMessage}</Alert>}

                    <Card variant="outlined">
                        <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                            {settingsLoading ? (
                              <Typography variant="body2" color="text.secondary">Loading settings...</Typography>
                            ) : (
                              settingOptions.map((option) => renderSettingControl(option))
                            )}

                            <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
                                <Button variant="contained" onClick={handleSave} disabled={!isDirty || settingsLoading || settingOptions.length === 0}>
                                    Save Changes
                                </Button>
                                {isDirty && <Typography variant="caption" sx={{ alignSelf: 'center', color: 'warning.main' }}>You have unsaved changes.</Typography>}
                            </Box>
                        </CardContent>
                    </Card>
                </Box>
            </Stack>
        </Box>
      </Box>
    </AppTheme>
  );
}
