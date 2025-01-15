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
import InfoModal from "./InfoModal";
import FilterComponent from "./FilterComponent";
import PageInfo from "./PageInfo";

const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

export default function SemanticZoom(props) {
  const datasets = [
    {
      value: "cifar10",
      displayValue: "CIFAR-10",
    },
  ];

  const features = [
    {
      title: "Description",
      description:
        "The CIFAR-10 and CIFAR-100 datasets are labeled subsets of the 80 million tiny images dataset.",
    },
    {
      title: "Image Size",
      description: "32x32",
    },
    {
      title: "Dataset Size",
      description: "50.000 images, ~177MB",
    },
    {
      title: "Classes",
      description:
        "There are 10 classes: airplane, automobile, bird, cat, deer, dog, frog, horse, ship, truck.",
    },
    {
      title: "Prediction Time",
      description: "~4 hours /w DeepDetect CPU, Ryzen 5 7600x",
    },
    {
      title: "Predictions",
      description:
        "For each image of the dataset the best 3 guesses were stored. This explains why so many images are being categorized by one object, it was because the object was present in the top 3 predictions.",
    },
  ];

  const [selectedDataset, setSelectedDataset] = useState("cifar10"); // Default dataset

  const handleDatasetChange = (event) => {
    setSelectedDataset(event.target.value);
  };

  const [labels, setLabels] = useState([]);
  const [parents, setParents] = useState([]);
  const [uris, setUris] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [values, setValues] = useState([]);
  const [selectedCluster, setSelectedCluster] = useState("");

  const [selectedClusterLabels, setSelectedClusterLabels] = useState([]);
  const [selectedClusterValues, setSelectedClusterValues] = useState([]);
  const [selectedClusterUris, setSelectedClusterUris] = useState([]);

  const [infOpen, setInfOpen] = useState(false);

  const [filterOpen, setFilterOpen] = useState(false);
  const [selectedCheckboxes, setSelectedCheckboxes] = useState([]);
  const [rangeSliderValue, setRangeSliderValue] = useState(5);
  const [rangeStartSliderValue, setRangeStartSliderValue] = useState(1);
  const [sort, setSort] = useState("highest_probability");

  const options = {
    sort: {
      values: ["highest_probability", "lowest_probability", "no_sort"],
      descriptions: ["Highest probability", "Lowest probability", "No sorting"],
    },
  };

  const handleInfOpen = () => {
    setInfOpen(true);
  };

  const handleInfClose = () => {
    setInfOpen(false);
  };

  const fetchClusterData = async () => {
    let queryParams = "";
    if (sort !== "no_sort") {
      queryParams = `sort=${sort}`;
    }

    queryParams =
      queryParams +
      `&range=${rangeSliderValue}&rangeStart=${rangeStartSliderValue}`; // mandatory
    const url =
      `http://127.0.0.1:8081/api/${selectedDataset}/clusters/rdf?` + queryParams;

    try {
      const response = await fetch(url);
      const data = await response.json();
      const result = data.data;

      updateValues(result);
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
    }
  };

  useEffect(() => {
    fetchClusterData();
  }, []);

  const fetchSingleCluster = async (clusterName) => {
    try {
      // encoding is needed because labels contain a whitespace
      const url = `http://127.0.0.1:8081/api/${selectedDataset}/clusters/json?name=${encodeURIComponent(
        clusterName
      )}&sort=probability&range=${rangeSliderValue}&rangeStart=${rangeStartSliderValue}`;
      const response = await fetch(url);
      const data = await response.json();
      const result = data.data;

      setSelectedClusterLabels(result.labels);
      setSelectedClusterValues(result.values);
      setSelectedClusterUris(result.uris);
      // setSelectedCluster(clusterName);

      // updateValues(result);
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
    }
  };

  const updateValues = async (result) => {
    setLabels(result.labels);
    setParents(result.parents);
    setValues(result.values); // Set the values for plotly
    setUris(result.uris);
    setIsLoading(false);
  };

  const handlePlotlyClick = (event) => {
    if (event.points) {
      const clickedCluster = event.points[0].label;
      // ensure a cluster was clicked and not an item
      if (selectedCluster === clickedCluster || !clickedCluster) {
        // this is a deselect
        setSelectedCluster("");
      } else {
        if (clickedCluster.includes("Cluster")) {
          fetchSingleCluster(clickedCluster);
          setSelectedCluster(clickedCluster);
        }
      }
    }
  };

  const handleRangeSliderChange = (event, newValue) => {
    setRangeSliderValue(newValue);
  };

  const handleRangeStartSliderChange = (event, newValue) => {
    setRangeStartSliderValue(newValue);
  };

  const handleSortChange = (event) => {
    setSort(event.target.value);
  };

  const handleApplyFilters = () => {
    fetchClusterData();
  };

  return (
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: "flex" }}>
        <SideMenu />
        <AppNavbar />
        <Container
          sx={{ width: "100%", maxWidth: { sm: "100%", md: "1400px" } }}
        >
          <Grid
            // container {/* Single page application for now... */}
            // spacing={2}
            columns={6}
            sx={{ mb: (theme) => theme.spacing(2), my: 4 }}
          >
            {/* <Grid item size={{ xs: 12, sm: 6, lg: 3 }}> */}{" "}
            {/* Decide whether to leave this page as a two column grid or not... */}
            <Grid>
              <PageInfo
                datasets={datasets}
                handleInfOpen={handleInfOpen}
                selectedDataset={selectedDataset}
                setSelectedDataset={setSelectedDataset}
                handleDatasetChange={handleDatasetChange}
              />
              <FilterComponent
                handleApplyFilters={handleApplyFilters}
                sort={sort}
                setSort={setSort}
                rangeSliderValue={rangeSliderValue}
                setRangeSliderValue={setRangeSliderValue}
                rangeStartSliderValue={rangeStartSliderValue}
                setRangeStartSliderValue={setRangeStartSliderValue}
                options={options}
              />
              {isLoading ? (
                <p>Loading cluster data...</p>
              ) : (
                <Plot
                  data={[
                    {
                      type: "treemap",
                      labels: labels,
                      parents: parents,
                      values: values,
                      textinfo: "label+value+percent parent",
                      hoverinfo: "label+text+percent entry",
                    },
                  ]}
                  layout={{
                    title: "Semantic Zoom Tree Map",
                    margin: { t: 50, l: 25, r: 25, b: 25 },
                    uirevision: "true", // this prevents a re-render of the plot that is not user generated
                  }}
                  onClick={(event) => {
                    handlePlotlyClick(event);
                  }}
                  style={{ width: "100%", height: "500px" }}
                  uirevision="true"
                />
              )}
              <Typography variant="h3" sx={{ my: 4 }}>
                {selectedCluster && !isLoading
                  ? selectedCluster
                  : "Select a cluster to see its details"}
              </Typography>
              {selectedCluster && !isLoading && (
                <Grid container spacing={2} alignItems="stretch">
                  {selectedClusterLabels.map((label, index) => {
                    if (!label.includes("Cluster"))
                      return (
                        <Grid item xs={12} sm={6} md={4} lg={3} key={index}>
                          <Card style={{ height: "100%" }}>
                            <CardMedia
                              component="img"
                              height="140"
                              image={selectedClusterUris[index]}
                              alt={label}
                            />
                            <CardContent>
                              <Typography variant="h6" align="center">
                                {label}
                              </Typography>
                              <Typography
                                variant="body2"
                                align="center"
                                color="textSecondary"
                              >
                                Confidence:{" "}
                                {(selectedClusterValues[index] * 100).toFixed(
                                  2
                                )}
                                %
                              </Typography>
                            </CardContent>
                          </Card>
                        </Grid>
                      );
                  })}
                </Grid>
              )}
            </Grid>
            {/* <Grid item size={{ xs: 12, sm: 6, lg: 3 }}>
              <Typography
                variant="h4"
                sx={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: { xs: "flex-start", sm: "center" },
                  textAlign: "center",
                  my: 4,
                }}
              >
                Dataset Details: {selectedDataset}
              </Typography>
            </Grid> */}
            <InfoModal
              infOpen={infOpen}
              handleInfClose={handleInfClose}
              features={features}
            />
          </Grid>
        </Container>
      </Box>
    </AppTheme>
  );
}
