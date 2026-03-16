import '@mui/material/styles';

declare module '@mui/material/styles' {
  interface Theme {
    // optional CSS variable backed theme values (used by some MUI setups)
    vars?: Record<string, any>;
  }
  interface ThemeOptions {
    vars?: Record<string, any>;
  }
}

export {};
