import * as React from "react";
import {
  Box,
  Typography,
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Modal,
  TextField,
  CircularProgress,
  Alert,
} from "@mui/material";
import CircleIcon from "@mui/icons-material/Circle";
import StaticMetadata from "../../common/managers/StaticMetadata";
import SelectedDataset from "../../common/managers/SelectedDataset";
import API_BASE_URL from "../../config";

const DEFAULT_DATASETS = ["bsds300", "cifar10"];

export default function InfoModal(props) {
  const infOpen = props.infOpen;
  const handleInfClose = props.handleInfClose;
  const selectedDatasetObj = new SelectedDataset(props.selectedDataset);
  const staticMetadataObj = new StaticMetadata(props.staticMetadata);

  const isDefault = DEFAULT_DATASETS.includes(selectedDatasetObj.get());

  // Editable metadata state (for dynamic datasets)
  const [displayValue, setDisplayValue] = React.useState("");
  const [summary, setSummary] = React.useState("");
  const [source, setSource] = React.useState("");
  const [editMode, setEditMode] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [saveStatus, setSaveStatus] = React.useState("");

  // Load persisted metadata when modal opens for a dynamic dataset
  React.useEffect(() => {
    if (!infOpen || isDefault || !selectedDatasetObj.get()) return;
    setLoading(true);
    setSaveStatus("");
    setEditMode(false);
    fetch(`${API_BASE_URL}/api/dataset-metadata/${selectedDatasetObj.get()}`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((data) => {
        const d = data.data || {};
        const dv = d.displayValue || "";
        const sm = d.summary || "";
        const src = d.source || "";
        setDisplayValue(dv);
        setSummary(sm);
        setSource(src);
        // Start in edit mode if there is no saved data yet
        if (!dv && !sm && !src) setEditMode(true);
      })
      .catch(() => { setEditMode(true); })
      .finally(() => setLoading(false));
  }, [infOpen, props.selectedDataset]);

  const handleSave = async () => {
    setSaving(true);
    setSaveStatus("");
    try {
      const resp = await fetch(
        `${API_BASE_URL}/api/dataset-metadata/${selectedDatasetObj.get()}`,
        {
          method: "PUT",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ displayValue, summary, source }),
        }
      );
      if (!resp.ok) throw new Error(`Save failed: ${resp.statusText}`);
      setSaveStatus("success");
      setEditMode(false);
    } catch (e) {
      setSaveStatus("error");
    } finally {
      setSaving(false);
    }
  };

  function getFeatures() {
    const sd = selectedDatasetObj.get();
    return staticMetadataObj.getFeatures(sd || "");
  }

  function getDisplayValue() {
    const sd = selectedDatasetObj.get();
    return staticMetadataObj.getDisplayValue(sd || "") || sd || "";
  }

  function getSummary() {
    const sd = selectedDatasetObj.get();
    return staticMetadataObj.getSummary(sd || "") || "";
  }

  function getSource() {
    const sd = selectedDatasetObj.get();
    return staticMetadataObj.getSource(sd || "") || "";
  }

  return (
    <Modal
      open={infOpen}
      onClose={handleInfClose}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
    >
      <Box
        sx={{
          position: "absolute",
          top: "50%",
          left: "50%",
          transform: "translate(-50%, -50%)",
          bgcolor: "background.paper",
          border: "2px solid #000",
          boxShadow: 24,
          p: { xs: 2, sm: 4 },
          borderRadius: 2,
          width: { xs: "92vw", sm: 560 },
          maxHeight: "80vh",
          overflowY: "auto",
        }}
      >
        {isDefault ? (
          /* ── Default dataset: read-only view ── */
          <>
            <Typography id="modal-title" variant="h6" component="h2">
              {getDisplayValue()}
            </Typography>
            <Typography id="modal-description" sx={{ mt: 2 }}>
              {getSummary()}&nbsp;
              <a
                href={getSource()}
                target="_blank"
                rel="noopener noreferrer"
                style={{ textDecoration: "none" }}
              >
                Source
              </a>
            </Typography>
            <List>
              {getFeatures().map((feature, index) => (
                <ListItem key={index} alignItems="flex-start" sx={{ gap: 1 }}>
                  <ListItemIcon>
                    <CircleIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary={feature.title}
                    secondary={
                      <Typography variant="body2" color="text.secondary">
                        {feature.description.split(":").length > 1
                          ? feature.description.split(":")[0] + ":"
                          : feature.description.split(":")}
                        <Typography
                          component="span"
                          variant="body2"
                          fontWeight="bold"
                          color="text.primary"
                        >
                          {feature.description.split(":")[1]}
                        </Typography>
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </>
        ) : (
          /* ── Dynamic dataset: editable metadata ── */
          <>
            <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
              Dataset Info — {selectedDatasetObj.get()}
            </Typography>
            {loading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress />
              </Box>
            ) : !editMode ? (
              /* ── View mode: show saved values ── */
              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                {displayValue && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Display Name</Typography>
                    <Typography variant="body1">{displayValue}</Typography>
                  </Box>
                )}
                {summary && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Summary</Typography>
                    <Typography variant="body1">{summary}</Typography>
                  </Box>
                )}
                {source && (
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">Source URL</Typography>
                    <Typography variant="body1">
                      <a href={source} target="_blank" rel="noopener noreferrer">
                        {source}
                      </a>
                    </Typography>
                  </Box>
                )}
                {saveStatus === "success" && (
                  <Alert severity="success">Saved successfully.</Alert>
                )}
                <Button variant="outlined" onClick={() => { setSaveStatus(""); setEditMode(true); }}>
                  Edit
                </Button>
              </Box>
            ) : (
              /* ── Edit mode: show text fields ── */
              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                <TextField
                  label="Summary"
                  value={summary}
                  onChange={(e) => setSummary(e.target.value)}
                  fullWidth
                  multiline
                  minRows={8}
                  maxRows={12}
                  sx={{
                    "& .MuiOutlinedInput-root": {
                      alignItems: "flex-start",
                    },
                    "& .MuiInputBase-input": {
                      overflowY: "auto",
                      resize: "vertical",
                    },
                  }}
                />
                <TextField
                  label="Source URL"
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  fullWidth
                  helperText="Optional: leave empty if no external source"
                />
                {saveStatus === "error" && (
                  <Alert severity="error">Failed to save. Please try again.</Alert>
                )}
                <Box sx={{ display: "flex", gap: 1 }}>
                  <Button
                    variant="contained"
                    onClick={handleSave}
                    disabled={saving}
                  >
                    {saving ? "Saving…" : "Save"}
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => { setSaveStatus(""); setEditMode(false); }}
                    disabled={saving}
                  >
                    Cancel
                  </Button>
                </Box>
              </Box>
            )}
          </>
        )}
        <Button onClick={handleInfClose} variant="outlined" sx={{ mt: 2 }}>
          Close
        </Button>
      </Box>
    </Modal>
  );
}
