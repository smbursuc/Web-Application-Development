import React, { createContext, useState, useContext } from "react";

const AppStateContext = createContext();

export const AppStateProvider = ({ children }) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [loggedIn, setLoggedIn] = useState(false);
  const [user, setUser] = useState({ username: "", email: "" });
  const [aboutOpen, setAboutOpen] = useState(false);
  const [isDirty, setIsDirty] = useState(false); // For unsaved changes protection

  return (
    <AppStateContext.Provider value={{ selectedIndex, setSelectedIndex, loggedIn, setLoggedIn, user, setUser, aboutOpen, setAboutOpen, isDirty, setIsDirty }}>
      {children}
    </AppStateContext.Provider>
  );
};

export const useAppState = () => useContext(AppStateContext);
