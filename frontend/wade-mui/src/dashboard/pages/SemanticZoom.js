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
import Groq from "groq-sdk";

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
    {
      value: "bsds300",
      displayValue: "BSDS300",
    },
  ];

  const [selectedDataset, setSelectedDataset] = useState("bsds300"); // Default dataset

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
  const [dataModel, setDataModel] = useState("rdf");

  const [selectedClusterLabel, setSelectedClusterLabel] = useState("");

  const [maxClusters, setMaxClusters] = useState(() => {
    return getFromLocalStorage("maxClusters") || {};
  });

  const handleDataModelChange = (event) => {
    setDataModel(event.target.value);
  };

  const dataModels = [
    {
      value: "rdf",
      displayValue: "RDF",
    },
    {
      value: "json",
      displayValue: "JSON",
    },
  ];

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

  const fetchClusterData = async (resetParams) => {
    if (resetParams) {
      setRangeSliderValue(5);
      setRangeStartSliderValue(1);
    }

    let queryParams = "";
    if (sort !== "no_sort") {
      queryParams = `sort=${sort}`;
    }

    queryParams =
      queryParams +
      `&range=${rangeSliderValue}&rangeStart=${rangeStartSliderValue}`; // mandatory
    const url =
      `http://127.0.0.1:8081/api/${selectedDataset}/clusters/${dataModel}?` +
      queryParams;

    try {
      const response = await fetch(url);
      const data = await response.json();
      const result = data.data;

      await updateValues(result);
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
    }
  };

  useEffect(() => {
    const fetchData = async () => {
      // setIsLoading(true);                            this unfortunately breaks the plot ! the plot re-renders and loses its event handler for the onclick
      await fetchClusterData(true);
      let cache = getFromLocalStorage("maxClusters");
      if (
        !cache ||
        !cache[selectedDataset] ||
        !cache[selectedDataset][dataModel]
      ) {
        fetchMetadata();
      } else {
        setMaxClusters(cache);
      }
      setIsLoading(false);
    };
    fetchData();
  }, [selectedDataset, dataModel]);

  function saveToLocalStorage(key, data) {
    localStorage.setItem(key, JSON.stringify(data));
  }

  function getFromLocalStorage(key) {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  }

  const fetchMetadata = async () => {
    const url = `http://127.0.0.1:8081/api/${selectedDataset}/metadata/${dataModel}/clusters`;
    const response = await fetch(url);
    const data = await response.json();
    const result = data.data;

    // load the metadata cache, update it, and save it back
    const cache = getFromLocalStorage("maxClusters") || {};
    cache[selectedDataset] = {
      ...(cache[selectedDataset] || {}), // safeguard for undefined dataset
      [dataModel]: result.size,
    };

    saveToLocalStorage("maxClusters", cache);
    setMaxClusters(cache);
    setIsLoading(false);
  };

  const fetchSingleCluster = async (clusterName) => {
    try {
      // encoding is needed because labels contain a whitespace
      const url = `http://127.0.0.1:8081/api/${selectedDataset}/clusters/${dataModel}?name=${encodeURIComponent(
        clusterName
      )}`;
      const response = await fetch(url);
      const data = await response.json();
      const result = data.data;

      await updateValuesSingle(result);
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
  };

  const updateValuesSingle = async (result) => {
    setSelectedClusterLabels(result.labels);
    setSelectedClusterValues(result.values);
    setSelectedClusterUris(result.uris);
  };

  const handlePlotlyClick = (event) => {
    if (event.points && !isLoading) {
      const clickedCluster = event.points[0].label;
      // ensure a cluster was clicked and not an item
      if (selectedCluster === clickedCluster || !clickedCluster) {
        // this is a deselect
        setSelectedCluster("");
        setSelectedClusterLabel("");
      } else {
        if (clickedCluster.includes("Cluster")) {
          setIsLoading(true);
          fetchSingleCluster(clickedCluster);
          setSelectedCluster(clickedCluster);
          setIsLoading(false);
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
    setIsLoading(true);
    fetchClusterData(false);
    setIsLoading(false);
  };

  const groq = new Groq({ apiKey: "your_api_key", dangerouslyAllowBrowser: true });

  async function getGroqChatCompletion(prompt) {
    return groq.chat.completions.create({
      messages: [
        {
          role: "user",
          content: prompt,
        },
      ],
      model: "llama-3.3-70b-versatile",
    });
  }

  const handleLabelCluster = () => {
    const sendPrompt = async () => {
      let promptQuery;
      promptQuery = "You will receive a list of objects and your task is to assign a cluster label for these objects.\n";
      promptQuery += "Reply with only one word (at most 2-3) with the fitting cluster name and nothing else.\n";
      promptQuery += "The list of objects is:\n";
      promptQuery += JSON.stringify(selectedClusterLabels);

      try
      {
        const chatCompletion = await getGroqChatCompletion(promptQuery);
        const content = chatCompletion.choices[0]?.message?.content || "";
        setSelectedClusterLabel(content);
      }
      catch (error)
      {
        // this is a non vital feature of the application, just ignore if user didnt provide an API key
      }
      
    }
    sendPrompt();
  }

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
                dataModels={dataModels}
                dataModel={dataModel}
                handleDataModelChange={handleDataModelChange}
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
                max={
                  getFromLocalStorage("maxClusters") &&
                  maxClusters[selectedDataset] &&
                  maxClusters[selectedDataset][dataModel]
                    ? maxClusters[selectedDataset][dataModel]
                    : 100
                }
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
                <Grid>
                  <Button
                    variant="contained"
                    sx={{ width: "100%" }}
                    onClick={handleLabelCluster}
                  >
                    Label this cluster
                  </Button>
                  {selectedClusterLabel && (
                    <Typography variant="h4" sx={{ my: 2 }}>
                      The detected label is:{" "}
                      <span style={{ color: "red" }}>{selectedClusterLabel}</span>
                    </Typography>
                  )}
                  <Grid container spacing={2} alignItems="stretch">
                    {selectedClusterValues.map((value, index) => {
                      if (!selectedClusterLabels[index].includes("Cluster"))
                        return (
                          <Grid
                            item="true"
                            xs={12}
                            sm={6}
                            md={4}
                            lg={3}
                            key={index}
                          >
                            <Card style={{ height: "100%" }}>
                              <CardMedia
                                component="img"
                                height="140"
                                image={selectedClusterUris[index].replace(
                                  "host.docker.internal",
                                  "localhost"
                                )}
                                alt={selectedClusterLabels[index + 1]}
                              />
                              <CardContent>
                                <Typography variant="h6" align="center">
                                  {selectedClusterLabels[index]}
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
              selectedDataset={selectedDataset}
            />
          </Grid>
        </Container>
      </Box>
    </AppTheme>
  );
}
