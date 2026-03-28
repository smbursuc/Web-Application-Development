import * as React from 'react';

import { alpha } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import AppNavbar from './components/AppNavbar';
import Header from './components/Header';
import MainGrid from './components/MainGrid';
import SideMenu from './components/SideMenu';
import AppTheme from '../shared-theme/AppTheme';
import {
  chartsCustomizations,
  dataGridCustomizations,
  datePickersCustomizations,
  treeViewCustomizations,
} from './theme/customizations';
import { useEffect, useState } from "react";
import { checkAuth } from '../utils/checkAuth';
import { useAppState } from '../contexts/AppStateContext';
import { useNavigate } from 'react-router-dom';
import { Alert, Box as LoadingBox, CircularProgress } from "@mui/material";

import DashboardContent from './pages/DashboardContent';

const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

export default function Dashboard(props) {
  const [responseMessage, setResponseMessage] = useState("");
  const [initialized, setInitialized] = useState(false);
  const appStateContext = useAppState();
  const loggedIn = appStateContext.loggedIn;
  const setLoggedIn = appStateContext.setLoggedIn;
  const setUser = appStateContext.setUser; // Add this
  const navigate = useNavigate();

  // first check if user is allowed to enter this page
  useEffect(() => {
    checkAuth(setLoggedIn, setResponseMessage, setInitialized, setUser);
  }, [])

  return (
    
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      {initialized ? (<Box sx={{ display: 'flex' }}>
        <SideMenu />
        <AppNavbar />
        {/* Main content */}
        <Box
          component="main"
          sx={(theme) => ({
            flexGrow: 1,
            backgroundColor: theme.vars
              ? `rgba(${theme.vars.palette.background.defaultChannel} / 1)`
              : alpha(theme.palette.background.default, 1),
            overflow: 'auto',
          })}
        >
          <Stack
            spacing={2}
            sx={{
              alignItems: 'center',
              mx: 3,
              pb: 5,
              mt: { xs: 8, md: 0 },
            }}
          >
            <DashboardContent /> 
          </Stack>
        </Box>
      </Box>) : (
        <LoadingBox
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            gap: 2,
          }}
        >
          <CircularProgress size={48} />
        </LoadingBox>
      )}
    </AppTheme>
  );
}
