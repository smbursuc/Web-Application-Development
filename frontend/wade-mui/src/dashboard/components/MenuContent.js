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
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAppState } from "../../contexts/AppStateContext";

export default function MenuContent() {
  const navigate = useNavigate();
  const appStateProps = useAppState();
  const selectedIndex = appStateProps.selectedIndex;
  const setSelectedIndex = appStateProps.setSelectedIndex;

  const handleListItemClick = (index, option) => {
    setSelectedIndex(index); 
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
    }
  };

  const mainListItems = [
    { text: "Home", icon: <HomeRoundedIcon /> },
    { text: "SPARQL Playground", icon: <AnalyticsRoundedIcon /> },
    { text: "Semantic Zoom", icon: <PeopleRoundedIcon /> },
    { text: "Correlations", icon: <AssignmentRoundedIcon /> },
  ];

  const secondaryListItems = [
    { text: "Settings", icon: <SettingsRoundedIcon /> },
    { text: "About", icon: <InfoRoundedIcon /> },
    { text: "Feedback", icon: <HelpRoundedIcon /> },
  ];

  return (
    <Stack sx={{ flexGrow: 1, p: 1, justifyContent: "space-between" }}>
      <List dense>
        {mainListItems.map((item, index) => (
          <ListItem
            key={index}
            disablePadding
            sx={{ display: "block" }}
            onClick={() => handleListItemClick(index, item.text)}
          >
            <ListItemButton selected={index === selectedIndex}>
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <List dense>
        {secondaryListItems.map((item, index) => (
          <ListItem key={index} disablePadding sx={{ display: "block" }}>
            <ListItemButton>
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Stack>
  );
}
