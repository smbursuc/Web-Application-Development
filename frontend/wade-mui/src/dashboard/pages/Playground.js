import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import Stack from "@mui/material/Stack";
import AppNavbar from "../components/AppNavbar";
import Header from "../components/Header";
import API_BASE_URL from '../../config';
import MainGrid from "../components/MainGrid";
import SideMenu from "../components/SideMenu";
import AppTheme from "../../shared-theme/AppTheme";
import {
  chartsCustomizations,
  dataGridCustomizations,
  datePickersCustomizations,
  treeViewCustomizations,
} from "../theme/customizations";
import { useState, useRef } from "react";
import {
  Container,
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Select,
  InputLabel,
  FormControl,
  CircularProgress,
} from "@mui/material";
import PageInfo from "./PageInfo";
import InfoModal from "./InfoModal";
import FilterComponent from "./FilterComponent";
import { DataGrid } from "@mui/x-data-grid";
import { useEffect } from "react";

const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

const CANCEL_AFTER_MS = 10000; // show cancel button after this many ms

export default function Playground(props) {
  const [infOpen, setInfOpen] = useState(false);
  const [selectedDataset, setSelectedDataset] = useState("cifar10"); // Default dataset
  const [sort, setSort] = useState("highest_probability");
  const [rangeSliderValue, setRangeSliderValue] = useState(5);
  const [rangeStartSliderValue, setRangeStartSliderValue] = useState(5);
  const [data, setData] = useState(null);
  const [rows, setRows] = useState([]);
  const [columns, setColumns] = useState([]);

  const options = {
    sort: {
      values: ["highest_probability", "lowest_probability", "no_sort"],
      descriptions: ["Highest probability", "Lowest probability", "No sorting"],
    },
  };

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

  const handleInfOpen = () => {
    setInfOpen(true);
  };

  const handleInfClose = () => {
    setInfOpen(false);
  };

  const handleDatasetChange = (event) => {
    setSelectedDataset(event.target.value);
  };

  const [dataType, setDataType] = useState("clusters");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [showCancel, setShowCancel] = useState(false);
  const abortControllerRef = useRef(null);
  const cancelTimeoutRef = useRef(null);

  // Predefined queries with descriptive labels
  const predefinedQueries = [
    {
      for: "clusters",
      label: "Get Cluster Data (5 clusters)",
      query: `
PREFIX ex: <http://example.org/ontology#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?clusterLabel ?predictedObjectLabel ?probability ?imageURI
WHERE {
  GRAPH <http://example.org/${selectedDataset}_clusters> {
    ?cluster a skos:Concept ;
             skos:prefLabel ?clusterLabel ;
             ex:hasPrediction ?prediction .
    ?prediction ex:hasProbability ?probability ;
                ex:hasURI ?imageURI ;
                ex:predictedObject ?predictedObject .
    ?predictedObject skos:prefLabel ?predictedObjectLabel .
  }
}
LIMIT 5

`,
    },
    {
      for: "clusters",
      label: "Average Cluster Probability Greater Than 90%",
      query: `PREFIX ex: <http://example.org/ontology#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?clusterLabel ?predictedObjectLabel ?probability ?imageURI
WHERE {
  GRAPH <http://example.org/${selectedDataset}_clusters> {
    ?cluster a skos:Concept ;
             skos:prefLabel ?clusterLabel ;
             ex:hasPrediction ?prediction .
    ?prediction ex:hasProbability ?probability ;
                ex:hasURI ?imageURI ;
                ex:predictedObject ?predictedObject .
    ?predictedObject skos:prefLabel ?predictedObjectLabel .
    FILTER (?probability > 0.9)
  }
}
LIMIT 5
`,
    },
    {
      for: "heatmap",
      label: "Get Heatmap (5 relations)",
      query: `PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX ex: <http://example.org/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?fromLabel ?toLabel ?similarityValue
WHERE {
  GRAPH <http://example.org/${selectedDataset}_heatmap> {
    ?sim ex:fromObject ?from .
    ?sim ex:toObject ?to .
    ?sim ex:hasCorrelationValue ?similarityValue .
    ?from a skos:Concept ;
          rdfs:label ?fromLabel .
    ?to a skos:Concept ;
          rdfs:label ?toLabel .
  }
}
LIMIT 5

`,
    },
    {
      for: "heatmap",
      label: "Pairings With Similarity Greater Than 20%",
      query: `PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX ex: <http://example.org/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?fromLabel ?toLabel ?similarityValue
WHERE {
  GRAPH <http://example.org/${selectedDataset}_heatmap> {
    ?sim ex:fromObject ?from .
    ?sim ex:toObject ?to .
    ?sim ex:hasCorrelationValue ?similarityValue .
    ?from a skos:Concept ;
          rdfs:label ?fromLabel .
    ?to a skos:Concept ;
          rdfs:label ?toLabel .
    FILTER(?similarityValue > 0.2)
  }
}
LIMIT 5`,
    },
  ];

  const filterQueries = {
    clusters: `PREFIX ex: <http://example.org/ontology#>
SELECT ?cluster (AVG(?probability) AS ?avgProbability)
WHERE {
  ?cluster a ex:Cluster ;
           ex:hasChild ?child .
  ?child a ex:Object ;
         ex:hasProbability ?probability .
}
GROUP BY ?cluster
`,
    heatmap: `PREFIX ex: <http://example.org/ontology#>
SELECT ?object1 ?object2 ?similarity
WHERE {
  ?object1 ex:hasSimilarity ?similarityNode .
  ?similarityNode ex:relatedTo ?object2 ;
                  ex:similarityValue ?similarity .
}
`,
  };

  const handleAutofill = (query) => {
    setQuery(query);
  };

  const handleApplyFilters = () => {
    let sortClause;
    if (sort !== "no_sort" && dataType === "clusterData") {
      if (sort === "highest_probability") {
        sortClause = "ORDER BY DESC(?avgProbability)";
      } else {
        sortClause = "ORDER BY ASC(?avgProbability)";
      }
    } else {
      sortClause = "";
    }

    const limitClause = rangeSliderValue;
    const offsetClause = rangeStartSliderValue;
    let query = predefinedQueries[dataType];
    query = query + (sortClause ? sortClause + "\n" : ""); // in case no-sort then don't include newline
    query = query + `LIMIT ${limitClause}\n`;
    query = query + `OFFSET ${offsetClause}`;
    setQuery(query);
  };

  const handleCancelRequest = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
  };

  const handleSubmit = async () => {
    if (cancelTimeoutRef.current) clearTimeout(cancelTimeoutRef.current);
    const controller = new AbortController();
    abortControllerRef.current = controller;
    setLoading(true);
    setShowCancel(false);

    cancelTimeoutRef.current = setTimeout(() => {
      setShowCancel(true);
    }, CANCEL_AFTER_MS);

    try {
      const url = `${API_BASE_URL}/api/${selectedDataset}/${dataType}/rdf?rawResults=true`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          query: query,
        }),
        credentials: "include",
        signal: controller.signal,
      });

      const result = await response.json();
      setData(result);
    } catch (error) {
      if (error.name !== "AbortError") {
        console.error("Error submitting query:", error);
        alert("Failed to send query. Check console for details.");
      }
    } finally {
      setLoading(false);
      setShowCancel(false);
      clearTimeout(cancelTimeoutRef.current);
    }
  };

  useEffect(() => {
    if (data !== null && data.data !== null && data.data.resultSet) {
      const resultSet = data.data.resultSet;

      if (resultSet.length === 0) {
        setColumns([]);
        setRows([]);
        return;
      }

      // Extract column names from keys of the first row
      const columnNames = Object.keys(resultSet[0]);

      const columns = columnNames.map((varName) => ({
        field: varName,
        headerName: varName.toUpperCase(),
        flex: 1,
        renderCell: (params) => {
          let value = params.row[varName];

          // Replace docker-internal hostnames so links work in a browser
          if (typeof value === "string") {
            value = value.replace(
              /https?:\/\/host\.docker\.internal(:\d+)?/g,
              API_BASE_URL || window.location.origin
            );
          }

          // Detect URI-like strings (http/https)
          if (typeof value === "string" && value.startsWith("http")) {
            return (
              <a href={value} target="_blank" rel="noopener noreferrer">
                {value}
              </a>
            );
          }
          return value;
        },
      }));

      const rows = resultSet.map((binding, index) => ({
        id: index,
        ...binding,
      }));

      setColumns(columns);
      setRows(rows);
    } else {
      setColumns([]);
      setRows([]);
    }
  }, [data]);

  const handleChange = (event) => {
    setQuery(event.target.value); // Update the query state with the input value
  };

  return (
    <AppTheme {...props} themeComponents={xThemeComponents}>
      <CssBaseline enableColorScheme />
      <Box sx={{ display: "flex" }}>
        <SideMenu />
        <AppNavbar />
        <Container
          sx={{ width: "100%", maxWidth: { sm: "100%", md: "1400px" }, pt: { xs: 8, md: 0 } }}
        >
          <PageInfo
            datasets={datasets}
            handleInfOpen={handleInfOpen}
            selectedDataset={selectedDataset}
            setSelectedDataset={setSelectedDataset}
            handleDatasetChange={handleDatasetChange}
          />
          <InfoModal
            infOpen={infOpen}
            handleInfClose={handleInfClose}
            selectedDataset={selectedDataset}
          />
          <Typography variant="h5" align="center" sx={{ my: 2 }}>
            SPARQL Query Builder
          </Typography>
          {/* Dropdown Selector */}
          <FormControl fullWidth sx={{}}>
            <InputLabel id="data-type-label">Data Type</InputLabel>
            <Select
              labelId="data-type-label"
              value={dataType}
              onChange={(e) => setDataType(e.target.value)}
            >
              <MenuItem value="clusters">Cluster Data</MenuItem>
              <MenuItem value="heatmap">Heatmap Data</MenuItem>
            </Select>
          </FormControl>
          <Box sx={{ display: "flex", justifyContent: "flex-start" }}>
            <Typography variant="h5" align="center" sx={{ my: 2 }}>
              Example Queries (click to obtain one)
            </Typography>
          </Box>
          {/* Autofill Buttons */}
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}>
            {predefinedQueries.map(
              (predef, index) =>
                predef.for === dataType && (
                  <Button
                    key={index}
                    variant="outlined"
                    onClick={() => handleAutofill(predef.query)}
                  >
                    {predef.label}
                  </Button>
                )
            )}
          </Box>
          {/* <Box
            sx={{
              display: "flex",
              justifyContent: "flex-start",
              flexDirection: "column",
              mt: 2,
            }}
          >
            <Typography variant="h5">
              Use filters to generate a custom query
            </Typography>
            <Typography variant="caption">
              Press "Filters", set the parameters and then press "Apply Filters"
            </Typography>
          </Box>
          <FilterComponent
            handleApplyFilters={handleApplyFilters}
            sort={sort}
            setSort={setSort}
            rangeSliderValue={rangeSliderValue}
            setRangeSliderValue={setRangeSliderValue}
            rangeStartSliderValue={rangeStartSliderValue}
            setRangeStartSliderValue={setRangeStartSliderValue}
            options={options}
          /> */}
          {/* Query Text Area */}
          <Box
            component="form"
            sx={{ "& .MuiTextField-root": { m: 1 } }}
            noValidate
            autoComplete="off"
          >
            <div>
              <TextField
                id="filled-multiline-static"
                label="SPARQL Query"
                multiline
                rows={20}
                placeholder="Write a SPARQL Query"
                value={query}
                variant="filled"
                fullWidth
                onChange={handleChange}
              />
            </div>
          </Box>
          {/* Submit Button */}
          <Button variant="contained" color="primary" onClick={handleSubmit}>
            Submit Query
          </Button>
          <Box sx={{ height: 400, width: "100%", position: "relative" }}>
            <Typography variant="h6" gutterBottom>
              SPARQL Results
            </Typography>
            <DataGrid
              rows={rows}
              columns={columns}
              pageSize={5}
              rowsPerPageOptions={[5, 10, 20]}
              disableSelectionOnClick
            />
            {loading && (
              <Box
                sx={{
                  position: "absolute",
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  backgroundColor: "rgba(0, 0, 0, 0.45)",
                  zIndex: 10,
                  gap: 2,
                  borderRadius: 1,
                }}
              >
                <CircularProgress size={52} />
                {showCancel && (
                  <Button
                    variant="contained"
                    color="error"
                    onClick={handleCancelRequest}
                  >
                    Cancel Request
                  </Button>
                )}
              </Box>
            )}
          </Box>
        </Container>
      </Box>
    </AppTheme>
  );
}
