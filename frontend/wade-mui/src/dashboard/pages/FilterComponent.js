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
  const sortType = props?.sortType;
  let max = props.max;

  const [filterOpen, setFilterOpen] = useState(false);
  const [maxRange, setMaxRange] = useState(0);

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
    let diff = max - rangeStartSliderValue;
    setMaxRange(diff > 50 ? 50 : diff);
  }, [rangeStartSliderValue])

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
            {options["sort"].values.map((value, index) => (
              <MenuItem key={value} value={value}>
                {options["sort"].descriptions[index]}
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
            max={max < 50 ? max : 50}
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
