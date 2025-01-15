import React, { createContext, useState, useContext } from "react";

const AppStateContext = createContext();

export const AppStateProvider = ({ children }) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [loggedIn, setLoggedIn] = useState(false);
  // const [selectedDataset, setSelectedDataset] = useState("Dataset 1");

  return (
    <AppStateContext.Provider value={{ selectedIndex, setSelectedIndex, loggedIn, setLoggedIn }}>
      {children}
    </AppStateContext.Provider>
  );
};

export const useAppState = () => useContext(AppStateContext);
