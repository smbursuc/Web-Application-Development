import * as React from "react";
import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import API_BASE_URL from '../../config';
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
import { chartsCustomizations,
  dataGridCustomizations,
  datePickersCustomizations,
  treeViewCustomizations,
} from "../theme/customizations";
import { useState, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
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
import Alert from "@mui/material/Alert";
import CircularProgress from "@mui/material/CircularProgress";
import DataControlPanel from "../components/DataControlPanel";
import CreateDatasetForm from "../components/CreateDatasetForm";
import ClusterForm from "../components/ClusterForm";
import { makeDataControlHandlers } from "../../common/DataControlHandlers";
import { max, range } from "d3";
import DatasetCommons from "../../common/DatasetCommons";
import MaxCache from "../../common/managers/MaxCache";
const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

export default function SemanticZoom(props) {
  const location = useLocation();
  const [datasets, setDatasets] = useState([]);
  const [selectedDataset, setSelectedDataset] = useState("");

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
  const [types, setTypes] = useState([]);
  const [selectedClusterTypes, setSelectedClusterTypes] = useState([]);
  const [plotReady, setPlotReady] = useState(false);

  const [infOpen, setInfOpen] = useState(false);

  const [rangeSliderValue, setRangeSliderValue] = useState(5);
  const [rangeStartSliderValue, setRangeStartSliderValue] = useState(0);
  const [sort, setSort] = useState("");
  const [dataModel, setDataModel] = useState("json");

  // Deep linking logic
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const ds = params.get("dataset");
    const dt = params.get("dataType");
    const sortParam = params.get("sort");
    const rangeParam = params.get("range");
    const rangeStartParam = params.get("rangeStart");
    if (ds) setSelectedDataset(ds);
    if (dt) setDataModel(dt);
    if (sortParam) setSort(sortParam);
    if (rangeParam && !Number.isNaN(Number(rangeParam))) setRangeSliderValue(Number(rangeParam));
    if (rangeStartParam && !Number.isNaN(Number(rangeStartParam))) setRangeStartSliderValue(Number(rangeStartParam));
  }, [location.search]);

  const [responseStatus, setResponseStatus] = useState("");

  // will adapt dynamically using the max possible range and the current start value
  const [maxRange, setMaxRange] = useState(0);
  // server-supplied MAX_RANGE constant; defaults to 100 until first metadata fetch
  const [maxRangeConst, setMaxRangeConst] = useState(100);
  const [staticMetadata, setStaticMetadata] = useState({});

  const [maxClusters, setMaxClusters] = useState({});

  const maxClustersObj = new MaxCache(maxClusters, setMaxClusters);

  const handleDataModelChange = (event) => {
    setDataModel(event.target.value);
  };

  const [dataModels, setDataModels] = useState({});
  const [sortOptions, setSortOptions] = useState({});

  const [controlOpen, setControlOpen] = useState(false);
  const [controlMode, setControlMode] = useState("");
  const [formInitial, setFormInitial] = useState({});
  const [controlKey, setControlKey] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);

  // max possible range
  const [max, setMax] = useState(0);

  // follow the order of DatasetCommons
  //   return {
  //   fetchMetadata,
  //   selectedDataset,
  //   dataModel,
  //   datasets,
  //   dataModels,
  //   staticMetadata,
  //   isLoading,
  //   responseStatus,
  // };

  const handleInfOpen = () => {
    setInfOpen(true);
  };

  const openControlModal = (mode, initial = {}) => {
    setControlMode(mode);
    setFormInitial(initial);
    setControlOpen(true);
    // make sure to remount the dialog each time, looks better this way?
    setControlKey((k) => k + 1);
  };

  // handlers will be initialized after DatasetCommons so fetchClusterData is available

  const handleInfClose = () => {
    setInfOpen(false);
  };

  const handleFormCancel = () => {
    setControlOpen(false);
    setFormInitial({});
  };

  const handleCreateCancel = () => setCreateOpen(false);

  const fetchSingleCluster = async (clusterName) => {
    try {
      // encoding is needed because labels contain a whitespace
      const url = `${API_BASE_URL}/api/${selectedDataset}/clusters/${dataModel}?mode=cluster&clusterName=${encodeURIComponent(
        clusterName
      )}`;
      const response = await fetch(url, { credentials: "include" });
      const data = await response.json();
      const status = data.status;
      if (status !== "success") {
        setResponseStatus(`${data.message}`);
        return;
      }
      const result = data.data;
      updateValues(result, true);
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
    }
  };

  const updateValues = async (result, singleCluster) => {
    // If the backend returned a hierarchical structure with `children`,
    // use that representation for both single-cluster and full listings.
    if (result && Array.isArray(result.children) && result.children.length >= 0) {
      const rootName = result.name || "root";
      const labels = [rootName];
      const parents = [""];
      const values = [0];
      const uris = [""];
      const typesLocal = ["cluster"]; // root is a cluster

      const children = result.children || [];
      for (const child of children) {
        // add the child node (keep arrays aligned)
        labels.push(child.name);
        parents.push(rootName);
        // Give every cluster a small fixed value so empty clusters remain visible in
        // the treemap, while keeping the value well below any real leaf probability so
        // leaf sub-tiles always have a noticeably larger proportional area than the
        // cluster's own "header" band (avoids the 50/50 split when leaf prob == 1).
        values.push(0.1);
        uris.push("");
        typesLocal.push("cluster");

        // if the child has grandchildren, add them as leaves
        if (child.children && child.children.length !== 0) {
          for (const grandChild of child.children) {
            labels.push(grandChild.name);
            parents.push(child.name);
            values.push(grandChild.Probability ?? 0);
            // JSON field is serialised as "URI" (capital) per @JsonProperty("URI") on ClusterNode
            uris.push(grandChild.URI || grandChild.uri || "");
            typesLocal.push("leaf");
          }
        }
      }

      if (singleCluster) {
        setSelectedClusterLabels(labels);
        setSelectedClusterValues(values);
        setSelectedClusterUris(uris);
        setSelectedClusterTypes(typesLocal);
      } else {
        // populate Plotly arrays
        setLabels(labels);
        setParents(parents);
        const mappedValues = values.map((v) => (v === 0 ? 1e-9 : v));
        setValues(mappedValues);
        setUris(uris);
        setTypes(typesLocal);

        // also build selected-cluster cards from the hierarchical arrays
        const selIdx = labels.indexOf(selectedCluster);
        const selectedClusterValues = [];
        const selectedClusterLabels = [];
        const selectedClusterUris = [];
        const selectedClusterTypes = [];
        if (selIdx >= 0) {
          for (let i = selIdx + 1; i < labels.length; i++) {
            selectedClusterValues.push(values[i]);
            selectedClusterLabels.push(labels[i]);
            selectedClusterUris.push(uris[i]);
            selectedClusterTypes.push(typesLocal[i]);
          }
        }
        setSelectedClusterValues(selectedClusterValues);
        setSelectedClusterLabels(selectedClusterLabels);
        setSelectedClusterUris(selectedClusterUris);
        setSelectedClusterTypes(selectedClusterTypes);
      }
      return;
    }

    // Fallback: flat result processing (from DatasetCommons) when no `children`
    const flatValues = [];
    const flatLabels = [];
    const flatUris = [];
    const flatTypes = [];
    const flatParents = [];

    if (result && result.data && result.data[0]) {
      const d = result.data[0].result;
      d.forEach((row) => {
        flatLabels.push(row.Object);
        flatValues.push(row.Probability === 0 ? 1e-9 : row.Probability);
        flatUris.push(row.URI || "");
        flatTypes.push(row.Object && row.Object.includes("Cluster") ? "cluster" : "leaf");
        flatParents.push(row.Parent || "");
      });
    }

    setValues(flatValues);
    setLabels(flatLabels);
    setUris(flatUris);
    setTypes(flatTypes);
    setParents(flatParents);

    // now build selected cluster cards from the flat arrays
    const selectedIndex = flatLabels.indexOf(selectedCluster);
    const selectedClusterValues = [];
    const selectedClusterLabels = [];
    const selectedClusterUris = [];
    const selectedClusterTypes = [];

    if (selectedIndex >= 0) {
      for (let i = selectedIndex + 1; i < flatLabels.length; i++) {
        selectedClusterValues.push(flatValues[i]);
        selectedClusterLabels.push(flatLabels[i]);
        selectedClusterUris.push(flatUris[i]);
        selectedClusterTypes.push(flatTypes[i]);
      }
    }

    setSelectedClusterValues(selectedClusterValues);
    setSelectedClusterLabels(selectedClusterLabels);
    setSelectedClusterUris(selectedClusterUris);
    setSelectedClusterTypes(selectedClusterTypes);
  };

  const plotRef = useRef();
  const handlePlotlyClick = (eventData) => {
    if (!eventData || !eventData.points || eventData.points.length === 0) return;
    const pt = eventData.points[0];
    const idx = pt.pointNumber;
    const nodeType = pt.data && pt.data.customdata ? pt.data.customdata[idx] : pt.label && pt.label.includes("Cluster") ? "cluster" : "leaf";
    const label = pt.label || (pt.data && pt.data.labels && pt.data.labels[idx]);

    if (!label) return;

    // Clicking the root node (parent is "") should always clear the selection
    if (parents[idx] === "") {
      setSelectedCluster("");
      setSelectedClusterLabels([]);
      setSelectedClusterValues([]);
      setSelectedClusterUris([]);
      setSelectedClusterTypes([]);
      return;
    }

    // Toggle selection if clicking the already-selected cluster
    if (selectedCluster === label) {
      setSelectedCluster("");
      setSelectedClusterLabels([]);
      setSelectedClusterValues([]);
      setSelectedClusterUris([]);
      setSelectedClusterTypes([]);
      return;
    }

    if (nodeType === "cluster") {
      setSelectedCluster(label);
      fetchSingleCluster(label);
    } else {
      setSelectedCluster(label);
      // For leaf nodes, find the URI from the main arrays and populate the detail panel directly
      const leafIdx = labels.indexOf(label);
      if (leafIdx !== -1) {
        setSelectedClusterLabels([label]);
        setSelectedClusterValues([values[leafIdx]]);
        setSelectedClusterUris([uris[leafIdx]]);
        setSelectedClusterTypes(["leaf"]);
      } else {
        setSelectedClusterLabels([]);
        setSelectedClusterValues([]);
        setSelectedClusterUris([]);
        setSelectedClusterTypes([]);
      }
    }
  };

  const handlePlotUpdate = (figure, graphDiv) => {
    if (graphDiv && !plotReady) {
      setPlotReady(true);
    }

    // Re-bind Plotly events on every update
    if (graphDiv) {
      graphDiv.removeAllListeners("plotly_click");
      graphDiv.on("plotly_click", (eventData) => {
        console.log("Plotly click captured:", eventData);
        handlePlotlyClick(eventData);
      });
    }
  };

  const handlePlotInitialized = (figure, graphDiv) => {
    plotRef.current = graphDiv;
    handlePlotUpdate(figure, graphDiv);
  };

  useEffect(() => {
    return () => {
      if (
        plotRef.current &&
        typeof plotRef.current.removeAllListeners === "function"
      ) {
        plotRef.current.removeAllListeners("plotly_click");
      }
    };
  }, []);

  const {
    fetchMetadata,
    fetchClusterData,
    fetchData,
    getFromLocalStorage,
    saveToLocalStorage,
  } = DatasetCommons({
    selectedDataset,
    dataModel,
    setDatasets,
    datasets,
    setSelectedDataset,
    setDataModel,
    dataModels,
    setDataModels,
    staticMetadata,
    setStaticMetadata,
    setIsLoading,
    setResponseStatus,
    datasetType: "clusters",
    setMaxClusters,
    rangeSliderValue,
    setRangeSliderValue,
    rangeStartSliderValue,
    setRangeStartSliderValue,
    sort,
    setSort,
    updateValues,
    sortOptions,
    setSortOptions,
    setMax,
    setMaxRangeConst,
  });

  // initialize handlers now that fetchClusterData is available
  const {
    handleCreate,
    handleAdd,
    handleUpdate,
    handleDelete,
    handleExport,
    handleFormSubmit,
    handleCreateSubmit,
  } = makeDataControlHandlers({
    selectedDataset,
    setSelectedDataset,
    dataModel,
    setDataModel,
    setDatasets,
    datasetType: "clusters",
    openControlModal,
    setCreateOpen,
    setControlOpen,
    setResponseStatus,
    refreshFn: fetchClusterData,
    fetchMetadata,
    sort,
    rangeSliderValue,
    rangeStartSliderValue,
  });

  const getMaxClusters = () => {
    const cache = getFromLocalStorage("maxClusters");
    // console.log(dataModel, selectedDataset, cache, cache?.[selectedDataset]?.[dataModel]);
    return cache?.[selectedDataset]?.[dataModel] || 10; // fallback value
  };

  const evaluateClusterCache = () => {
    // Return true when basic dataset/model options are available
    if (!datasets || datasets.length === 0) return false;
    if (!dataModels || Object.keys(dataModels).length === 0) return false;
    return true;
  };

  const handleApplyFilters = () => {
    // Re-fetch cluster data with current filter settings
    fetchClusterData(false);
  };

  useEffect(() => {
    // Invalidate graph and selection state when dataset or dataModel changes
    setLabels([]);
    setParents([]);
    setValues([]);
    setUris([]);
    setTypes([]);
    setSelectedCluster("");
    setSelectedClusterLabels([]);
    setSelectedClusterValues([]);
    setSelectedClusterUris([]);
    setSelectedClusterTypes([]);
  }, [selectedDataset, dataModel]);

  return (
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: "flex" }}>
        <SideMenu />
        <AppNavbar />
        <Container
          sx={{ width: "100%", maxWidth: { sm: "100%", md: "1400px" }, pt: { xs: 8, md: 0 } }}
        >
          <Grid
            // container {/* Single page application for now... */}
            // spacing={2}
            columns={6}
            sx={{ mb: (theme) => theme.spacing(2), my: 4 }}
          >
            {/* <Grid item size={{ xs: 12, sm: 6, lg: 3 }}> */}{" "}
            {/* Decide whether to leave this page as a two column grid or not... */}
            {isLoading && !evaluateClusterCache() ? (
              <Grid sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
                <CircularProgress />
              </Grid>
            ) : !evaluateClusterCache() ? (
              <Grid>
                <Alert severity="error" sx={{ mb: 2 }}>
                  Trouble setting up this page. Please contact the administrator
                  and wait for a fix.
                </Alert>
              </Grid>
            ) : (
              <Grid>
                {responseStatus && (
                  <Alert
                    severity={
                      responseStatus.includes("error") ? "error" : "success"
                    }
                  >
                    {responseStatus}
                  </Alert>
                )}
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
                <DataControlPanel
                  onCreate={handleCreate}
                  onAdd={handleAdd}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                  onExport={handleExport}
                  isDefaultDataset={["bsds300", "cifar10"].includes(selectedDataset)}
                />
                <Modal open={createOpen} onClose={handleCreateCancel}>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "center",
                      marginTop: 80,
                    }}
                  >
                    <CreateDatasetForm
                      onSubmit={handleCreateSubmit}
                      onCancel={handleCreateCancel}
                      defaultDatasetType="clusters"
                    />
                  </div>
                </Modal>
                <Modal open={controlOpen} onClose={handleFormCancel}>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "center",
                      marginTop: 80,
                    }}
                  >
                    <ClusterForm
                      key={controlKey}
                      mode={controlMode}
                      initial={formInitial}
                      candidates={labels.filter((_, idx) => types[idx] === "cluster" && labels[idx] !== "Root")}
                      onSubmit={(values) =>
                        handleFormSubmit(values, controlMode)
                      }
                      onCancel={handleFormCancel}
                    />
                  </div>
                </Modal>
                <FilterComponent
                  handleApplyFilters={handleApplyFilters}
                  sort={sort}
                  setSort={setSort}
                  rangeSliderValue={rangeSliderValue}
                  setRangeSliderValue={setRangeSliderValue}
                  rangeStartSliderValue={rangeStartSliderValue}
                  setRangeStartSliderValue={setRangeStartSliderValue}
                  options={sortOptions}
                  max={getMaxClusters()}
                  maxRange={maxRange}
                  setMaxRange={setMaxRange}
                  maxRangeConst={maxRangeConst}
                  selectedDataset={selectedDataset}
                  dataModel={dataModel}
                  datasetType="clusters"
                />
                {isLoading ? (
                  <p>Loading cluster data...</p>
                ) : !labels.some(l => l !== "Root") ? (
                  <Box sx={{ p: 4 }}>
                    <Alert severity="info">There is no cluster data. Please add cluster nodes using the ADD button.</Alert>
                  </Box>
                ) : (
                  <Plot
                    data={[
                      {
                        type: "treemap",
                        labels: labels,
                        parents: parents,
                        values: values,
                        customdata: types,
                        textinfo: "label+value+percent parent",
                        hoverinfo: "label+text+percent entry",
                      },
                    ]}
                    layout={{
                      title: "Semantic Zoom Tree Map",
                      margin: { t: 50, l: 25, r: 25, b: 25 },
                      uirevision: "true",
                      clickmode: "event+select",
                    }}
                    config={{
                      responsive: true,
                      displayModeBar: true,
                    }}
                    onUpdate={handlePlotUpdate}
                    onInitialized={handlePlotInitialized}
                    style={{ width: "100%", height: "500px" }}
                    uirevision="true"
                    useResizeHandler={true}
                  />
                )}
                <Typography variant="h3" sx={{ my: 4 }}>
                  {selectedCluster && !isLoading && selectedCluster !== (labels[0] || "")
                    ? selectedCluster
                    : "Select a cluster to see its details"}
                </Typography>
                {selectedCluster && !isLoading && (
                  <Grid container spacing={2} alignItems="stretch">
                    {selectedClusterLabels.map((label, index) => {
                      const value = selectedClusterValues[index];
                      const uri = selectedClusterUris[index];
                      const type = selectedClusterTypes[index];

                      // only render leaf nodes with non-zero probability
                      if (type === "leaf" && value && value !== 0) {
                        return (
                          <Grid item={true} xs={12} sm={6} md={4} lg={3} key={index}>
                            <Card style={{ height: "100%" }}>
                              <CardMedia
                                component="img"
                                height="140"
                                image={/^https?:\/\//.test(uri) ? uri : `${API_BASE_URL}${uri}`}
                                alt={label}
                              />
                              <CardContent>
                                <Typography variant="h6" align="center">
                                  {label}
                                </Typography>
                                <Typography variant="body2" align="center" color="textSecondary">
                                  Confidence: {(value * 100).toFixed(2)}%
                                </Typography>
                              </CardContent>
                            </Card>
                          </Grid>
                        );
                      }
                      return null;
                    })}
                  </Grid>
                )}
              </Grid>
            )}
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
              staticMetadata={staticMetadata}
            />
          </Grid>
        </Container>
      </Box>
    </AppTheme>
  );
}
