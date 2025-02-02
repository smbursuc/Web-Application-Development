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

export default function PageInfo(props) {
  const datasets = props.datasets;
  const handleInfOpen = props.handleInfOpen;
  const selectedDataset = props.selectedDataset;
  const setSelectedDataset = props.setSelectedDataset;
  const handleDatasetChange = props.handleDatasetChange;
  const dataModel = props?.dataModel;
  const dataModels = props?.dataModels;
  const handleDataModelChange = props?.handleDataModelChange;

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
          {
            datasets.find((dataset) => dataset.value === selectedDataset)
              .displayValue
          }
        </Typography>
        <IconButton aria-label="Information" onClick={handleInfOpen}>
          <HelpRoundedIcon />
        </IconButton>
      </Container>
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
      {dataModels && <FormControl fullWidth >
        <InputLabel id="datamodel-select-label">Select Data Model for Representation</InputLabel>
        <Select
          labelId="datamodel-select-label"
          id="datamodel-select"
          value={dataModel}
          onChange={handleDataModelChange}
        >
          {dataModels.map((dataModel) => (
            <MenuItem key={dataModel.value} value={dataModel.value}>
              {dataModel.displayValue}
            </MenuItem>
          ))}
        </Select>
      </FormControl>}
    </Box>
  );
}
