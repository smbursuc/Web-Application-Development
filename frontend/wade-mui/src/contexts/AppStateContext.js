import React, { createContext, useState, useContext } from "react";

const AppStateContext = createContext();

export const AppStateProvider = ({ children }) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [loggedIn, setLoggedIn] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  // const [selectedDataset, setSelectedDataset] = useState("Dataset 1");

  return (
    <AppStateContext.Provider value={{ selectedIndex, setSelectedIndex, loggedIn, setLoggedIn, username, setUsername, email, setEmail }}>
      {children}
    </AppStateContext.Provider>
  );
};

export const useAppState = () => useContext(AppStateContext);
