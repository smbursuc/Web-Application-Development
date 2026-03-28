import * as React from "react";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Stack from "@mui/material/Stack";
import HomeRoundedIcon from "@mui/icons-material/HomeRounded";
import AnalyticsRoundedIcon from "@mui/icons-material/AnalyticsRounded";
import PeopleRoundedIcon from "@mui/icons-material/PeopleRounded";
import AssignmentRoundedIcon from "@mui/icons-material/AssignmentRounded";
import SettingsRoundedIcon from "@mui/icons-material/SettingsRounded";
import InfoRoundedIcon from "@mui/icons-material/InfoRounded";
import HelpRoundedIcon from "@mui/icons-material/HelpRounded";
import MenuBookRoundedIcon from "@mui/icons-material/MenuBookRounded";
import { useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAppState } from "../../contexts/AppStateContext";

export default function MenuContent() {
  const navigate = useNavigate();
  const location = useLocation();
  const appStateProps = useAppState();
  const setAboutOpen = appStateProps.setAboutOpen;
  const isDirty = appStateProps.isDirty;
  const setIsDirty = appStateProps.setIsDirty;
  const loggedIn = appStateProps.loggedIn;

  const routeByOption = {
    Home: "/dashboard",
    "SPARQL Playground": "/sparql-playground",
    "Semantic Zoom": "/semantic-zoom",
    Correlations: "/correlations",
    Settings: "/settings",
    Feedback: "/feedback",
    "How to Use": "/how-to-use",
  };

  // Fix: active highlight is now computed from the current URL path instead of a global selected index,
  // so navigation triggered outside menu clicks (e.g. clicking logo -> /dashboard) always updates highlight correctly.
  const isOptionSelected = (option) => routeByOption[option] === location.pathname;

  const handleListItemClick = (index, option) => {
    // Check for unsaved changes
    if (isDirty) {
        // Simple confirmation
        const confirmed = window.confirm("You have unsaved changes. Are you sure you want to leave?");
        if (!confirmed) return;
        setIsDirty(false); // Reset if they agree to leave
    }

    switch (option)
    {
      case "Home":
        navigate("/dashboard");
        break;
      case "SPARQL Playground":
        navigate("/sparql-playground");
        break;
      case "Semantic Zoom":
        navigate("/semantic-zoom");
        break;
      case "Correlations":
        navigate("/correlations");
        break;
      case "Settings":
        navigate("/settings");
        break;
      case "About":
        // Stay on current page, just open modal
        setAboutOpen(true);
        break;
      case "Feedback":
        navigate("/feedback");
        break;
      case "How to Use":
        navigate("/how-to-use");
        break;
    }
  };

  const mainListItems = [
    { text: "Home", icon: <HomeRoundedIcon /> },
    { text: "SPARQL Playground", icon: <AnalyticsRoundedIcon /> },
    { text: "Semantic Zoom", icon: <PeopleRoundedIcon /> },
    { text: "Correlations", icon: <AssignmentRoundedIcon /> },
  ];

  const secondaryListItems = [
    { text: "Settings", icon: <SettingsRoundedIcon />, guestDisabled: true },
    { text: "How to Use", icon: <MenuBookRoundedIcon /> },
    { text: "About", icon: <InfoRoundedIcon /> },
    { text: "Feedback", icon: <HelpRoundedIcon />, guestDisabled: true },
  ];

  return (
    <Stack sx={{ flexGrow: 1, p: 1, justifyContent: "space-between" }}>
      <List dense>
        {mainListItems.map((item, index) => (
          <ListItem key={index} disablePadding sx={{ display: "block" }}>
            <ListItemButton
              selected={isOptionSelected(item.text)}
              onClick={() => handleListItemClick(index, item.text)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <List dense>
        {secondaryListItems.map((item, index) => {
          if (!loggedIn && item.guestDisabled) return null;
          return (
          <ListItem key={index} disablePadding sx={{ display: "block" }}>
            <ListItemButton
              selected={isOptionSelected(item.text)}
                onClick={() => handleListItemClick(index + mainListItems.length, item.text)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        )})}
      </List>
    </Stack>
  );
}
