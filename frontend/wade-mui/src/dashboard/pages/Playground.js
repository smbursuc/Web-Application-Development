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
      query: `PREFIX ex: <http://example.org/ontology#>
SELECT ?cluster ?child ?name ?probability ?uri
WHERE {
  ?cluster a ex:Cluster ;
           ex:hasChild ?child .
  ?child a ex:Object ;
         ex:hasName ?name ;
         ex:hasProbability ?probability ;
         ex:hasURI ?uri .
}
LIMIT 5
OFFSET 5`,
    },
    {
      for: "clusterData",
      label: "Average Cluster Probability Greater Than 20%",
      query: `PREFIX ex: <http://example.org/ontology#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?cluster ?child ?name ?probability ?uri
WHERE {
  ?cluster a ex:Cluster ;
           ex:hasChild ?child .
  ?child a ex:Object ;
         ex:hasName ?name ;
         ex:hasProbability ?probability ;
         ex:hasURI ?uri .
  FILTER (?probability > 0.2)
}
LIMIT 5
OFFSET 5
`,
    },
    {
      for: "heatmapData",
      label: "Get Heatmap (5 relations)",
      query: `PREFIX ex: <http://example.org/ontology#>

SELECT ?object1 ?object2 ?similarity
WHERE {
  ?object1 ex:hasSimilarity ?similarityNode .
  ?similarityNode ex:relatedTo ?object2 ;
                  ex:similarityValue ?similarity .
}
LIMIT 5
OFFSET 5
`,
    },
    {
      for: "heatmapData",
      label: "Pairings With Similarity Greater Than 20%",
      query: `PREFIX ex: <http://example.org/ontology#>
SELECT ?object1 ?object2 ?similarity
WHERE {
  ?object1 ex:hasSimilarity ?similarityNode .
  ?similarityNode ex:relatedTo ?object2 ;
                  ex:similarityValue ?similarity .
  FILTER (?similarity > 0.2)
}
ORDER BY DESC(?similarity)
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
    let query = filterQueries[dataType];
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
            features={features}
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
          <Box
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
          />
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
