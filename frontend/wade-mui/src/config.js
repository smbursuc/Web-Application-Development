// API base URL — set REACT_APP_API_BASE_URL in the appropriate .env file.
// Development (.env.development): http://localhost:8081
// Production  (.env.production):  (leave empty so requests use relative paths through nginx)
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

export default API_BASE_URL;
