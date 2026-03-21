import * as React from "react";

import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
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
import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import Plot from "react-plotly.js";
import Container from "@mui/material/Container";
import Modal from "@mui/material/Modal";
import DataControlPanel from "../components/DataControlPanel";
import HeatmapForm from "../components/HeatmapForm";
import CreateDatasetForm from "../components/CreateDatasetForm";
import { makeDataControlHandlers } from "../../common/DataControlHandlers";
import FilterComponent from "./FilterComponent";
import { FormControl, InputLabel, Select, MenuItem } from "@mui/material";
import PageInfo from "./PageInfo";
import InfoModal from "./InfoModal";
import Alert from "@mui/material/Alert";
import DatasetCommons from "../../common/DatasetCommons";
import SelectedDataset from "../../common/managers/SelectedDataset";
import Datasets from "../../common/managers/Datasets";
import DataModels from "../../common/managers/DataModels";
import SortOptions from "../../common/managers/SortOptions";
import MaxCache from "../../common/managers/MaxCache";
import StaticMetadata from "../../common/managers/StaticMetadata";


const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

function SortType(props) {
  const options = props.options;
  const sortType = props.sortType;
  const setSortType = props.setSortType;
  const selectedDataset = props?.selectedDataset;

  const handleSortTypeChange = (event) => {
    setSortType(event.target.value);
  };

  function getSortTypes () {
    // console.log("sort types", options);
    // options[selectedDataset][datasetType]["sortBy"]
    if (!options                          || 
        Object.keys(options).length === 0 ||
        Object.keys(options[selectedDataset]) === undefined ||
        Object.keys(options[selectedDataset]["heatmap"]).length == 0 ||
        options[selectedDataset]["heatmap"] === undefined ||
        options[selectedDataset]["heatmap"]["sortType"].length === 0) {
      return [];
    }
    return options[selectedDataset]["heatmap"]["sortType"];
  }

  return (
    <FormControl fullWidth sx={{ my: 2 }}>
      <InputLabel id="sort-type">Sort Type</InputLabel>
      <Select
        labelId="sort-type"
        value={sortType}
        onChange={handleSortTypeChange}
        label="Sort by:"
      >
        {getSortTypes().map((value, index) => (
          <MenuItem key={value["value"]} value={value["value"]}>
            {value !== "no_sort" && value["displayValue"]}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export default function Correlations(props) {
  const location = useLocation();
  const [isLoading, setIsLoading] = useState(true);
  const [objects, setObjects] = useState(null);
  const [matrix, setMatrix] = useState(null);

  const [rangeSliderValue, setRangeSliderValue] = useState(5);
  const [rangeStartSliderValue, setRangeStartSliderValue] = useState(1);
  const [sort, setSort] = useState("highest_probability");
  const [sortType, setSortType] = useState("strongest_pair");
  const [selectedDataset, setSelectedDataset] = useState("bsds300");
  const [infOpen, setInfOpen] = useState(false);

  const [dataModel, setDataModel] = useState("json");

  // Deep linking logic
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const ds = params.get("dataset");
    const dt = params.get("dataType");
    const sortParam = params.get("sort");
    const sortTypeParam = params.get("sortType");
    const rangeParam = params.get("range");
    const rangeStartParam = params.get("rangeStart");
    if (ds) setSelectedDataset(ds);
    if (dt) setDataModel(dt);
    if (sortParam) setSort(sortParam);
    if (sortTypeParam) setSortType(sortTypeParam);
    if (rangeParam && !Number.isNaN(Number(rangeParam))) setRangeSliderValue(Number(rangeParam));
    if (rangeStartParam && !Number.isNaN(Number(rangeStartParam))) setRangeStartSliderValue(Number(rangeStartParam));
  }, [location.search]);
  const [responseStatus, setResponseStatus] = useState("");

  const [controlOpen, setControlOpen] = useState(false);
  const [controlMode, setControlMode] = useState("");
  const [formInitial, setFormInitial] = useState({});
  const [createOpen, setCreateOpen] = useState(false);

  // will adapt dynamically using the max possible range and the current start value
  const [maxRange, setMaxRange] = useState(0);
  const [staticMetadata, setStaticMetadata] = useState({});

  const [maxHeatmap, setMaxHeatmap] = useState({});

  const handleDataModelChange = (event) => {
    setDataModel(event.target.value);
  };

  const [dataModels, setDataModels] = useState({});
  const [datasets, setDatasets] = useState([]);
  const [sortOptions, setSortOptions] = useState({});

  // managers for safer access
  const selectedDatasetObj = new SelectedDataset(selectedDataset, setSelectedDataset);
  const datasetsObj = new Datasets(datasets, setDatasets);
  const dataModelsObj = new DataModels(dataModels, setDataModels);
  const sortOptionsObj = new SortOptions(sortOptions, setSortOptions);
  const maxHeatmapObj = new MaxCache(maxHeatmap, setMaxHeatmap);
  const staticMetadataObj = new StaticMetadata(staticMetadata, setStaticMetadata);

  // max possible range
  const [max, setMax] = useState(0);

  const handleApplyFilters = () => {
    fetchHeatmapData(false);
  };

  // Data control handlers (heatmap-specific)
  const openControlModal = (mode, initial = {}) => {
    setControlMode(mode);
    setFormInitial(initial);
    setControlOpen(true);
  };

  

  const handleInfOpen = () => {
    setInfOpen(true);
  };

  useEffect(() => {
    // make sure to not fill this parameter if sort is not being used
    if (sort === "no_sort" && sortType !== "no_sort") {
      setSortType("no_sort");
    } else if (sort !== "no_sort" && (!sortType || sortType === "no_sort")) {
      // if it used give it a default value
      setSortType("strongest_pair");
    }
  }, [sort, sortType]);

  const handleDatasetChange = (event) => {
    setSelectedDataset(event.target.value);
  };

  const handleInfClose = () => {
    setInfOpen(false);
  };

  function getDatasets() {
    return datasetsObj.get();
  }

  function getMaxRange() {
    // const mh = maxHeatmapObj.get();
    // if (!mh || Object.keys(mh).length === 0) return 6;
    // const val = maxHeatmapObj.getFor(selectedDataset, dataModel);
    // if (val === undefined || val === null) return 6;
    // let max = val;
    // let start = rangeStartSliderValue;
    // setMax(max - start);
    // return max - start;
    return max;
  }

  const {
      fetchMetadata,
      fetchClusterData,
      fetchHeatmapData,
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
      datasetType: "heatmap",
      rangeSliderValue,
      setRangeSliderValue,
      rangeStartSliderValue,
      setRangeStartSliderValue,
        sort,
        setSort,
        objects,
        matrix,
        setObjects,
        setMatrix,
        maxHeatmap,
        setMaxHeatmap,
      sortType,
      sortOptions,
      setSortOptions,
      setMax
    });

  const {
    handleCreate: commonHandleCreate,
    handleAdd: commonHandleAdd,
    handleUpdate: commonHandleUpdate,
    handleDelete: commonHandleDelete,
    handleExport: commonHandleExport,
    handleFormSubmit: commonHandleFormSubmit,
    handleCreateSubmit: commonHandleCreateSubmit,
  } = makeDataControlHandlers({
    selectedDataset,
    setSelectedDataset,
    dataModel,
    setDataModel,
    setDatasets,
    datasetType: "heatmap",
    openControlModal,
    setCreateOpen,
    setResponseStatus,
    refreshFn: fetchHeatmapData,
    fetchMetadata,
  });

  const handleFormCancel = () => {
    setControlOpen(false);
    setFormInitial({});
  };

  const handleCreateCancel = () => setCreateOpen(false);

  return (
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: "flex" }}>
        <SideMenu />
        <AppNavbar />
        <Container sx={{ my: 4 }}>
          {responseStatus && responseStatus.toLowerCase().includes("error") && (
            <Alert severity="error">
              {responseStatus}
            </Alert>
          )}
          <PageInfo
            datasets={getDatasets()}
            handleInfOpen={handleInfOpen}
            selectedDataset={selectedDataset}
            setSelectedDataset={setSelectedDataset}
            handleDatasetChange={handleDatasetChange}
            dataModels={dataModels}
            dataModel={dataModel}
            handleDataModelChange={handleDataModelChange}
          />
          <DataControlPanel
            onCreate={commonHandleCreate}
            onAdd={commonHandleAdd}
            onUpdate={commonHandleUpdate}
            onDelete={commonHandleDelete}
            onExport={commonHandleExport}
          />
          <Modal open={createOpen} onClose={handleCreateCancel}>
            <div style={{ display: "flex", justifyContent: "center", marginTop: 80 }}>
              <CreateDatasetForm onSubmit={commonHandleCreateSubmit} onCancel={handleCreateCancel} />
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
            sortType={
              <SortType
                options={sortOptions}
                sortType={sortType}
                setSortType={setSortType}
                selectedDataset={selectedDataset}
              />
            }
            max={getMaxRange()}
            maxRange={maxRange}
            setMaxRange={setMaxRange}
            selectedDataset={selectedDataset}
            dataModel={dataModel}
            datasetType="heatmap"
          />
          <Modal open={controlOpen} onClose={handleFormCancel}>
            <div style={{ display: "flex", justifyContent: "center", marginTop: 80 }}>
              <HeatmapForm mode={controlMode} initial={formInitial} onSubmit={(values) => commonHandleFormSubmit(values, controlMode)} onCancel={handleFormCancel} />
            </div>
          </Modal>
          {isLoading ? (
            <p>Loading heatmap data...</p>
          ) : (
            // If there's no matrix/objects data show a placeholder message
            (!matrix || !Array.isArray(matrix) || matrix.length === 0 || !objects || !Array.isArray(objects) || objects.length === 0) ? (
              <Box sx={{ p: 4 }}>
                <Alert severity="info">There is no heatmap data. Please add heatmap data using the ADD button.</Alert>
              </Box>
            ) : (
              <Plot
                data={[
                  {
                    z: matrix,
                    x: objects,
                    y: objects,
                    type: "heatmap",
                    colorscale: "Viridis",
                  },
                ]}
                layout={{
                  title: "Semantic Similarity Heatmap",
                  xaxis: { title: "Objects", automargin: true },
                  yaxis: { title: "Objects", automargin: true },
                  margin: { t: 50, l: 25, r: 25, b: 25 },
                }}
                style={{ width: "100%", height: "600px" }}
              />
            )
          )}
          <InfoModal
            infOpen={infOpen}
            handleInfClose={handleInfClose}
            selectedDataset={selectedDataset}
            staticMetadata={staticMetadata}
          />
        </Container>
      </Box>
    </AppTheme>
  );
}
