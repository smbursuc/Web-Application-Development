import * as React from "react";
import Box from "@mui/material/Box";
import Container from "@mui/material/Container";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Grid from "@mui/material/Grid2";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardActions from "@mui/material/CardActions";
import Divider from "@mui/material/Divider";
import Chip from "@mui/material/Chip";
import AppTheme from "./shared-theme/AppTheme";
import CssBaseline from "@mui/material/CssBaseline";
import { useNavigate } from "react-router-dom";
import heroImage from "./haligtree.webp";
import ImprLogoHeader from "./marketing-page/components/ImprLogoHeader";

const apps = [
  {
    id: "impr",
    name: "IMPR",
    tagline: "Image Processing & Representation",
    description:
      "IMPR is a tool for semantically grouping images based on their visual content. It also allows the creation of personal datasets based on the same principle.",
    route: "/home",
    status: "Live",
  },
//   {
//     id: "wip1",
//     name: "Project Alpha",
//     tagline: "Coming soon",
//     description:
//       "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
//     route: null,
//     logo: null,
//     status: "In Progress",
//   },
//   {
//     id: "wip2",
//     name: "Project Beta",
//     tagline: "Coming soon",
//     description:
//       "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident.",
//     route: null,
//     logo: null,
//     status: "Planned",
//   },
];

export default function LandingPage(props) {
  const navigate = useNavigate();

  return (
    <AppTheme {...props}>
      <CssBaseline enableColorScheme />
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          flexDirection: "column",
        }}
      >
        {/* Hero / Header */}
        <Box
          sx={{
            background: (theme) =>
              theme.palette.mode === "dark"
                ? "linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)"
                : "linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 50%, #fce4ec 100%)",
            py: { xs: 6, md: 10 },
            textAlign: "center",
          }}
        >
          <Container maxWidth="md">
            <Typography
              variant="h2"
              component="h1"
              fontWeight={700}
              gutterBottom
              sx={{ fontSize: { xs: "2rem", sm: "3rem", md: "3.75rem" } }}
            >
              Hi!
            </Typography>
            <Typography
              variant="h5"
              color="text.secondary"
              sx={{ mb: 3, fontSize: { xs: "1rem", sm: "1.25rem", md: "1.5rem" } }}
            >
              Nice to see you!
            </Typography>
            <Typography
              variant="body1"
              color="text.secondary"
              sx={{ maxWidth: 640, mx: "auto", lineHeight: 1.8 }}
            >
              My name is Bursuc Serban-Mihai. This is the landing page for my personal portfolio. Below you can find the projects that I'm currently hosting. You can also check out 
              my <a href="https://github.com/smbursuc" target="_blank" rel="noopener noreferrer">GitHub page</a> to see more projects. Enjoy!
            </Typography>
          </Container>
        </Box>

        {/* Central image */}
        <Container maxWidth="sm" sx={{ textAlign: "center", py: { xs: 4, md: 6 } }}>
          <Box
            component="img"
            src={heroImage}
            alt="Application showcase"
            sx={{
              width: "100%",
              maxWidth: 220,
              height: "auto",
              borderRadius: 3,
              boxShadow: 6,
            }}
          />
            <Typography
              variant="h5"
              sx={{ mb: 3, fontSize: { xs: "1rem", sm: "1.25rem", md: "1.5rem" } }}
            >
              Elphael?...
            </Typography>
            <Typography
              variant="body1"
              color="text.secondary"
              sx={{ maxWidth: 640, mx: "auto", lineHeight: 1.8 }}
            >
              It's just an Elden Ring reference.
            </Typography>
        </Container>

        <Divider>
          <Chip label="Applications" size="small" />
        </Divider>

        {/* App list */}
        <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 }, flexGrow: 1 }}>
          <Typography
            variant="h4"
            fontWeight={600}
            textAlign="center"
            gutterBottom
            sx={{ mb: 4 }}
          >
            Projects
          </Typography>
          <Grid container spacing={3} justifyContent="center">
            {apps.map((app) => (
              <Grid key={app.id} size={{ xs: 12, sm: 6, md: 4 }}>
                <Card
                  variant="outlined"
                  sx={{
                    height: "100%",
                    display: "flex",
                    flexDirection: "column",
                    transition: "box-shadow 0.2s",
                    "&:hover": app.route ? { boxShadow: 6 } : {},
                  }}
                >
                  {app.id === "impr" ? (
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "center",
                        pt: 3,
                        pb: 1,
                      }}
                    >
                      <ImprLogoHeader />
                    </Box>
                  ) : app.logo ? (
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "center",
                        pt: 3,
                        pb: 1,
                      }}
                    >
                      <Box
                        component="img"
                        src={app.logo}
                        alt={app.name}
                        sx={{ height: 48, objectFit: "contain" }}
                      />
                    </Box>
                  ) : null}
                  <CardContent sx={{ flexGrow: 1 }}>
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                        mb: 0.5,
                      }}
                    >
                      <Typography variant="h6" fontWeight={600}>
                        {app.name}
                      </Typography>
                      <Chip
                        label={app.status}
                        size="small"
                        color={app.status === "Live" ? "success" : "default"}
                        variant="outlined"
                      />
                    </Box>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      display="block"
                      gutterBottom
                    >
                      {app.tagline}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {app.description}
                    </Typography>
                  </CardContent>
                  <CardActions sx={{ px: 2, pb: 2 }}>
                    <Button
                      variant={app.route ? "contained" : "outlined"}
                      disabled={!app.route}
                      fullWidth
                      onClick={() => {
                        if (app.route) {
                          window.scrollTo({ top: 0, behavior: 'auto' });
                          navigate(app.route);
                        }
                      }}
                    >
                      {app.route ? `Launch ${app.name}` : "Coming Soon"}
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>

        {/* Footer */}
        <Box
          component="footer"
          sx={{
            py: 3,
            textAlign: "center",
            borderTop: "1px solid",
            borderColor: "divider",
          }}
        >
          <Typography variant="body2" color="text.secondary">
            © {new Date().getFullYear()} Bursuc Serban-Mihai
          </Typography>
        </Box>
      </Box>
    </AppTheme>
  );
}
