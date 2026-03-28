import API_BASE_URL from '../config';

/**
 * Removes the cached size entry for a specific dataset/dataModel combination so the
 * next read will re-query the server for the updated row count.
 * Should be called after any operation that changes the number of rows (create / delete).
 */
function invalidateSizeCache(datasetType, datasetName, dataModel) {
  const cacheKey = "max" + (datasetType === "clusters" ? "Clusters" : "Heatmap");
  try {
    const raw = sessionStorage.getItem(cacheKey);
    if (!raw) return;
    const cache = JSON.parse(raw);
    if (cache[datasetName]) {
      delete cache[datasetName][dataModel];
      // Clean up empty dataset entry to keep the store tidy
      if (Object.keys(cache[datasetName]).length === 0) {
        delete cache[datasetName];
      }
    }
    sessionStorage.setItem(cacheKey, JSON.stringify(cache));
  } catch (e) {
    // Defensive — if the cache is malformed just remove the whole key
    sessionStorage.removeItem(cacheKey);
  }
}

export function makeDataControlHandlers({
  selectedDataset,
  setSelectedDataset,
  dataModel,
  setDataModel,
  setDatasets,
  datasetType = "heatmap",
  openControlModal,
  setCreateOpen,
  setControlOpen,
  setResponseStatus,
  refreshFn,
  fetchMetadata,
  sort,
  rangeSliderValue,
  rangeStartSliderValue,
}) {
  const handleCreate = () => {
    // Open the dataset creation dialog for all dataset types
    if (typeof setCreateOpen === "function") setCreateOpen(true);
  };

  const handleAdd = () => {
    if (datasetType === "heatmap") {
      openControlModal("add", {});
    } else {
      openControlModal("add", {});
    }
  };

  const handleUpdate = () => {
    if (datasetType === "heatmap") {
      openControlModal("update", {});
    } else {
      openControlModal("update", {});
    }
  };

  const handleDelete = () => {
    if (datasetType === "heatmap") {
      openControlModal("delete", {});
    } else {
      openControlModal("delete", {});
    }
  };

  const handleExport = async (honorFilters = false) => {
    try {
      const urlType = datasetType === "heatmap" ? "heatmap" : "clusters";
      let url = `${API_BASE_URL}/api/${selectedDataset}/${urlType}/${dataModel}?export=true`;
      if (honorFilters) {
        url += `&honorFilters=true`;
        if (sort) url += `&sortDirection=${encodeURIComponent(sort)}`;
        if (rangeSliderValue != null) url += `&range=${rangeSliderValue}`;
        if (rangeStartSliderValue != null) url += `&rangeStart=${rangeStartSliderValue}`;
      }
      const resp = await fetch(url, { credentials: "include" });
      if (!resp.ok) throw new Error(`Export failed: ${resp.statusText}`);
      const json = await resp.json();
      const exportData = json?.data ?? json;
      const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: "application/json" });
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = `${selectedDataset}-${urlType}-export.json`;
      document.body.appendChild(a);
      a.click();
      a.remove();
    } catch (err) {
      console.error(err);
      setResponseStatus(`error: ${err.message}`);
    }
  };

  const handleFormSubmit = async (values, controlMode) => {
    try {
      if (datasetType === "heatmap") {
        const requestType = controlMode === "delete" ? "delete" : controlMode === "update" ? "update" : "create";
        let entryData = {};

        if (requestType === "create") {
          entryData = { pairs: [ { object1: values.object1, object2: values.object2, similarity: Number(values.similarity) } ] };
        } else if (requestType === "delete") {
          entryData = { pairs: [ { object1: values.object1, object2: values.object2 } ] };
        } else if (requestType === "update") {
          if (values.id) {
            entryData = { id: values.id, similarity: Number(values.similarity) };
          } else {
            entryData = { pairs: [ { object1: values.object1, object2: values.object2, similarity: Number(values.similarity) } ] };
          }
        }

        const body = {
          datasetName: selectedDataset,
          dataType: dataModel,
          datasetType: "heatmap",
          requestType: requestType,
          entry: { data: entryData },
        };

        const url = `${API_BASE_URL}/api/${selectedDataset}/heatmap/${dataModel}`;
        const method = requestType === "update" ? "PUT" : requestType === "delete" ? "DELETE" : "POST";

        const resp = await fetch(url, {
          method,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
          credentials: "include",
        });
        const data = await resp.json();
        if (data.status && data.status !== "success") {
          setResponseStatus(`error: ${data.message || JSON.stringify(data)}`);
        } else {
          // Invalidate size cache for operations that change row count.
          // Then re-fetch the updated size from the server so the range sliders
          // reflect the new count before the data refresh fires.
          if (requestType === "create" || requestType === "delete") {
            invalidateSizeCache("heatmap", selectedDataset, dataModel);
            if (typeof fetchMetadata === "function") await fetchMetadata(false);
          }
          setResponseStatus("success: operation completed");
          if (typeof setControlOpen === "function") setControlOpen(false);
          if (typeof refreshFn === "function") refreshFn(false);
        }
      } else {
        // Cluster CRUD operations
        const requestType = controlMode === "delete" ? "delete" : controlMode === "update" ? "update" : "create";
        let entryData;

        if (requestType === "create" || requestType === "add") {
          if (values.children) {
            // AI auto-create mode: hierarchical payload — new cluster + leaf in one shot.
            // values = { name: clusterName, parent: "Root", children: [{name, Probability, URI}], clusterTag }
            entryData = {
              name: values.name,
              parent: values.parent || "Root",
              children: values.children,
            };
          } else if (values.parent === "Root" && !values.probability && !values.uri) {
            // "Add a new cluster" mode — create just the cluster node
            entryData = { name: values.name, parent: "Root" };
          } else {
            // "Add object to existing cluster" mode
            const node = {};
            if (values.name) node.name = values.name;
            if (values.parent) node.parent = values.parent;
            if (values.probability != null && values.probability !== "") node.Probability = Number(values.probability);
            if (values.uri) node.URI = values.uri;
            entryData = node;
          }
        } else if (requestType === "delete") {
          // Delete by name and parent
          entryData = [{
            name: values.name,
            parent: values.parent,
          }];
        } else if (requestType === "update") {
          // Update by ID or by name+parent
          if (values.id) {
            entryData = {
              id: Number(values.id),
              probability: Number(values.probability),
            };
          } else {
            entryData = [{
              name: values.name,
              parent: values.parent,
              probability: Number(values.probability),
            }];
          }
        }

        const body = {
          datasetName: selectedDataset,
          dataType: dataModel,
          datasetType: "clusters",
          requestType: requestType,
          entry: { data: entryData },
        };

        const url = `${API_BASE_URL}/api/${selectedDataset}/clusters/${dataModel}`;
        const method = requestType === "update" ? "PUT" : requestType === "delete" ? "DELETE" : "POST";

        const resp = await fetch(url, {
          method,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
          credentials: "include",
        });
        const data = await resp.json();
        if (data.status && data.status !== "success") {
          setResponseStatus(`error: ${data.message || JSON.stringify(data)}`);
        } else {
          // Invalidate size cache for operations that change row count.
          // Then re-fetch the updated size from the server so the range sliders
          // reflect the new count before the data refresh fires.
          if (requestType === "create" || requestType === "delete") {
            invalidateSizeCache("clusters", selectedDataset, dataModel);
            if (typeof fetchMetadata === "function") await fetchMetadata(false);
          }
          setResponseStatus("success: operation completed");
          if (typeof setControlOpen === "function") setControlOpen(false);
          if (typeof refreshFn === "function") refreshFn(false);
        }
      }
    } catch (err) {
      console.error(err);
      setResponseStatus(`error: ${err.message}`);
    }
  };

  const handleCreateSubmit = async (body) => {
    try {
      if (datasetType === "heatmap") {
        const url = `${API_BASE_URL}/api/create`;
        const resp = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify(body),
        });
        const data = await resp.json();
        if (data.status && data.status !== "success") {
          setResponseStatus(`error: ${data.message || JSON.stringify(data)}`);
        } else {
          setResponseStatus("success: dataset created");
          setCreateOpen(false);
          // add the new dataset to the datasets listing so selects have the option
          if (typeof setDatasets === "function" && body && body.datasetName) {
            setDatasets((prev) => {
              const prevArr = Array.isArray(prev) ? prev : [];
              const exists = prevArr.find((d) => d && d.value === body.datasetName);
              if (exists) return prevArr;
              const entry = { value: body.datasetName, displayValue: body.datasetName };
              return [...prevArr, entry];
            });
          }
          // switch to the newly created dataset so DatasetCommons hooks run
          if (typeof setSelectedDataset === "function" && body && body.datasetName) {
            setSelectedDataset(body.datasetName);
          }
          // Also update the dataModel to the value chosen in the creation form
          if (typeof setDataModel === "function" && body && (body.dataType || body.dataModel)) {
            // Create form uses `dataType`; backend or other callers may use `dataModel`
            setDataModel(body.dataType || body.dataModel);
          }
          if (typeof fetchMetadata === "function") fetchMetadata(true);
        }
      } else {
        // For non-heatmap dataset types (e.g., clusters) creation uses the same
        // `/api/create` endpoint and the CreateDatasetForm payload.
        const url = `${API_BASE_URL}/api/create`;
        const resp = await fetch(url, {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
        const data = await resp.json();
        if (data.status && data.status !== "success") {
          setResponseStatus(`error: ${data.message || JSON.stringify(data)}`);
        } else {
          setResponseStatus("success: dataset created");
          setCreateOpen(false);
          if (typeof setDatasets === "function" && body && body.datasetName) {
            setDatasets((prev) => {
              const prevArr = Array.isArray(prev) ? prev : [];
              const exists = prevArr.find((d) => d && d.value === body.datasetName);
              if (exists) return prevArr;
              const entry = { value: body.datasetName, displayValue: body.datasetName };
              return [...prevArr, entry];
            });
          }
          if (typeof setSelectedDataset === "function" && body && body.datasetName) {
            setSelectedDataset(body.datasetName);
          }
          if (typeof setDataModel === "function" && body && (body.dataType || body.dataModel)) {
            setDataModel(body.dataType || body.dataModel);
          }
          if (typeof fetchMetadata === "function") fetchMetadata(true);
        }
      }
    } catch (err) {
      console.error(err);
      setResponseStatus(`error: ${err.message}`);
    }
  };

  return {
    handleCreate,
    handleAdd,
    handleUpdate,
    handleDelete,
    handleExport,
    handleFormSubmit,
    handleCreateSubmit,
  };
}
