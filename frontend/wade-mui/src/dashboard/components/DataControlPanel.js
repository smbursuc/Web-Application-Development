import * as React from "react";
import {
  Box, Button, FormControlLabel, Checkbox,
  Dialog, DialogTitle, DialogContent, DialogActions, Typography,
} from "@mui/material";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { useAppState } from "../../contexts/AppStateContext";
import { useNavigate } from "react-router-dom";

export default function DataControlPanel(props) {
  const {
    onCreate,
    onAdd,
    onUpdate,
    onDelete,
    onExport,
    isDefaultDataset = false,
  } = props;

  const { loggedIn } = useAppState();
  const navigate = useNavigate();
  const [honorFilters, setHonorFilters] = React.useState(true);
  const [exportModalOpen, setExportModalOpen] = React.useState(false);
  const [guestModalOpen, setGuestModalOpen] = React.useState(false);

  const handleConfirmExport = () => {
    onExport(honorFilters);
    setExportModalOpen(false);
  };

  const handleCreateClick = () => {
    if (!loggedIn) {
      setGuestModalOpen(true);
    } else {
      onCreate();
    }
  };

  return (
    <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap", my: 2, alignItems: "center" }}>
      <Button variant="contained" onClick={handleCreateClick} sx={{ my: 2 }}>
        Create Dataset
      </Button>
      {!isDefaultDataset && (
        <Button variant="contained" onClick={onAdd} sx={{ my: 2 }}>
          Add
        </Button>
      )}
      {!isDefaultDataset && (
        <Button variant="contained" onClick={onUpdate} sx={{ my: 2 }}>
          Update
        </Button>
      )}
      {!isDefaultDataset && (
        <Button variant="contained" onClick={onDelete} sx={{ my: 2 }}>
          Delete
        </Button>
      )}
      <Button
        variant="contained"
        startIcon={<FileDownloadIcon />}
        onClick={() => setExportModalOpen(true)}
        sx={{ my: 2 }}
      >
        Export
      </Button>

      <Dialog open={exportModalOpen} onClose={() => setExportModalOpen(false)}>
        <DialogTitle>Export Dataset</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Download the current dataset as a JSON file.
          </Typography>
          <FormControlLabel
            control={
              <Checkbox
                checked={honorFilters}
                onChange={(e) => setHonorFilters(e.target.checked)}
              />
            }
            label="Honor current filters"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportModalOpen(false)}>Cancel</Button>
          <Button variant="contained" startIcon={<FileDownloadIcon />} onClick={handleConfirmExport}>
            Confirm Export
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={guestModalOpen} onClose={() => setGuestModalOpen(false)}>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <LockOutlinedIcon color="warning" />
          Registered Users Only
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Creating datasets is only available to registered users. Sign up or log in to get started.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGuestModalOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => { setGuestModalOpen(false); navigate('/'); }}>
            Go to Sign In
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
