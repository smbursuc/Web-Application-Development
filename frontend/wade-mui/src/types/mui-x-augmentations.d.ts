declare module '@mui/x-data-grid-pro/themeAugmentation' {
  import { Components } from '@mui/material/styles';
  export type DataGridProComponents<T = any> = Components<T>;
}

declare module '@mui/x-data-grid/themeAugmentation' {
  import { Components } from '@mui/material/styles';
  export type DataGridComponents<T = any> = Components<T>;
}

declare module '@mui/x-date-pickers-pro/themeAugmentation' {
  import { Components } from '@mui/material/styles';
  export type PickersProComponents<T = any> = Components<T>;
}

declare module '@mui/x-date-pickers/themeAugmentation' {
  import { Components } from '@mui/material/styles';
  export type PickerComponents<T = any> = Components<T>;
}

export {};
