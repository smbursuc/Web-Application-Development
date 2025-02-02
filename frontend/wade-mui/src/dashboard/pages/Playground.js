import { alpha } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
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
import { useState } from "react";
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
      displayValue: "BSDS300"
    }
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

  const [dataType, setDataType] = useState("clusterData");
  const [query, setQuery] = useState("");

  // Predefined queries with descriptive labels
  const predefinedQueries = [
    {
      for: "clusterData",
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
      for: "clusterData",
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
      for: "heatmapData",
      label: "Get Heatmap (5 relations)",
      query: `
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
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
      for: "heatmapData",
      label: "Pairings With Similarity Greater Than 20%",
      query: `
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
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
    clusterData: `PREFIX ex: <http://example.org/ontology#>
SELECT ?cluster (AVG(?probability) AS ?avgProbability)
WHERE {
  ?cluster a ex:Cluster ;
           ex:hasChild ?child .
  ?child a ex:Object ;
         ex:hasProbability ?probability .
}
GROUP BY ?cluster
`,
    heatmapData: `PREFIX ex: <http://example.org/ontology#>
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

  const handleSubmit = async () => {
    try {
      const response = await fetch("http://localhost:3030/impr", {
        method: "POST",
        headers: {
          "Content-Type": "application/sparql-query",
        },
        body: query,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json(); // Change to response.json() if the server returns JSON
      setData(result);
    } catch (error) {
      console.error("Error submitting query:", error);
      alert("Failed to send query. Check console for details.");
    }
  };

  useEffect(() => {
    if (data !== null) {
      const columns = data.head.vars.map((varName) => ({
        field: varName,
        headerName: varName.toUpperCase(),
        flex: 1,
        renderCell: (params) => {
          const value = params.row[varName];
          if (value.type === "uri") {
            return (
              <a href={value.value} target="_blank" rel="noopener noreferrer">
                {value.value}
              </a>
            );
          }
          return value.value;
        },
      }));

      const rows = data.results.bindings.map((binding, index) => {
        const row = { id: index }; // Unique ID for each row
        data.head.vars.forEach((varName) => {
          row[varName] = binding[varName];
        });
        return row;
      });

      setColumns(columns);
      setRows(rows);
    }
    else {
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
          sx={{ width: "100%", maxWidth: { sm: "100%", md: "1400px" } }}
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
              <MenuItem value="clusterData">Cluster Data</MenuItem>
              <MenuItem value="heatmapData">Heatmap Data</MenuItem>
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
          <Box sx={{ height: 400, width: "100%" }}>
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
          </Box>
        </Container>
      </Box>
    </AppTheme>
  );
}
