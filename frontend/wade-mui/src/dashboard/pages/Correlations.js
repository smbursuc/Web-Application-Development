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
import {
  chartsCustomizations,
  dataGridCustomizations,
  datePickersCustomizations,
  treeViewCustomizations,
} from "../theme/customizations";
import { useEffect, useState } from "react";
import Plot from "react-plotly.js";
import Container from "@mui/material/Container";
import FilterComponent from "./FilterComponent";
import { FormControl, InputLabel, Select, MenuItem } from "@mui/material";
import PageInfo from "./PageInfo";
import InfoModal from "./InfoModal";

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

  const handleSortTypeChange = (event) => {
    setSortType(event.target.value);
  };

  return (
    <FormControl fullWidth sx={{ my: 2 }}>
      <InputLabel id="sort-type">Sort Type</InputLabel>
      <Select
        labelId="sort-type"
        value={sortType}
        onChange={handleSortTypeChange}
        label="Sort by:"
      >
        {options["sort_type"].values.map((value, index) => (
          <MenuItem key={value} value={value}>
            {value !== "no_sort" && options["sort_type"].descriptions[index]}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export default function Correlations(props) {
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
  const [maxHeatmap, setMaxHeatmap] = useState(() => {
      return getFromLocalStorage("maxHeatmap") || {};
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

  const features = {
    cifar10: [
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
    ],
    bsds300: [
      {
        title: "Description",
        description:
          "The BSDS300 and CIFAR-100 datasets are labeled subsets of the 80 million tiny images dataset.",
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
    ],
  };

  const options = {
    sort: {
      values: ["highest_probability", "lowest_probability", "no_sort"],
      descriptions: ["Highest Probability", "Lowest Probability", "No sorting"],
    },
    sort_type: {
      values: ["average_similarity", "strongest_pair", "no_sort"],
      descriptions: ["Average Similarity", "Strongest Pair", "No Sorting"],
    },
  };

  const maxObjects = {
    json: {
      cifar10: 50000,
      bsds300: 91,
    },
    rdf: {
      cifar10: 50000,
      bsds300: 8190,
    },
  };

  const fetchData = async () => {
    let queryParams = "";
    if (sort !== "no_sort") {
      queryParams = queryParams + `sort=${sort}`;
    }

    queryParams = queryParams + "&";

    if (sortType !== "no_sort") {
      queryParams = queryParams + `sortType=${sortType}`;
    }

    // http://127.0.0.1:5000/api/correlations/cifar10?
    // http://127.0.0.1:8081/api/${selectedDataset}/heatmaps/json?
    const url =
      `http://127.0.0.1:8081/api/${selectedDataset}/heatmaps/${dataModel}?` +
      queryParams +
      `&range=${rangeSliderValue}&rangeStart=${rangeStartSliderValue}`;

    try {
      const response = await fetch(url);
      const data = await response.json();
      setIsLoading(false);

      if (!data.data) {
        setObjects(data.objects);
        setMatrix(data.matrix);
      } else {
        setObjects(data.data.objects);
        setMatrix(data.data.matrix);
      }
    } catch (error) {
      console.error("Failed to fetch correlation data:", error);
    }
  };

  const fetchMetadata = async (resetParams) => {
    setIsLoading(true);

    if (resetParams) {
      setRangeSliderValue(5);
      setRangeSliderValue(1);
    }
    
    const url = `http://127.0.0.1:8081/api/${selectedDataset}/metadata/${dataModel}/clusters`;
    const response = await fetch(url);
    const data = await response.json();
    const result = data.data;

    // load the metadata cache, update it, and save it back
    const cache = getFromLocalStorage("maxHeatmap") || {};
    cache[selectedDataset] = {
      ...(cache[selectedDataset] || {}), // safeguard for undefined dataset
      [dataModel]: result.size,
    };

    saveToLocalStorage("maxHeatmap", cache);
    setMaxHeatmap(cache);
    setIsLoading(false);
  };

  function saveToLocalStorage(key, data) {
    localStorage.setItem(key, JSON.stringify(data));
  }

  function getFromLocalStorage(key) {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  }

  useEffect(() => {
    fetchData(true);
    let cache = getFromLocalStorage("maxHeatmap");
    if (
      !cache ||
      !cache[selectedDataset] ||
      !cache[selectedDataset][dataModel]
    ) {
      fetchMetadata();
    } else {
      setMaxHeatmap(cache);
    }
  }, [selectedDataset, dataModel]);

  const handleApplyFilters = () => {
    fetchData(false);
  };

  const handleInfOpen = () => {
    setInfOpen(true);
  };

  useEffect(() => {
    // make sure to not fill this parameter if sort is not being used
    if (sort === "no_sort") {
      setSortType("no_sort");
    } else {
      // if it used give it a default value
      setSortType("strongest_pair");
    }
  }, [sort]);

  const handleDatasetChange = (event) => {
    setSelectedDataset(event.target.value);
  };

  const handleInfClose = () => {
    setInfOpen(false);
  };

  return (
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: "flex" }}>
        <SideMenu />
        <AppNavbar />
        <Container sx={{ my: 4 }}>
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
            sortType={
              <SortType
                options={options}
                sortType={sortType}
                setSortType={setSortType}
              />
            }
            max={maxObjects[dataModel][selectedDataset]}
          />
          {isLoading ? (
            <p>Loading heatmap data...</p>
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
          )}
          <InfoModal
            infOpen={infOpen}
            handleInfClose={handleInfClose}
            selectedDataset={selectedDataset}
          />
        </Container>
      </Box>
    </AppTheme>
  );
}
