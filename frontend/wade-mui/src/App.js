import React from "react";
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Container from "@mui/material/Container";
import Dashboard from "./dashboard/Dashboard";
import SignIn from "./sign-in/SignIn";
import SignUp from "./sign-up/SignUp";
import MarketingPage from "./marketing-page/MarketingPage";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Playground from "./dashboard/pages/Playground";
import { AppStateProvider } from "./contexts/AppStateContext";
import SemanticZoom from "./dashboard/pages/SemanticZoom";
import Correlations from "./dashboard/pages/Correlations";
import GlobalSiteManager from "./common/GlobalSiteManager";
import AboutModal from "./dashboard/components/AboutModal";
import Settings from "./dashboard/pages/Settings";
import Feedback from "./dashboard/pages/Feedback";
import AppTheme from "./shared-theme/AppTheme";
import LandingPage from "./LandingPage";
import HowToUse from "./dashboard/pages/HowToUse";

function App() {
  return (
    <AppStateProvider>
      <AppTheme>
        <GlobalSiteManager />
        <AboutModal />
      </AppTheme>
      <Router>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/home" element={<MarketingPage />} />
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/sparql-playground" element={<Playground />} />
          <Route path="/semantic-zoom" element={<SemanticZoom />} />
          <Route path="/correlations" element={<Correlations />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/feedback" element={<Feedback />} />
          <Route path="/how-to-use" element={<HowToUse />} />
        </Routes>
      </Router>
    </AppStateProvider>
  );
}

export default App;
