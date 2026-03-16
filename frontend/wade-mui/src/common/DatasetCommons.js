import { useEffect, useRef } from "react";
import { data } from "react-router-dom";
import SelectedDataset from "./managers/SelectedDataset";
import SortOptions from "./managers/SortOptions";
import Datasets from "./managers/Datasets";
import DataModels from "./managers/DataModels";
import StaticMetadata from "./managers/StaticMetadata";
import MaxCache from "./managers/MaxCache";
import Results from "./managers/Results";
import ResponseStatus from "./managers/ResponseStatus";

export default function DatasetCommons(props) {
  const selectedDataset = props.selectedDataset;
  const dataModel = props.dataModel;
  const datasets = props.datasets;
  const dataModels = props.dataModels;
  const sortOptions = props.sortOptions;
  const setDatasets = props.setDatasets;
  const datasetsObj = new Datasets(props.datasets, setDatasets);
  const setSelectedDataset = props.setSelectedDataset;
  const setDataModel = props.setDataModel;
  const dataModelsObj = new DataModels(props.dataModels, props.setDataModels);
  const staticMetadataObj = new StaticMetadata(props.staticMetadata, props.setStaticMetadata);
  const setIsLoading = props.setIsLoading;
  const responseStatusObj = new ResponseStatus(props.responseStatus, props.setResponseStatus);
  const maxClustersObj = new MaxCache(props.maxClusters, props.setMaxClusters);
  const maxHeatmapObj = new MaxCache(props.maxHeatmap, props.setMaxHeatmap);
  const datasetType = props.datasetType;
  const isLoading = props.isLoading;
  const responseStatus = props.responseStatus;
  const rangeSliderValue = props.rangeSliderValue;
  let setRangeSliderValue = props.setRangeSliderValue;
  const rangeStartSliderValue = props.rangeStartSliderValue;
  const setRangeStartSliderValue = props.setRangeStartSliderValue;
  const sort = props.sort;
  const setSort = props.setSort;
  const sortType = props.sortType? props.sortType : null;
  const setSortType = props.setSortType? props.setSortType : null;
  const updateValues = props.updateValues ? props.updateValues : null;
  const resultsObj = new Results(props.objects, props.matrix, props.setObjects, props.setMatrix);
  const setSortOptions = props.setSortOptions;

  const selectedDatasetObj = new SelectedDataset(selectedDataset, setSelectedDataset);
  const sortOptionsObj = new SortOptions(props.sortOptions, setSortOptions);
  const setMax = props.setMax;

  const fetchMetadata = async (generalInfo) => {
    setIsLoading(true);
    console.log(
      "Fetching metadata for",
      "'" + selectedDatasetObj.get() + "'",
      "'" + dataModel + "'",
      generalInfo
    );
    const params = new URLSearchParams({
      datasetName: selectedDatasetObj.get(),
      dataType: dataModel,
      datasetType: datasetType,
      generalInfo: generalInfo,
    });
    const url = `http://localhost:8081/api/metadata?${params.toString()}`;
    let data = {};
    try {
      const response = await fetch(url, { credentials: "include" });
      data = await response.json();
    } catch (error) {
      console.error("Failed to fetch metadata:", error);
    }

    const status = data.status;
    if (status !== "success") {
      responseStatusObj.set(`Error: ${data.message}`);
      return;
    }
    const result = data.data;

    if (!generalInfo) {
      // load the metadata cache, update it, and save it back
      let cacheKey =
        "max" + (datasetType === "clusters" ? "Clusters" : "Heatmap");
      const cache = getFromLocalStorage(cacheKey) || {};
      cache[selectedDatasetObj.get()] = {
        ...(cache[selectedDatasetObj.get()] || {}), // safeguard for undefined dataset
        [dataModel]: result.size,
      };

      saveToLocalStorage(cacheKey, cache);
      if (datasetType === "clusters") {
        maxClustersObj.set(cache);
      } else {
        maxHeatmapObj.set(cache);
      }
    } else {
      const staticMetadata = result.staticMetadata;
      staticMetadataObj.set(staticMetadata);
    }
    setIsLoading(false);
    return true;
  };

  function saveToLocalStorage(key, data) {
    localStorage.setItem(key, JSON.stringify(data));
  }

  function getFromLocalStorage(key) {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  }

  useEffect(() => {
    // read current static metadata from manager and reset local state for rebuilding
    const staticMetadata = staticMetadataObj.get() || {};
    const newDatasets = [];
    const newDataModels = {};
    const newSortOptions = {};

    // Helper for updating sort options locally
    const updateSortOptions = (dataset, type, sortBy, sortType) => {
      if (!newSortOptions[dataset]) newSortOptions[dataset] = {};
      newSortOptions[dataset][type] = { sortBy, sortType };
    };

    // If a global `common` block exists in static metadata, populate it
    if (staticMetadata["common"]) {
      const sortByArrCommon = [];
      const sortTypeArrCommon = [];
      const sortOptionsForCommon = staticMetadataObj.getSortOptions("common") || {};
      for (let sortKey of Object.keys(sortOptionsForCommon)) {
        if (sortKey === "common") {
          let commonSortOptions = sortOptionsForCommon[sortKey];
          for (let sortOption of commonSortOptions) {
            sortByArrCommon.push({
              value: sortOption["name"],
              displayValue: sortOption["displayValue"],
            });
          }
        }

        if (sortKey === "heatmaps" && datasetType === "heatmap") {
          let heatmapSortOptions = sortOptionsForCommon[sortKey];
          for (let sortOption of heatmapSortOptions) {
            sortTypeArrCommon.push({
              value: sortOption["name"],
              displayValue: sortOption["displayValue"],
            });
          }
        }
      }
      updateSortOptions("common", datasetType, sortByArrCommon, sortTypeArrCommon);
    }

    for (const dataset of Object.keys(staticMetadata)) {
      if (dataset === "common") continue;

      // data models for this dataset (respect datasetType-aware `datasetInfo`)
      const dataModelEntries = [];
      const rawModels = staticMetadataObj.getDataModels(dataset, datasetType);
      for (let model of rawModels) {
        dataModelEntries.push({
          value: model["name"],
          displayValue: model["displayValue"],
        });
      }

      // ONLY include datasets that have at least one valid data model for the current datasetType
      if (dataModelEntries.length > 0) {
        newDatasets.push({
          value: dataset,
          displayValue: staticMetadataObj.getDisplayValue(dataset) || dataset,
        });
        newDataModels[dataset] = dataModelEntries;

        const sortByArr = [];
        const sortTypeArr = [];
        let sortOptionsForDataset = staticMetadataObj.getSortOptions(dataset);
        for (let sortKey of Object.keys(sortOptionsForDataset)) {
          if (sortKey === "common") {
            let commonSortOptions = sortOptionsForDataset[sortKey];
            for (let sortOption of commonSortOptions) {
              sortByArr.push({
                value: sortOption["name"],
                displayValue: sortOption["displayValue"],
              });
            }
          }

          if (sortKey === "heatmaps" && datasetType === "heatmap") {
            let heatmapSortOptions = sortOptionsForDataset[sortKey];
            for (let sortOption of heatmapSortOptions) {
              sortTypeArr.push({
                value: sortOption["name"],
                displayValue: sortOption["displayValue"],
              });
            }
          }
        }
        updateSortOptions(dataset, datasetType, sortByArr, sortTypeArr);
      }
    }

    // UPDATE ALL MANAGERS AT ONCE
    datasetsObj.set(newDatasets);
    dataModelsObj.set(newDataModels);
    sortOptionsObj.set(newSortOptions);

    let defaultModel = staticMetadataObj.getDataModels(selectedDatasetObj.get(), datasetType)[0]?.name || "";
    if (defaultModel !== "" && dataModel === "") setDataModel(defaultModel);
  }, [props.staticMetadata]);

  useEffect(() => {
    const ds = datasetsObj.get();
    const currentDataset = selectedDatasetObj.get();
    const staticMetadataLoaded = props.staticMetadata && Object.keys(props.staticMetadata).length > 0;

    if (currentDataset === "") {
      if (ds && ds.length > 0) {
        selectedDatasetObj.set(ds[0].value);
      }
    } else {
      // If the current selected dataset is not in the list of available datasets
      // for this datasetType, reset it to the first available one.
      if (ds && ds.length > 0) {
        const found = ds.find((d) => d.value === currentDataset);
        if (!found) {
          console.log(`Resetting invalid selection ${currentDataset} to ${ds[0].value}`);
          selectedDatasetObj.set(ds[0].value);
          setDataModel(""); 
          return;
        }
      } else if (ds && ds.length === 0 && currentDataset !== "") {
        // During initial page load, keep deep-link selection until metadata has been loaded.
        // Only clear selection if metadata is already loaded and confirms no datasets for this type.
        if (staticMetadataLoaded) {
          selectedDatasetObj.set("");
          setDataModel("");
          return;
        }
      }
    }
    // If the available data models for the selected dataset change, ensure
    // the current `dataModel` is valid for the CURRENT dataset.
    const modelsForDataset = dataModelsObj.getFor(selectedDatasetObj.get());
    if (modelsForDataset && modelsForDataset.length > 0) {
      const allowed = modelsForDataset.find((m) => m.value === dataModel);
      if (!allowed) {
        setDataModel(modelsForDataset[0].value);
      }
    }
    if (sort === "") {
      const soFor = sortOptionsObj.getFor(selectedDatasetObj.get());
      if (soFor && soFor[datasetType]) {
        const sortByOptions = sortOptionsObj.getSortBy(selectedDatasetObj.get(), datasetType);
        if (sortByOptions && sortByOptions.length > 0) {
          setSort(sortByOptions[0].value);
        }
      }
    }
    if (sortType === "" && datasetType === "heatmap") {
      const sortTypeOptions = sortOptionsObj.getSortType(selectedDatasetObj.get(), datasetType);
      if (sortTypeOptions && sortTypeOptions.length > 0) {
        setSortType(sortTypeOptions[0].value);
      }
    }
  }, [datasets, dataModels, sortOptions, selectedDataset, dataModel]);

  const prevSelectionRef = useRef({ dataset: selectedDatasetObj.get(), type: datasetType });
  const lastLoadKeyRef = useRef("");

  useEffect(() => {
    const loadData = async () => {
      const current = selectedDatasetObj.get();
      const prev = prevSelectionRef.current;

      // If selected dataset or view type changed, invalidate state and return.
      // This allows the next render cycle to have a clean state before fetching.
      if (current !== "" && (prev.dataset !== current || prev.type !== datasetType)) {
        prevSelectionRef.current = { dataset: current, type: datasetType };
        setDataModel("");
        return;
      }
      prevSelectionRef.current = { dataset: current, type: datasetType };

      // Safeguard: do not attempt to load data if the selected dataset 
      // is not in the filtered list of available datasets for this view.
      const ds = datasetsObj.get();
      if (current !== "" && ds && ds.length > 0) {
        if (!ds.find(d => d.value === current)) {
          console.warn(`Dataset ${current} not valid for type ${datasetType}. Skipping fetch.`);
          return;
        }
      }

      let isInitialSelectionInvalid =
        !selectedDatasetObj.get() ||
        selectedDatasetObj.get() === "" ||
        !dataModel ||
        dataModel === "";

      let isDatasetValid = ds && ds.length > 0 && ds.find(d => d.value === selectedDatasetObj.get());

      let isDataModelValid = dataModelsObj.get() && dataModelsObj.get()[selectedDatasetObj.get()] && dataModelsObj.get()[selectedDatasetObj.get()].length > 0;

      let sortOptionsValid;
      let sortOptionsValidHeatmap;
      if (datasetType === "heatmap") {
        const soFor = sortOptionsObj.getFor(selectedDatasetObj.get());
        sortOptionsValidHeatmap =
          sortOptionsObj.get() &&
          Object.keys(sortOptionsObj.get()).length > 0 &&
          soFor !== undefined &&
          soFor[datasetType] !== undefined &&
          Array.isArray(sortOptionsObj.getSortType(selectedDatasetObj.get(), datasetType)) &&
          sortOptionsObj.getSortType(selectedDatasetObj.get(), datasetType).length > 0 &&
          Array.isArray(sortOptionsObj.getSortBy(selectedDatasetObj.get(), datasetType)) &&
          sortOptionsObj.getSortBy(selectedDatasetObj.get(), datasetType).length > 0;
      }
      // Prefer dataset-specific sort options, fall back to the `common` block when absent
      let sortOptionsValidCommon = false;
      if (sortOptionsObj.get()) {
        const sortByForDataset = sortOptionsObj.getSortBy(selectedDatasetObj.get(), datasetType);
        sortOptionsValidCommon = Array.isArray(sortByForDataset) && sortByForDataset.length > 0;
      }

      sortOptionsValid =
        sortOptionsValidCommon &&
        (datasetType === "clusters" ? true : sortOptionsValidHeatmap);

      let initializationNeeded =
        isInitialSelectionInvalid ||
        !isDatasetValid ||
        !isDataModelValid ||
        !sortOptionsValid;

      const loadKey = `${datasetType}|${selectedDatasetObj.get()}|${dataModel}|${sort || ""}`;

      if (initializationNeeded) {
        lastLoadKeyRef.current = "";
        // We lack a valid selection or the client state is not fully initialized.
        // Fetch master metadata (generalInfo=true) to populate/refresh the selectors.
        await fetchMetadata(true);
        return;
      }

      // Prevent duplicate fetches for identical selection/sort (e.g., React StrictMode dev double-invoke).
      if (lastLoadKeyRef.current === loadKey) {
        return;
      }
      lastLoadKeyRef.current = loadKey;

      // If we reach here, we have a valid selection and metadata is loaded (Step 2).
      // The range selectors need to know the max range; query for the specific size (generalInfo=false).
      let cacheKey =
        "max" + (datasetType === "clusters" ? "Clusters" : "Heatmap");
      let cache = getFromLocalStorage(cacheKey);
      if (
        (!cache ||
          !cache[selectedDatasetObj.get()] ||
          !cache[selectedDatasetObj.get()][dataModel])
      ) {
        await fetchMetadata(false);
      }

      console.log("Loading data for", current, dataModel, "initializationNeeded:", initializationNeeded);
      if (datasetType === "clusters") {
        await fetchClusterData(false);
      } else {
        await fetchHeatmapData();
      }
    };
    loadData();
  }, [selectedDataset, dataModel, sort, datasets, dataModels]);

  useEffect(() => {
    // update rangeSliderValue and rangeStartSliderValue
    const maxClustersCache = maxClustersObj.get();
    const maxHeatmapCache = maxHeatmapObj.get();
    let maxValue = null;
    if (datasetType === "clusters") {
      if (
        maxClustersCache &&
        maxClustersCache[selectedDatasetObj.get()] &&
        maxClustersCache[selectedDatasetObj.get()][dataModel]
      ) {
        maxValue =
          maxClustersCache[selectedDatasetObj.get()][dataModel];
      }
    } else {
      if (
        maxHeatmapCache &&
        maxHeatmapCache[selectedDatasetObj.get()] &&
        maxHeatmapCache[selectedDatasetObj.get()][dataModel]
      ) {
        maxValue =
          maxHeatmapCache[selectedDatasetObj.get()][dataModel];
    }
    }
    if (maxValue !== null) {
      // maxValue == 1 ? setMax(2) : setMax(maxValue - 1);
      setMax(maxValue);
      setRangeStartSliderValue(0);
    }
  }, [props.maxClusters, props.maxHeatmap]);

  const fetchClusterData = async (resetParams) => {
    setIsLoading(true);

    const defaultRange = 5;
    const defaultRangeStart = 1;

    // do not use the react state values as they might be outdated due to async nature
    // the setters are not synchronous!
    let newRangeStart = rangeStartSliderValue;
    let newRange = rangeSliderValue;

    if (resetParams) {
      setRangeStartSliderValue(defaultRangeStart);
      setRangeSliderValue(defaultRange);
      newRangeStart = defaultRangeStart;
      newRange = defaultRange;
    }

    let queryParams = "";
    if (sort !== "no_sort") {
      queryParams = `sortDirection=${sort}`;
    }

    console.log("Fetching cluster data with params:", {
      range: newRange,
      rangeStart: newRangeStart,
      sort: sort,
    });

    queryParams =
      queryParams +
      `&range=${newRange}&rangeStart=${newRangeStart}&mode=cluster`; // mandatory
    const url =
      `http://localhost:8081/api/${selectedDatasetObj.get()}/clusters/${dataModel}?` +
      queryParams;

    try {
      const response = await fetch(url, { credentials: "include" });
      const data = await response.json();
      const status = data.status;
        if (status !== "success") {
        responseStatusObj.set(`${data.message}`);
        setIsLoading(false);
        return;
      }
      const result = data.data;
      responseStatusObj.set(null);

      updateValues(result, false);
    } catch (error) {
      console.error("Failed to fetch cluster data:", error);
    }
    setIsLoading(false);
    return true;
  };

  const fetchHeatmapData = async () => {
    let queryParams = "";
    if (sort !== "no_sort") {
      queryParams = queryParams + `sortDirection=${sort}`;
    }

    queryParams = queryParams + "&";

    if (sortType !== "no_sort") {
      queryParams = queryParams + `similaritySortCriteria=${sortType}`;
    }

    // http://localhost:5000/api/correlations/cifar10?
    // http://localhost:8081/api/${selectedDataset}/heatmaps/json?
    const url =
      `http://localhost:8081/api/${selectedDatasetObj.get()}/heatmap/${dataModel}?` +
      queryParams +
      `&range=${rangeSliderValue}&rangeStart=${rangeStartSliderValue}&mode=similarity`;

    try {
      const response = await fetch(url, { credentials: "include" });
      const data = await response.json();
      setIsLoading(false);

      if (!data.data) {
        resultsObj.setObjects(data.objects);
        resultsObj.setMatrix(data.matrix);
      } else {
        resultsObj.setObjects(data.data.objects);
        resultsObj.setMatrix(data.data.matrix);
      }
    } catch (error) {
      console.error("Failed to fetch correlation data:", error);
    }
    return true;
  };

  useEffect(() => {
    console.log("rangeSliderValue changed:", rangeSliderValue);
  }, [rangeSliderValue]);

  const originalSetter = setRangeSliderValue;
  setRangeSliderValue = (newValue) => {
    if (newValue === -1) {
      console.error('🐛 rangeSliderValue being set to -1 here:');
      console.trace();
      debugger;
    }
    return originalSetter(newValue);
  };

  return {
    fetchMetadata,
    fetchClusterData,
    fetchHeatmapData,
    getFromLocalStorage,
    saveToLocalStorage,
  };
}
