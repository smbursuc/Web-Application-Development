import API_BASE_URL from '../config';

const BASE_URL = `${API_BASE_URL}/api/site-info`;

export const getSiteMessage = async (key) => {
    try {
        const response = await fetch(`${BASE_URL}/${key}`);
        if (!response.ok) {
            throw new Error(`Failed to fetch ${key}: ${response.statusText}`);
        }
        const data = await response.json();
        return data.content;
    } catch (error) {
        console.error("SiteInfoService Error:", error);
        return null;
    }
};

export const checkHeartbeat = async () => {
    try {
        const response = await fetch(`${BASE_URL}/heartbeat`);
        return response.ok;
    } catch (error) {
        return false;
    }
};
