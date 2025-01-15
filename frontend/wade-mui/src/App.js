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

function App() {
  return (
    <AppStateProvider>
      <Router>
        <Routes>
          {/* Define your routes */}
          <Route path="/" element={<MarketingPage />} />
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/sparql-playground" element={<Playground />} />
          <Route path="/semantic-zoom" element={<SemanticZoom />} />
          <Route path="/correlations" element={<Correlations />} />
        </Routes>
      </Router>
    </AppStateProvider>
  );
}

export default App;
