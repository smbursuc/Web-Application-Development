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
import SemanticZoomModal from "./InfoModal";
import SortOptions from "../../common/managers/SortOptions";
import { filter } from "d3";

export default function FilterComponent(props) {
  const handleApplyFilters = props.handleApplyFilters;
  const sort = props.sort;
  const setSort = props.setSort;
  const rangeSliderValue = props.rangeSliderValue;
  const setRangeSliderValue = props.setRangeSliderValue;
  const rangeStartSliderValue = props.rangeStartSliderValue;
  const setRangeStartSliderValue = props.setRangeStartSliderValue;
  const options = props.options;
  const sortOptionsObj = new SortOptions(options, props.setSortOptions);
  const sortType = props?.sortType;
  let selectedDataset = props?.selectedDataset;
  let dataModel = props?.dataModel;
  let datasetType = props?.datasetType;

  // max possible range
  let max = props.max;

  // max possible range considering start value
  let maxRange = props.maxRange;
  const setMaxRange = props.setMaxRange;
  // server-supplied upper bound for the range slider (from metadata)
  const maxRangeConst = props.maxRangeConst || 100;

  const [filterOpen, setFilterOpen] = useState(false);

  const handleRangeSliderChange = (event, newValue) => {
    setRangeSliderValue(newValue);
  };

  const handleRangeStartSliderChange = (event, newValue) => {
    setRangeStartSliderValue(newValue);
  };

  const handleSortChange = (event) => {
    setSort(event.target.value);
  };

  const GetSortType = () => {
    if (!sortType) {
        return null;
    }

    return sortType;
  }

  useEffect(() => {
    // Only update max/range when a valid `max` (from metadata/cache) is provided.
    // If `max` is missing or not positive, leave existing values alone.
    const maxNum = Number(max);
    let diff;
    if (maxNum > 0) {
      diff = maxNum - rangeStartSliderValue;
      if (diff < 1) diff = 1; // Slider `min` is 1
      // Cap at the server-supplied MAX_RANGE constant
      const cappedDiff = Math.min(diff, maxRangeConst);
      // Keep current within [1, cappedDiff]
      const boundedCurrent = Math.max(1, Number(rangeSliderValue) || 1);
      const newVal = Math.min(boundedCurrent, cappedDiff);
      setRangeSliderValue(newVal);
      setMaxRange(cappedDiff);
    }
  }, [rangeStartSliderValue, max]);

  function getSortOptions () {
    const sortBy = sortOptionsObj.getSortBy(selectedDataset || "", datasetType || "");
    if (!Array.isArray(sortBy) || sortBy.length === 0) return [];
    return sortBy;
  }

  return (
    <Box>
      <Button
        variant="contained"
        onClick={() => setFilterOpen((prev) => !prev)}
        sx={{ my: 2 }}
      >
        Filters
      </Button>
      {filterOpen && <Box
        sx={{
          mb: 2,
          p: 2,
          bgcolor: "background.paper",
          boxShadow: 3,
          borderRadius: 2,
          width: "100%",
        }}
      >
        <Typography variant="h6" gutterBottom>
          Filter Options
        </Typography>

        {/* Checkboxes
                            {options.map((option) => (
                              <FormControlLabel
                                key={option.option}
                                control={
                                  <Checkbox
                                    checked={selectedCheckboxes.includes(option.option)}
                                    onChange={() => handleCheckboxChange(option.option)}
                                  />
                                }
                                label={option.description}
                              />
                            ))} */}

        <FormControl fullWidth>
          <InputLabel id="filter-select-label">Sort By</InputLabel>
          <Select
            labelId="filter-select-label"
            value={sort}
            onChange={handleSortChange}
            label="Sort by:"
          >
            {getSortOptions().map((value, index) => (
              <MenuItem 
              key={value["value"]} 
              value={value["value"]}>
                {value["displayValue"]}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {sort !== "no_sort" && <GetSortType></GetSortType>}

        {/* Number Slider */}
        <Box sx={{ mt: 3 }}>
          <Typography gutterBottom>Range</Typography>
          <Slider
            value={rangeSliderValue}
            onChange={handleRangeSliderChange}
            valueLabelDisplay="auto"
            min={1}
            max={maxRange}
          />
        </Box>

        {/* Number Slider */}
        <Box sx={{ mt: 3 }}>
          <Typography gutterBottom>Range Start</Typography>
          <Slider
            value={rangeStartSliderValue}
            onChange={handleRangeStartSliderChange}
            valueLabelDisplay="auto"
            min={1}
            max={Math.max(1, max - 1)}
          />
        </Box>

        {/* Apply Button */}
        <Button
          variant="contained"
          sx={{ mt: 2, width: "100%" }}
          onClick={handleApplyFilters}
        >
          Apply Filters
        </Button>
      </Box>}
    </Box>
  );
}
