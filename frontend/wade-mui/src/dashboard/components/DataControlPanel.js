import * as React from "react";
import { Box, Button } from "@mui/material";

export default function DataControlPanel(props) {
  const {
    onCreate,
    onAdd,
    onUpdate,
    onDelete,
    onExport,
  } = props;

  return (
    <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", my: 2 }}>
      <Button variant="contained" onClick={onCreate} sx={{ my: 2 }}>
        Create
      </Button>
      <Button variant="contained" onClick={onAdd} sx={{ my: 2 }}>
        Add
      </Button>
      <Button variant="contained" onClick={onUpdate} sx={{ my: 2 }}>
        Update
      </Button>
      <Button variant="contained" onClick={onDelete} sx={{ my: 2 }}>
        Delete
      </Button>
      <Button variant="contained" onClick={onExport} sx={{ my: 2 }}>
        Export
      </Button>
    </Box>
  );
}
