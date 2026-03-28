package com.soloproductions.wade.service;

import com.soloproductions.wade.controller.ResponseStatus;
import com.soloproductions.wade.dataset.*;
import com.soloproductions.wade.dto.*;
import com.soloproductions.wade.entity.DatasetMetadataEntity;
import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.metadata.DatasetMetadata;
import com.soloproductions.wade.repository.DatasetMetadataRepository;
import com.soloproductions.wade.repository.UserDatasetRepository;
import com.soloproductions.wade.util.ApplicationContextProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central orchestration service for dataset request processing across all
 * dataset types and persistence backends.
 */
@Service
public class DatasetService
{
    /**
     * In-memory dataset cache.
     *
     * Structure: User -> DatasetName -> DatasetType -> DataType -> DatasetData.
     */
    private static Map<String, Map<String, Map<DatasetType, Map<DataType, DatasetData>>>> userDatasets = new ConcurrentHashMap<>();

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(DatasetService.class);

    /**
     * Resolves user-action service from application context.
     *
     * @return  user-action service bean
     */
    private static UserActionService getUserActionService()
    {
        return ApplicationContextProvider.getBean(UserActionService.class);
    }

    /**
     * Resolves user dataset repository from application context.
     *
     * @return  user dataset repository bean
     */
    private static UserDatasetRepository getUserDatasetRepository()
    {
        return ApplicationContextProvider.getBean(UserDatasetRepository.class);
    }

    /**
     * Resolves current username and falls back to {@code guest} for default datasets.
     *
     * @param   dataset
     *          dataset name being accessed
     *
     * @return  current username or guest fallback
     */
    private static String getUsernameWithGuestFallback(String dataset)
    {
        String username = getCurrentUsername();
        if (username == null && AbstractDatasetData.isDefaultDataset(dataset))
        {
            return "guest";
        }
        return username;
    }

    /**
     * Resolves the authenticated username from Spring Security context.
     *
     * @return  authenticated username or {@code null} when unavailable
     */
    public static String getCurrentUsername()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
        {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user)
            {
                return user.getUsername();
            }
            LOG.warn("Authentication found but principal type unknown: {}", principal.getClass().getName());
        }
        else
        {
            LOG.warn("No authentication found in SecurityContextHolder (or anonymous)");
        }
        return null;
    }

    /**
     * Clears all in-memory datasets for a user.
     *
     * @param   username
     *          user whose cache should be removed
     */
    public void clearUserDatasets(String username)
    {
        userDatasets.remove(username);
    }

    /**
     * Validates and executes a dataset request.
     *
     * @param   adr
     *          abstract dataset request
     *
     * @return  processed request result
     *
     * @throws  IOException
     *          when metadata/file-backed operations fail
     */
    public Object processRequest(AbstractDatasetRequest adr) throws IOException
    {
        if (adr.isDatasetMetadataRequest())
        {
            return handleDatasetMetadataRequest((DatasetMetadataRequest) adr);
        }

        if (adr.isMetadataRequest())
        {
            MetadataRequest mr = (MetadataRequest) adr;
            if (mr.isGeneralInfo())
            {
                DatasetMetadata dm = new DatasetMetadata();
                AbstractDatasetData.setMetadataStatic(dm);
                return dm;
            }
        }

        adr.validateParams();
        DatasetData createdDataset = null;
        Map<DataType, DatasetData> dataTypeMap = null;

        DataType dataType = AbstractDatasetData.resolveDataTypeFromString(adr.getDataType());
        String dataset = adr.getDatasetName();
        boolean isDefaultDataset = AbstractDatasetData.isDefaultDataset(dataset);
        DatasetType datasetType = AbstractDatasetData.resolveDatasetTypeFromString(adr.getDatasetType());

        String username = getUsernameWithGuestFallback(dataset);
        if (username == null)
        {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            LOG.error("Authentication failed for dataset: {}. Auth object: {}", dataset, auth);
            throw new IllegalStateException("User not authenticated");
        }

        try
        {
            String opType = "VIEW"; 
            if (AbstractDatasetRequest.isRequestType(adr, DatasetRequestType.CREATE)) 
            {
                opType = "CREATE";
            }
            else if (AbstractDatasetRequest.isRequestType(adr, DatasetRequestType.UPDATE)) 
            {
                opType = "UPDATE";
            }
            else if (AbstractDatasetRequest.isRequestType(adr, DatasetRequestType.DELETE)) 
            {
                opType = "DELETE";
            }
            
            if (adr instanceof ExportRequest) 
            {
                opType = "EXPORT";
            }
            
            String description = "Dataset " + opType.toLowerCase() + ": " + dataset + " [" + datasetType + "/" + dataType + "]";
            if (opType.equals("VIEW")) 
            {
                List<String> snapshotParts = new ArrayList<>();
                snapshotParts.add("dataset=" + URLEncoder.encode(dataset, StandardCharsets.UTF_8));
                snapshotParts.add("dataType=" + URLEncoder.encode(adr.getDataType(), StandardCharsets.UTF_8));

                if (adr.getSortDirection() != null && !adr.getSortDirection().isBlank()) 
                {
                    snapshotParts.add("sort=" + URLEncoder.encode(adr.getSortDirection(), StandardCharsets.UTF_8));
                }
                if (adr.getRange() != null) 
                {
                    snapshotParts.add("range=" + adr.getRange());
                }
                if (adr.getRangeStart() != null) 
                {
                    snapshotParts.add("rangeStart=" + adr.getRangeStart());
                }
                if (adr instanceof SimilarityDatasetRequest sdr && sdr.getSimilaritySortCriteria() != null && !sdr.getSimilaritySortCriteria().isBlank()) 
                {
                    snapshotParts.add("sortType=" + URLEncoder.encode(sdr.getSimilaritySortCriteria(), StandardCharsets.UTF_8));
                }
                description = description + " | snapshot: " + String.join("&", snapshotParts);
            }

            if (!username.equals("guest"))
            {
                UserActionService actionService = getUserActionService();
                actionService.logAction(username, opType, dataset, adr.getDatasetType(), description);
            }
        }
        catch (Exception e)
        {
            LOG.warn("Failed to log user action", e);
        }

        Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.computeIfAbsent(username, k -> new HashMap<>());

        if (!datasets.containsKey(dataset))
        {
            datasets.put(dataset, new HashMap<>());
        }

        Map<DatasetType, Map<DataType, DatasetData>> datasetData = datasets.get(dataset);
        if (!datasetData.containsKey(datasetType))
        {
            datasetData.put(datasetType, new HashMap<>());
        }

        dataTypeMap = datasetData.get(datasetType);

        if (!dataTypeMap.containsKey(dataType))
        {
            switch (datasetType)
            {
                case HEATMAP ->
                {
                    if (isDefaultDataset)
                    {
                        createdDataset = new SimilarityDataset(dataset, dataType);
                    }
                    else
                    {
                        SimilarityDataset sd = new SimilarityDataset();
                        sd.setDatasetName(dataset);
                        sd.setDataType(dataType);
                        createdDataset = sd;
                    }
                }
                case CLUSTERS ->
                {
                    if (isDefaultDataset)
                    {
                        createdDataset = new ClusterDataset(dataset, dataType);
                    }
                    else
                    {
                        ClusterDataset cd = new ClusterDataset();
                        cd.setDatasetName(dataset);
                        cd.setDataType(dataType);
                        createdDataset = cd;
                    }
                }
            }
            dataTypeMap.put(dataType, createdDataset);
        }


        Object response;
        DatasetData d = dataTypeMap.get(dataType);
        d.applyFilters(adr);

        if (adr.isMetadataRequest())
        {
            response = d.getMetadata();
        }
        else if (adr.isExportRequest())
        {
            ExportRequest exportRequest = (ExportRequest) adr;
            response = d.exportData(exportRequest);
        }
        else
        {
            response = d.executeRequest(adr);
        }

        if (!(response instanceof DatasetMetadata) && !(response instanceof DataEntity))
        {
            if (!(response instanceof List))
            {
                if (response instanceof Exception rEx)
                {
                    throw new RuntimeException(rEx.getCause());
                }
            }
        }

        return response;
    }

    /**
     * Resolves the dataset-metadata repository from the application context.
     *
     * @return   dataset-metadata repository bean
     */
    private static DatasetMetadataRepository getDatasetMetadataRepository()
    {
        return ApplicationContextProvider.getBean(DatasetMetadataRepository.class);
    }

    /**
     * Handles a {@link DatasetMetadataRequest}: reads or upserts a
     * {@link DatasetMetadataEntity} for the current user and dataset.
     *
     * <p>Dynamic datasets are user-owned, so this method requires an authenticated user.
     * No guest fallback is applied.</p>
     *
     * @param   r   
     *          the dataset-metadata request
     * 
     * @return  response DTO with {@code displayValue}, {@code summary}, and {@code source}
     *
     * @throws  IllegalStateException
     *          when user is not authenticated
     */
    private DatasetMetadataResponse handleDatasetMetadataRequest(DatasetMetadataRequest r)
    {
        String username = getCurrentUsername();
        if (username == null)
        {
            LOG.error("Attempted dataset-metadata access without authentication for dataset '{}'", r.getDatasetName());
            throw new IllegalStateException("User not authenticated. Dynamic dataset metadata requires authentication.");
        }

        String datasetName = r.getDatasetName();
        DatasetMetadataRepository repo = getDatasetMetadataRepository();
        boolean isPut = r.getRequestType().equalsIgnoreCase("put");

        DatasetMetadataEntity entity;
        if (isPut)
        {
            entity = repo.findByUsernameAndDatasetName(username, datasetName)
                         .orElseGet(() -> 
                         {
                            DatasetMetadataEntity e = new DatasetMetadataEntity();
                            e.setUsername(username);
                            e.setDatasetName(datasetName);
                            return e;
                         });

            if (r.getSummary() != null) 
            {
                entity.setSummary(r.getSummary());
            }

            if (r.getSource() != null) 
            {
                entity.setSource(r.getSource());
            }

            try
            {
                repo.save(entity);
            }
            catch (Exception e)
            {
                LOG.error("Failed to save dataset metadata for dataset '{}' and user '{}'", datasetName, username, e);
                throw e;
            }
        }
        else
        {
            entity = repo.findByUsernameAndDatasetName(username, datasetName).orElse(null);

            if (entity == null)
            {
                // Return empty defaults if metadata hasn't been persisted yet
                return new DatasetMetadataResponse("", "");
            }
        }

        return new DatasetMetadataResponse
        (
            entity.getSummary(),
            entity.getSource()
        );
    }

    /**
     * Handles request execution and wraps result into a standard API response.
     *
     * @param   adr
     *          abstract dataset request
     *
     * @return  standard response containing status, message, and data
     */
    public StandardResponse<Object> handleRequest(AbstractDatasetRequest adr)
    {
        String status = StandardResponse.resolveResponseStatusFromEnum(ResponseStatus.SUCCESS);
        String message = getMessage(adr, true, null);

        Object data = null;
        try
        {
            data = processRequest(adr);
        }
        catch (Exception e)
        {
            status = StandardResponse.resolveResponseStatusFromEnum(ResponseStatus.FAIL);
            boolean showException = e instanceof IllegalStateException || e instanceof UnsupportedOperationException ||
                    e.getClass() == RuntimeException.class || e instanceof IllegalArgumentException;
            String errorDetail = showException ? extractUserMessage(e) : null;
            message = getMessage(adr, false, errorDetail);
            LOG.error(message, e);
        }

        StandardResponse<Object> response = new StandardResponse<>(
                status,
                message,
                data
        );

        return response;
    }

    /**
     * Checks if a dataset exists for the current user context.
     *
     * @param   dataset
     *          dataset name
     *
     * @return  {@code true} when dataset exists
     */
    public static boolean containsDataset(String dataset)
    {
        String username = getUsernameWithGuestFallback(dataset);

        // Check in-memory
        Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get(username);
        if (datasets != null && datasets.get(dataset) != null)
        {
            return true;
        }

        // Check registry
        try
        {
            UserDatasetRepository repo = getUserDatasetRepository();
            if (repo != null)
            {
                return repo.existsByUsernameAndDatasetName(username, dataset);
            }
        }
        catch (Exception e)
        {
            LOG.error("Error checking registry for dataset: {}", dataset, e);
        }

        return false;
    }

    /**
     * Returns known dataset names for current user context plus defaults.
     *
     * @return  array of dataset names
     */
    public static String[] getDatasetNames()
    {
        String username = getCurrentUsername();
        Set<String> allNames = new HashSet<>(AbstractDatasetData.DATASETS);
        if (username != null)
        {
            Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get(username);
            if (datasets != null) allNames.addAll(datasets.keySet());

            // Add datasets from persistent storage (registry)
            try
            {
                UserDatasetRepository repo = getUserDatasetRepository();
                if (repo != null)
                {
                    allNames.addAll(repo.findDistinctDatasetNamesByUsername(username));
                }
            }
            catch (Exception e)
            {
                LOG.error("Failed to fetch persistent datasets for user {}: {}", username, e.getMessage());
            }
        }
        else
        {
            Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get("guest");
            if (datasets != null) 
            {
                allNames.addAll(datasets.keySet());
            }
        }
        return allNames.toArray(new String[0]);
    }

    /**
     * Returns dataset types available for a dataset in current user context.
     *
     * @param   dataset
     *          dataset name
     *
     * @return  available dataset types
     */
    public static Set<DatasetType> getDatasetTypes(String dataset)
    {
        Set<DatasetType> types = new HashSet<>();

        // Default datasets support both types by default (can be further refined if needed)
        if (AbstractDatasetData.isDefaultDataset(dataset))
        {
            types.add(DatasetType.HEATMAP);
            types.add(DatasetType.CLUSTERS);
        }

        String username = getUsernameWithGuestFallback(dataset);

        // Check in-memory
        Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get(username);
        Map<DatasetType, Map<DataType, DatasetData>> datasetInfo = datasets == null ? null : datasets.get(dataset);
        if (datasetInfo != null)
        {
            types.addAll(datasetInfo.keySet());
        }

        // Check persistent storage
        try
        {
            UserDatasetRepository repo = getUserDatasetRepository();
            if (repo != null)
            {
                List<String> persistentTypes = repo.findDatasetTypesByUsernameAndDatasetName(username, dataset);
                for (String t : persistentTypes)
                {
                    try
                    {
                        types.add(DatasetType.valueOf(t));
                    }
                    catch (Exception ex)
                    {
                        LOG.warn("Unknown dataset type in DB: {}", t);
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to fetch persistent dataset types for dataset {}: {}", dataset, e.getMessage());
        }

        return types;
    }

    /**
     * Returns persistence data types available for a dataset and dataset type.
     *
     * @param   dataset
     *          dataset name
     * @param   dt
     *          dataset type
     *
     * @return  available data type names
     */
    public static Set<String> getDatasetDataTypes(String dataset, DatasetType dt)
    {
        Set<String> dataTypes = new HashSet<>();

        // Default datasets support JSON and RDF by default
        if (AbstractDatasetData.isDefaultDataset(dataset))
        {
            dataTypes.add("JSON");
            dataTypes.add("RDF");
        }

        String username = getUsernameWithGuestFallback(dataset);

        // Check in-memory
        Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get(username);
        if (datasets != null)
        {
            Map<DatasetType, Map<DataType, DatasetData>> datasetInfo = datasets.get(dataset);
            if (datasetInfo != null && datasetInfo.containsKey(dt))
            {
                for (DataType dtIt : datasetInfo.get(dt).keySet())
                {
                    dataTypes.add(dtIt.name());
                }
            }
        }

        // Check persistent storage
        try
        {
            UserDatasetRepository repo = getUserDatasetRepository();
            if (repo != null)
            {
                List<String> persistentDataTypes = repo.findDataTypesByUsernameAndDatasetNameAndDatasetType(username, dataset, dt.name());
                dataTypes.addAll(persistentDataTypes);
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to fetch persistent data types for dataset {}: {}", dataset, e.getMessage());
        }

        return dataTypes;
    }

    /**
     * Resolves cached dataset data for a specific dataset/type/backend combination.
     *
     * @param   dataset
     *          dataset name
     * @param   datasetType
     *          dataset type
     * @param   dataType
     *          persistence data type
     *
     * @return  cached dataset data instance or {@code null}
     */
    public static DatasetData getDatasetData(String dataset, DatasetType datasetType, DataType dataType)
    {
        String username = getUsernameWithGuestFallback(dataset);
        Map<String, Map<DatasetType, Map<DataType, DatasetData>>> datasets = userDatasets.get(username);
        if (datasets == null) 
        {
            return null;
        }

        Map<DatasetType, Map<DataType, DatasetData>> datasetInfo = datasets.get(dataset);
        if (datasetInfo == null)
        {
            return null;
        }
        Map<DataType, DatasetData> datasetDataByDataTypes = datasetInfo.get(datasetType);
        if (datasetDataByDataTypes == null)
        {
            return null;
        }
        return datasetDataByDataTypes.get(dataType);
    }

    /**
     * Checks if cached dataset data exists for the specified key tuple.
     *
     * @param   dataset
     *          dataset name
     * @param   datasetType
     *          dataset type
     * @param   dataType
     *          persistence data type
     *
     * @return  {@code true} when dataset data exists
     */
    public static boolean existsDatasetData(String dataset, DatasetType datasetType, DataType dataType)
    {
        return getDatasetData(dataset, datasetType, dataType) != null;
    }

    /**
     * Builds a human-readable status message for request processing outcomes.
     *
     * @param   adr
     *          processed request
     * @param   success
     *          whether processing succeeded
     * @param   e
     *          optional exception to include in message context
     *
     * @return  formatted response message
     */
    /**
     * Strips any fully-qualified class prefix from an exception message so that
     * internal package names are never forwarded to the API consumer.
     *
     * <p>{@code RuntimeException(Throwable)} sets its message to {@code cause.toString()},
     * which includes the class name — e.g. {@code "com.example.Foo: bad input"}.
     * This helper returns only the relevant tail.
     *
     * @param   e
     *          exception whose message should be sanitised
     *
     * @return  clean message, or {@code null} when unavailable
     */
    private static String extractUserMessage(Exception e)
    {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank())
        {
            return null;
        }

        int sep = msg.indexOf(": ");
        if (sep > 0)
        {
            String prefix = msg.substring(0, sep);
            if (prefix.contains(".") || prefix.endsWith("Exception") || prefix.endsWith("Error"))
            {
                msg = msg.substring(sep + 2);
            }
        }
        return msg.isBlank() ? null : msg;
    }

    /**
     * Helper class for building an API response message.
     * 
     * @param   adr
     *          processed request
     * @param   success
     *          whether processing succeeded
     * @param   errorDetail
     *          optional error detail to include in message context
     * 
     * @return  formatted response message
     */
    private String getMessage(AbstractDatasetRequest adr, boolean success, String errorDetail)
    {
        String message = "Request for %s %s! Dataset name: %s, data type: %s, dataset type: %s.";
        String requestFor = "UNDEFINED";
        if (adr.isDatasetMetadataRequest())
        {
            String op = adr.getRequestType().equalsIgnoreCase("put") ? "saving" : "fetching";
            requestFor = op + " dataset metadata";
        }
        else if (adr.isMetadataRequest())
        {
            requestFor = "fetching metadata";
        }
        else if (adr.isSparqlQueryRequest())
        {
            requestFor = "executing SPARQL query";
        }
        else
        {
            switch (AbstractDatasetData.resolveRequestType(adr.getRequestType()))
            {
                case READ ->
                {
                    requestFor = "fetching";
                }
                case CREATE ->
                {
                    requestFor = "creating";
                }
                case UPDATE ->
                {
                    requestFor = "updating";
                }
                case DELETE ->
                {
                    requestFor = "deleting";
                }
            }
        }
        if (adr instanceof SimilarityDatasetRequest || adr instanceof ClusterDatasetRequest)
        {
            String ranges = String.format(" Ranges requested: range: %s, rangeStart: %s. ", adr.getRange(), adr.getRangeStart());
            message += ranges;
        }
        String requestFinish = success ? "completed successfully" : "failed";
        message = String.format(message,
                                requestFor,
                                requestFinish,
                                adr.getDatasetName(),
                                adr.getDataType(),
                                adr.getDatasetType());
        if (errorDetail != null)
        {
            message = message + " Message: " + errorDetail;
        }

        return message;
    }
}
