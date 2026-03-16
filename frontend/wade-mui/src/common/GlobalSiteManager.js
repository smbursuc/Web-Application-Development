import React, { useEffect, useState } from 'react';
import { 
    Dialog, 
    DialogTitle, 
    DialogContent, 
    DialogContentText, 
    DialogActions, 
    Button, 
    Snackbar,
    Alert
} from '@mui/material';
import { getSiteMessage, checkHeartbeat } from './SiteInfoService';

const WATCHDOG_INTERVAL_MS = 10000; // Check every 10 seconds
const WATCHDOG_THRESHOLD = 3; // Number of failed checks before alerting

export default function GlobalSiteManager() {
    // Disclaimer State
    const [disclaimerOpen, setDisclaimerOpen] = useState(false);
    const [disclaimerText, setDisclaimerText] = useState("");

    // Watchdog State
    const [serverOffline, setServerOffline] = useState(false);
    const [failedChecks, setFailedChecks] = useState(0);

    // Initial Fetch - Disclaimer
    useEffect(() => {
        const fetchDisclaimer = async () => {
            // Check if user has already acknowledged in this session (optional, but good UX)
            // For now, per requirements "show immediately after entering", we show it always on reload
            const text = await getSiteMessage('disclaimer');
            if (text) {
                setDisclaimerText(text);
                setDisclaimerOpen(true);
            }
        };
        fetchDisclaimer();
    }, []);

    // Watchdog Timer
    useEffect(() => {
        const intervalId = setInterval(async () => {
            const isAlive = await checkHeartbeat();
            
            if (isAlive) {
                setFailedChecks(0);
                if (serverOffline) setServerOffline(false); // Recovered
            } else {
                setFailedChecks(prev => {
                    const newVal = prev + 1;
                    if (newVal >= WATCHDOG_THRESHOLD) {
                        setServerOffline(true);
                    }
                    return newVal;
                });
            }

        }, WATCHDOG_INTERVAL_MS);

        return () => clearInterval(intervalId);
    }, [serverOffline]);

    const handleDisclaimerClose = () => {
        setDisclaimerOpen(false);
    };

    const handleRetryConnection = () => {
        setFailedChecks(0);
        setServerOffline(false);
        // It will pick up again in the next interval, or we could force a check now
        checkHeartbeat().then(isAlive => {
            if (!isAlive) setServerOffline(true);
        });
    };

    return (
        <>
            {/* Disclaimer Dialog - Shows on startup */}
            <Dialog 
                open={disclaimerOpen} 
                onClose={handleDisclaimerClose}
                aria-labelledby="disclaimer-dialog-title"
                aria-describedby="disclaimer-dialog-description"
            >
                <DialogTitle id="disclaimer-dialog-title">
                    Important Notice
                </DialogTitle>
                <DialogContent>
                    <DialogContentText id="disclaimer-dialog-description">
                        {disclaimerText}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleDisclaimerClose} autoFocus>
                        I Understand
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Watchdog Offline Alert - Shows when server dies */}
            <Dialog
                open={serverOffline}
                aria-labelledby="server-offline-title"
                aria-describedby="server-offline-description"
                disableEscapeKeyDown
            >
                <DialogTitle id="server-offline-title" color="error">
                    Connection Lost
                </DialogTitle>
                <DialogContent>
                    <DialogContentText id="server-offline-description">
                        The connection to the WADe server has been lost. 
                        The application may not function correctly until the server is back online.
                        Please ensure the backend service is running.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleRetryConnection} color="primary">
                        Retry Connection
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
