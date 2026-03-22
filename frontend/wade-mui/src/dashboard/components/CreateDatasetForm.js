import * as React from "react";
import { Box, TextField, Button, FormControl, InputLabel, Select, MenuItem } from "@mui/material";

/**
 * Data types that support dataset creation.
 * Add a new entry here when a data type gains create/write support.
 */
export const CREATABLE_DATA_TYPES = [
  { value: "sql", label: "SQL" },
  // { value: "json", label: "JSON" },  // uncomment when JSON creation is implemented
  // { value: "rdf",  label: "RDF" },   // uncomment when RDF creation is implemented
];

export default function CreateDatasetForm(props) {
  const { initial = {}, onSubmit, onCancel, defaultDatasetType } = props;
  const [datasetName, setDatasetName] = React.useState(initial.datasetName || "");
  const [datasetType, setDatasetType] = React.useState(defaultDatasetType || initial.datasetType || "heatmap");
  const [dataType, setDataType] = React.useState(initial.dataType || CREATABLE_DATA_TYPES[0].value);

  const submit = (e) => {
    e.preventDefault();
    // creation now supports empty body on server; send only metadata
    onSubmit({ datasetName, datasetType, dataType });
  };

  return (
    <Box component="form" onSubmit={submit} sx={{ display: "flex", flexDirection: "column", gap: 2, width: 480 }}>
      <TextField label="Dataset Name" value={datasetName} onChange={(e) => setDatasetName(e.target.value)} required />

      <FormControl>
        <InputLabel id="dataset-type-label">Dataset Type</InputLabel>
        <Select labelId="dataset-type-label" value={datasetType} label="Dataset Type" onChange={(e) => setDatasetType(e.target.value)}>
          <MenuItem value="heatmap">heatmap</MenuItem>
          <MenuItem value="clusters">clusters</MenuItem>
        </Select>
      </FormControl>

      <FormControl>
        <InputLabel id="data-type-label">Data Type</InputLabel>
        <Select labelId="data-type-label" value={dataType} label="Data Type" onChange={(e) => setDataType(e.target.value)}>
          {CREATABLE_DATA_TYPES.map((dt) => (
            <MenuItem key={dt.value} value={dt.value}>{dt.label}</MenuItem>
          ))}
        </Select>
      </FormControl>

      <Box sx={{ display: "flex", gap: 1 }}>
        <Button type="submit" variant="contained">Create Dataset</Button>
        <Button variant="outlined" onClick={onCancel}>Cancel</Button>
      </Box>
    </Box>
  );
}
