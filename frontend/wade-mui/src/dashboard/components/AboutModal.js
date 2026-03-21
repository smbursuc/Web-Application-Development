import * as React from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Modal from '@mui/material/Modal';
import Button from '@mui/material/Button';
import { useAppState } from '../../contexts/AppStateContext';
import { getSiteMessage } from '../../common/SiteInfoService';
import { useState, useEffect } from 'react';
import ImprLogoHeader from '../../marketing-page/components/ImprLogoHeader';

const style = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 400,
  bgcolor: 'background.paper',
  color: 'text.primary',
  border: '1px solid',
  borderColor: 'divider',
  boxShadow: 24,
  p: 4,
  borderRadius: 2
};

export default function AboutModal() {
  const { aboutOpen, setAboutOpen } = useAppState();
  const [aboutText, setAboutText] = useState("Loading...");
  const [version, setVersion] = useState("");

  useEffect(() => {
    if (aboutOpen) {
        getSiteMessage("about").then(txt => setAboutText(txt || "Information unavailable."));
        getSiteMessage("version").then(ver => setVersion(ver || "Unknown"));
    }
  }, [aboutOpen]);

  const handleClose = () => setAboutOpen(false);

  return (
    <Modal
      open={aboutOpen}
      onClose={handleClose}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
    >
      <Box sx={style}>
        <ImprLogoHeader />
        <Typography id="modal-modal-title" variant="h6" component="h2" color="primary" mt={2} fontSize={20}>
          About IMPR
        </Typography>
        <Typography id="modal-modal-description" sx={{ mt: 2, whiteSpace: 'pre-wrap' }} color="text.primary" fontSize={12}>
          {aboutText}
        </Typography>
        
        <Typography variant="caption" display="block" sx={{ mt: 3, fontStyle: 'italic', color: 'text.secondary' }} fontSize={12}>
            Version: {version}
        </Typography>

        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
             <Button onClick={handleClose}>Close</Button>
        </Box>
      </Box>
    </Modal>
  );
}
