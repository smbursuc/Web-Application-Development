import React from "react";
import Box from "@mui/material/Box";
import {Link} from "react-router-dom";

const ImprLogo = () => (
  <Link to="/" style={{ textDecoration: "none" }}>
    <Box
      component="img"
      src="/assets/Fireflies_logo.webp"
      alt="Fireflies Logo"
      sx={{
        width: "100px",
        height: "auto",
        cursor: "pointer", // Makes it visually clear it's clickable
      }}
    />
  </Link>
);

export default ImprLogo;
