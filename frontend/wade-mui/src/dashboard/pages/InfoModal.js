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

const xThemeComponents = {
  ...chartsCustomizations,
  ...dataGridCustomizations,
  ...datePickersCustomizations,
  ...treeViewCustomizations,
};

export default function InfoModal(props) {
  const infOpen = props.infOpen;
  const handleInfClose = props.handleInfClose;
  const selectedDataset = props.selectedDataset;

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

  return (
    <Modal
      open={infOpen}
      onClose={handleInfClose}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
    >
      <Box
        sx={{
          position: "absolute",
          top: "50%",
          left: "50%",
          transform: "translate(-50%, -50%)",
          bgcolor: "background.paper",
          border: "2px solid #000",
          boxShadow: 24,
          p: 4,
          borderRadius: 2,
        }}
      >
        <Typography id="modal-title" variant="h6" component="h2">
          CIFAR-10
        </Typography>
        <Typography id="modal-description" sx={{ mt: 2 }}>
          The CIFAR-10 dataset is a labeled subsets of the 80 million tiny
          images dataset.&nbsp;
          <a
            href="https://www.cs.toronto.edu/~kriz/cifar.html"
            target="_blank"
            rel="noopener noreferrer"
            style={{ textDecoration: "none" }}
          >
            Source
          </a>
        </Typography>
        <List>
          {features[selectedDataset].map((feature, index) => (
            <ListItem key={index} alignItems="flex-start" sx={{ gap: 1 }}>
              <ListItemIcon>
                <CircleIcon color="primary" />
              </ListItemIcon>
              <ListItemText
                primary={feature.title}
                secondary={
                  <Typography variant="body2" color="text.secondary">
                    {/* Split the description to bold specific parts */}
                    {feature.description.split(":").length > 1
                      ? feature.description.split(":")[0] + ":"
                      : feature.description.split(":")}
                    <Typography
                      component="span"
                      variant="body2"
                      fontWeight="bold"
                      color="text.primary"
                    >
                      {feature.description.split(":")[1]}
                    </Typography>
                  </Typography>
                }
              />
            </ListItem>
          ))}
        </List>
        <Button onClick={handleInfClose} variant="contained" sx={{ mt: 2 }}>
          Close
        </Button>
      </Box>
    </Modal>
  );
}
