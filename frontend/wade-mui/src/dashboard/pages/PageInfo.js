import * as React from "react";
import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import {
  Container,
  Box,
  Typography,
  MenuItem,
  Button,
  Select,
  Skeleton,
  FormControl,
  InputLabel,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Card,
  CardMedia,
  CardContent,
  IconButton,
  Modal,
  FormControlLabel,
  Checkbox,
  Slider,
} from "@mui/material";
import AppNavbar from "../components/AppNavbar";
import Header from "../components/Header";
import MainGrid from "../components/MainGrid";
import SideMenu from "../components/SideMenu";
import AppTheme from "../../shared-theme/AppTheme";
import {
  chartsCustomizations,
  dataGridCustomizations,
  datePickersCustomizations,
  treeViewCustomizations,
} from "../theme/customizations";
import { useState, useEffect, useRef } from "react";
import Plot from "react-plotly.js";
import Grid from "@mui/material/Grid2";
import HelpRoundedIcon from "@mui/icons-material/HelpRounded";
import SpeedIcon from "@mui/icons-material/Speed";
import LockIcon from "@mui/icons-material/Lock";
import TouchAppIcon from "@mui/icons-material/TouchApp";
import SupportAgentIcon from "@mui/icons-material/SupportAgent";
import BuildIcon from "@mui/icons-material/Build";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CircleIcon from "@mui/icons-material/Circle";
import SemanticZoomModal from "./InfoModal";
import FilterComponent from "./FilterComponent";
import Datasets from "../../common/managers/Datasets";
import DataModels from "../../common/managers/DataModels";

export default function PageInfo(props) {
  const datasets = props.datasets;
  const handleInfOpen = props.handleInfOpen;
  const selectedDataset = props.selectedDataset;
  const setSelectedDataset = props.setSelectedDataset;
  const handleDatasetChange = props.handleDatasetChange;
  const dataModel = props?.dataModel;
  const dataModels = props?.dataModels;
  const handleDataModelChange = props?.handleDataModelChange;

  const datasetsObj = new Datasets(datasets);
  function getDataset() {
    const ds = datasetsObj.get();
    if (!ds || ds.length === 0) return null;
    return ds.find((dataset) => dataset.value === selectedDataset);
  }

  const dataModelsObj = new DataModels(dataModels);
  function getDataModels() {
    const dm = dataModelsObj.get();
    if (!dm || Object.keys(dm).length === 0) return [];
    if (!dm[selectedDataset] || dm[selectedDataset].length === 0) return [];
    return dm[selectedDataset];
  }

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        textAlign: "center",
        gap: 2,
        width: "100%",
      }}
    >
      <Typography variant="h4" sx={{ my: 4 }}>
        Dataset Object Viewer
      </Typography>
      <Container
        sx={{
          display: "flex",
          flexDirection: "row",
          alignItems: "center",
          justifyContent: "center",
          gap: 1,
        }}
      >
        <Typography variant="h3">
          {getDataset() ? getDataset().displayValue : ""}
        </Typography>
        <IconButton aria-label="Information" onClick={handleInfOpen}>
          <HelpRoundedIcon />
        </IconButton>
      </Container>
      {datasets && datasets.length > 0 ? (
        <FormControl fullWidth >
          <InputLabel id="dataset-select-label">Select Dataset</InputLabel>
          <Select
            labelId="dataset-select-label"
            id="dataset-select"
            value={selectedDataset}
            onChange={handleDatasetChange}
          >
            {datasets.map((dataset) => (
              <MenuItem key={dataset.value} value={dataset.value}>
                {dataset.displayValue}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      ) : (
        <Skeleton variant="rectangular" width="100%" height={56} />
      )}
      {getDataModels().length > 0 && <FormControl fullWidth >
        <InputLabel id="datamodel-select-label">Select Data Model for Representation</InputLabel>
        <Select
          labelId="datamodel-select-label"
          id="datamodel-select"
          value={dataModel}
          onChange={handleDataModelChange}
        >
          {getDataModels().map((dataModel) => (
            <MenuItem key={dataModel.value} value={dataModel.value}>
              {dataModel.displayValue}
            </MenuItem>
          ))}
        </Select>
      </FormControl>}
    </Box>
  );
}
