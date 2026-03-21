import * as React from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import API_BASE_URL from '../../config';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Chip from '@mui/material/Chip';
import ListItemButton from '@mui/material/ListItemButton';
import { useAppState } from '../../contexts/AppStateContext';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import { Card, CardContent } from '@mui/material';

// Icons
import HistoryIcon from '@mui/icons-material/History';
import AssessmentIcon from '@mui/icons-material/Assessment';
import DatasetIcon from '@mui/icons-material/Dataset';
import InsertChartIcon from '@mui/icons-material/InsertChart';
import TouchAppIcon from '@mui/icons-material/TouchApp';

export default function DashboardContent() {
    const { user } = useAppState();
    const [history, setHistory] = React.useState([]);
    const [stats, setStats] = React.useState({
        totalActions: 0,
        datasetsCreated: 0,
        datasetsViewed: 0
    });
    const navigate = useNavigate();

    React.useEffect(() => {
        console.log('[DashboardContent] loading history + stats');
        // Fetch History
        fetch(`${API_BASE_URL}/api/history?limit=10`, { credentials: 'include' })
            .then(async (res) => {
                if (!res.ok) {
                    const body = await res.text();
                    console.warn('[DashboardContent] history request failed', { status: res.status, body });
                    return [];
                }
                const payload = await res.json();
                console.log('[DashboardContent] history payload', payload);
                return payload;
            })
            .then(data => {
                const items = Array.isArray(data)
                    ? data
                    : (Array.isArray(data?.data) ? data.data : []);
                console.log('[DashboardContent] parsed history items', items.length);
                setHistory(items);
            })
            .catch(err => console.error("History fetch error", err));

        // Fetch Stats
        fetch(`${API_BASE_URL}/api/history/stats`, { credentials: 'include' })
            .then(async (res) => {
                if (!res.ok) {
                    const body = await res.text();
                    console.warn('[DashboardContent] stats request failed', { status: res.status, body });
                    return {};
                }
                const payload = await res.json();
                console.log('[DashboardContent] stats payload', payload);
                return payload;
            })
            .then(data => {
                const statsPayload = data?.data && typeof data.data === 'object' ? data.data : data;
                setStats({
                    totalActions: statsPayload?.totalActions ?? 0,
                    datasetsCreated: statsPayload?.datasetsCreated ?? 0,
                    datasetsViewed: statsPayload?.datasetsViewed ?? 0,
                });
                console.log('[DashboardContent] parsed stats', statsPayload);
            })
            .catch(err => console.error("Stats fetch error", err));
    }, []);

    const handleHistoryClick = (item) => {
        // item contains: datasetId, datasetType (heatmap/clusters)
        // We navigate and pass query parameters so the target page can pre-select the dataset.
        const normalizedDatasetType = (item.datasetType || '').toLowerCase();
        const path = normalizedDatasetType === 'heatmap' ? '/correlations' : '/semantic-zoom';
        const description = item.description || '';

        const snapshotMarker = 'snapshot:';
        const snapshotIndex = description.indexOf(snapshotMarker);
        if (snapshotIndex >= 0) {
            const snapshotQuery = description.slice(snapshotIndex + snapshotMarker.length).trim();
            if (snapshotQuery.length > 0) {
                navigate(`${path}?${snapshotQuery}`);
                return;
            }
        }
        // Note: datasetType might be "clusters" or "heatmap". 
        // We know Correlations is for heatmap, and SemanticZoom is for clusters.
        
        // Extract datatype from description if possible, or just default to JSON for now.
        // The description was: "Dataset accessed: dsName [dstype/datatype]"
        let dataType = "json";
        if (item.description && item.description.includes("/")) {
            dataType = item.description.split("/").pop().replace("]", "");
        }

        navigate(`${path}?dataset=${item.datasetId}&dataType=${dataType}`); 
    };

    const getDisplayDescription = (description) => {
        const text = description || '';
        const marker = 'snapshot:';
        const index = text.indexOf(marker);
        if (index < 0) return text;
        return text.slice(0, index).trim();
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString();
    };

    return (
        <Box sx={{ width: '100%', maxWidth: { sm: '100%', md: '1700px' } }}>
             <Header />
            
            {/* Greeting */}
            <Box sx={{ mb: 4, mt: 2 }}>
                <Typography variant="h4" component="h1" gutterBottom color="text.primary">
                    Welcome back, {user?.displayName || user?.username || "Researcher"}!
                </Typography>
                <Typography variant="body1" color="text.secondary">
                    Here is an overview of your recent activity and workspace statistics.
                </Typography>
            </Box>

            <Grid container spacing={3}>
                {/* Stats Section */}
                <Grid item xs={12} md={4}>
                    <Paper elevation={2} sx={{ p: 3, height: '100%', borderRadius: 2 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                            <AssessmentIcon color="primary" sx={{ mr: 1 }} />
                            <Typography variant="h6">Your Stats</Typography>
                        </Box>
                        <Divider sx={{ mb: 2 }} />
                        
                        <Grid container spacing={2}>
                            <Grid item xs={12}>
                                <StatCard label="Total Actions" value={stats.totalActions} icon={<HistoryIcon />} />
                            </Grid>
                            <Grid item xs={6}>
                                <StatCard label="Datasets Created" value={stats.datasetsCreated} icon={<DatasetIcon />} />
                            </Grid>
                            <Grid item xs={6}>
                                <StatCard label="Datasets Viewed" value={stats.datasetsViewed} icon={<InsertChartIcon />} />
                            </Grid>
                        </Grid>
                    </Paper>
                </Grid>

                {/* History Section */}
                <Grid item xs={12} md={8}>
                    <Paper elevation={2} sx={{ p: 3, height: '100%', borderRadius: 2 }}>
                         <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                            <HistoryIcon color="secondary" sx={{ mr: 1 }} />
                            <Typography variant="h6">Quick Access / History</Typography>
                        </Box>
                        <Divider />
                        
                        {history.length === 0 ? (
                            <Typography sx={{ p: 2, fontStyle: 'italic' }}>No recent activity found.</Typography>
                        ) : (
                            <List>
                                {history.map((item) => (
                                    <React.Fragment key={item.id}>
                                        <ListItemButton onClick={() => handleHistoryClick(item)} alignItems="flex-start">
                                            <ListItemText
                                                primary={
                                                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                                        <Typography variant="subtitle1" component="span" fontWeight="500">
                                                            {getDisplayDescription(item.description)}
                                                        </Typography>
                                                        <Chip 
                                                            label={item.actionType} 
                                                            size="small" 
                                                            color={item.actionType === 'CREATE' ? 'success' : 'default'} 
                                                            variant="outlined" 
                                                        />
                                                    </Box>
                                                }
                                                secondary={
                                                    <React.Fragment>
                                                        <Typography
                                                            sx={{ display: 'inline' }}
                                                            component="span"
                                                            variant="body2"
                                                            color="text.primary"
                                                        >
                                                            {(item.datasetType || 'unknown').toUpperCase()} - {item.datasetId || '-'}
                                                        </Typography>
                                                        {" — " + formatDate(item.timestamp)}
                                                        <Box component="span" sx={{ display: 'flex', alignItems: 'center', mt: 0.5, color: 'primary.main', fontSize: '0.8rem' }}>
                                                            <TouchAppIcon fontSize="inherit" sx={{ mr: 0.5 }} /> Return to workspace
                                                        </Box>
                                                    </React.Fragment>
                                                }
                                            />
                                        </ListItemButton>
                                        <Divider component="li" />
                                    </React.Fragment>
                                ))}
                            </List>
                        )}
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
}

function StatCard({ label, value, icon }) {
    return (
        <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: 2, '&:last-child': { pb: 2 } }}>
                <Box sx={{ color: 'text.secondary', mb: 1 }}>{icon}</Box>
                <Typography variant="h4" component="div" fontWeight="bold">
                    {value}
                </Typography>
                <Typography variant="body2" color="text.secondary" align="center">
                    {label}
                </Typography>
            </CardContent>
        </Card>
    );
}
