import { styled } from '@mui/material/styles';
import Divider, { dividerClasses } from '@mui/material/Divider';
import Menu from '@mui/material/Menu';
import Alert from "@mui/material/Alert";
import MuiMenuItem from '@mui/material/MenuItem';
import { paperClasses } from '@mui/material/Paper';
import { listClasses } from '@mui/material/List';
import ListItemText from '@mui/material/ListItemText';
import ListItemIcon, { listItemIconClasses } from '@mui/material/ListItemIcon';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded';
import MenuButton from './MenuButton';
import { useAppState } from '../../contexts/AppStateContext';
import { useLogout } from '../../utils/logout';
import { useState, Fragment } from "react";
import { useNavigate } from 'react-router-dom';

const MenuItem = styled(MuiMenuItem)({
  margin: '2px 0',
});

export default function OptionsMenu() {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  const [responseMessage, setResponseMessage] = useState("");
  const appStateProps = useAppState();
  const loggedIn = appStateProps.loggedIn;
  const setLoggedIn = appStateProps.setLoggedIn;

  const logout = useLogout(setResponseMessage);
  const navigate = useNavigate();

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    handleClose();
  };

  const handleReturnToMain = () => {
    navigate('/');
    handleClose();
  };

  return (
    <Fragment>
      <MenuButton
        aria-label="Open menu"
        onClick={handleClick}
        sx={{ borderColor: 'transparent' }}
      >
        <MoreVertRoundedIcon />
      </MenuButton>
      <Menu
        anchorEl={anchorEl}
        id="menu"
        open={open}
        onClose={handleClose}
        onClick={handleClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        sx={{
          [`& .${listClasses.root}`]: {
            padding: '4px',
            minWidth: 160,
          },
          [`& .${paperClasses.root}`]: {
            padding: 0,
          },
          [`& .${dividerClasses.root}`]: {
            margin: '4px -4px',
          },
        }}
      >
        {/* <MenuItem onClick={handleClose}>Profile</MenuItem>
        <MenuItem onClick={handleClose}>My account</MenuItem>
        <Divider />
        <MenuItem onClick={handleClose}>Add another account</MenuItem>
        <MenuItem onClick={handleClose}>Settings</MenuItem>
        <Divider /> */}
        {loggedIn ? (
          <MenuItem
            onClick={handleLogout}
            sx={{
              pl: 2,
              [`& .${listItemIconClasses.root}`]: {
                ml: 'auto',
                minWidth: 0,
              },
            }}
          >
            <ListItemText>Logout</ListItemText>
            <ListItemIcon>
              <LogoutRoundedIcon fontSize="small" />
            </ListItemIcon>
          </MenuItem>
        ) : (
          <MenuItem
            onClick={handleReturnToMain}
            sx={{
              pl: 2,
              [`& .${listItemIconClasses.root}`]: {
                ml: 'auto',
                minWidth: 0,
              },
            }}
          >
            <ListItemText>Return to main page</ListItemText>
            <ListItemIcon>
              <HomeRoundedIcon fontSize="small" />
            </ListItemIcon>
          </MenuItem>
        )}
      </Menu>
    </Fragment>
  );
}
