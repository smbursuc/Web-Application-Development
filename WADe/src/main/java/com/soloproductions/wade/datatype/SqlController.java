package com.soloproductions.wade.datatype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dataset.*;
import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.DatasetBodyRequest;
import com.soloproductions.wade.dto.ExportRequest;
import com.soloproductions.wade.entity.ClusterEntity;
import com.soloproductions.wade.entity.HeatmapEntity;
import com.soloproductions.wade.metadata.DatasetMetadata;
import com.soloproductions.wade.pojo.DataEntry;
import com.soloproductions.wade.repository.ClusterRepository;
import com.soloproductions.wade.repository.HeatmapRepository;
import com.soloproductions.wade.util.ApplicationContextProvider;
import com.soloproductions.wade.service.DatasetService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Data type controller that reads and writes dataset data against a relational SQL
 * database through Spring Data JPA repositories. 
 * 
 * <p> Supports full CRUD operations. </p>
 */
public class SqlController extends AbstractDataTypeController
{
    /** Repository for persisting heatmap similarity pair entities. */
    private final HeatmapRepository heatmapRepository;

    /** Repository for persisting cluster node entities. */
    private final ClusterRepository clusterRepository;

    /** Jackson mapper used for payload deserialization. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(SqlController.class);

    /**
     * Returns the username of the currently authenticated user, falling back to
     * "guest" for unauthenticated access to default datasets.
     *
     * @return  current username, or guest" for unauthenticated default-dataset access
     */
    private String getUsername()
    {
        String name = DatasetService.getCurrentUsername();
        if (name == null && AbstractDatasetData.isDefaultDataset(getDatasetData().getDatasetName()))
        {
            return "guest";
        }
        return name;
    }

    /**
     * Constructor.
     *
     * @param   datasetData
     *          dataset model to operate on
     */
    public SqlController(DatasetData datasetData)
    {
        this(datasetData,
             ApplicationContextProvider.getBean(HeatmapRepository.class),
             ApplicationContextProvider.getBean(ClusterRepository.class));
    }

    /**
     * Constructs an SQL controller with explicit repository dependencies.
     * Primarily used for testing.
     *
     * @param   datasetData
     *          dataset model to operate on
     * @param   heatmapRepository
     *          repository for heatmap entities
     * @param   clusterRepository
     *          repository for cluster entities
     */
    public SqlController(DatasetData datasetData, HeatmapRepository heatmapRepository, ClusterRepository clusterRepository)
    {
        super(datasetData);
        this.heatmapRepository = heatmapRepository;
        this.clusterRepository = clusterRepository;
    }

    /**
     * Returns size metadata for the dataset by counting the rows owned by the current user.
     *
     * @return  dataset metadata containing the row count
     */
    @Override
    public DatasetMetadata getMetadata()
    {
        DatasetMetadata dm = new DatasetMetadata();
        DatasetType dt = getDatasetData().getDatasetType();
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        switch (dt)
        {
            case CLUSTERS ->
            {
                dm.setSize((int) clusterRepository.countByUsernameAndDatasetName(username, datasetName));
            }
            case HEATMAP ->
            {
                dm.setSize((int) heatmapRepository.countByUsernameAndDatasetName(username, datasetName));
            }
        }
        return dm;
    }

    /**
     * Executes a read request by fetching paginated rows from the database and
     * assembling the appropriate dataset model.
     *
     * @return  populated cluster node tree or similarity dataset
     *
     * @throws  UnsupportedOperationException
     *          when the dataset type is unrecognized
     */
    public Object executeReadRequest()
    {
        DatasetData dd = getDatasetData();
        DatasetType dt = dd.getDatasetType();
        String datasetName = dd.getDatasetName();
        String username = getUsername();
        Integer range = dd.getRange();
        Integer rangeStart = dd.getRangeStart();
        switch (dt)
        {
            case CLUSTERS ->
            {
                // build ClusterNode tree from DB rows
                ClusterDataset cd = (ClusterDataset) getDatasetData();
                List<ClusterEntity> rows;
                if (range != null && rangeStart != null)
                {
                    // PageRequest uses page index, so compute page and then trim if offset not page-aligned
                    int page = rangeStart / Math.max(1, range);
                    int trim = rangeStart % Math.max(1, range);
                    Pageable p = PageRequest.of(page, range);
                    List<ClusterEntity> pageRows = clusterRepository.findByUsernameAndDatasetName(username, datasetName, p);
                    if (trim > 0 && pageRows.size() > trim)
                    {
                        int toIndex = Math.min(pageRows.size(), trim + range);
                        rows = pageRows.subList(trim, toIndex);
                    }
                    else
                    {
                        rows = pageRows;
                    }
                }
                else
                {
                    rows = clusterRepository.findByUsernameAndDatasetName(username, datasetName);
                }
                ClusterNode root = (ClusterNode) buildClusterTree(rows, cd.getClusterName());
                return sortClusterChildren(root, cd.getSortDirection());
            }
            case HEATMAP ->
            {
                SimilarityDataset sd = (SimilarityDataset) getDatasetData();
                List<HeatmapEntity> rows;
                if (range != null && rangeStart != null)
                {
                    int page = rangeStart / Math.max(1, range);
                    int trim = rangeStart % Math.max(1, range);
                    Pageable p = PageRequest.of(page, range);
                    List<HeatmapEntity> pageRows = heatmapRepository.findByUsernameAndDatasetName(username, datasetName, p);
                    if (trim > 0 && pageRows.size() > trim)
                    {
                        int toIndex = Math.min(pageRows.size(), trim + range);
                        rows = pageRows.subList(trim, toIndex);
                    }
                    else
                    {
                        rows = pageRows;
                    }
                }
                else
                {
                    rows = heatmapRepository.findByUsernameAndDatasetName(username, datasetName);
                }
                SimilarityDataset rawSd = (SimilarityDataset) buildSimilarityFromRows(rows);
                int n = rawSd.getObjects().size();
                return sortAndSliceSimilarityMatrix(
                        rawSd.getObjects(), rawSd.getMatrix(),
                        sd.getSimilaritySortCriteria(), sd.getSortDirection(), 0, n);
            }
        }
        return null;
    }

    /**
     * Executes a create (POST) request by persisting new rows to the database.
     *
     * @param   adr
     *          request carrying the creation payload
     *
     * @return  list of saved entities
     */
    public Object executeCreateRequest(AbstractDatasetRequest adr)
    {
        DatasetData dd = getDatasetData();
        DatasetType dt = dd.getDatasetType();
        switch (dt)
        {
            case CLUSTERS ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                return createCluster(dbr);
            }
            case HEATMAP ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                return createHeatmap(dbr);
            }
        }
        return null;
    }

    /**
     * Executes an update (PUT) request by finding the target row either by explicit ID
     * or by matching the payload fields, then updating it in the database.
     *
     * @param   adr
     *          request carrying the update payload
     *
     * @return  updated entity
     *
     * @throws  UnsupportedOperationException
     *          when the dataset type is unrecognized
     * @throws  RuntimeException
     *          when no matching entity is found
     */
    public Object executeUpdateRequest(AbstractDatasetRequest adr)
    {
        DatasetData dd = getDatasetData();
        DatasetType dt = dd.getDatasetType();
        switch (dt)
        {
            case CLUSTERS ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                Object maybeId = extractIdFromBody(dbr);
                if (maybeId instanceof Number)
                {
                    Long id = ((Number) maybeId).longValue();
                    return updateCluster(id, dbr);
                }

                // otherwise find matching nodes and update the first one
                List<ClusterEntity> found = findClusterByPayload(dbr);
                if (!found.isEmpty())
                {
                    return updateCluster(found.get(0).getId(), dbr);
                }

                throw new RuntimeException("Update failed!");
            }
            case HEATMAP ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                Object maybeId = extractIdFromBody(dbr);
                if (maybeId instanceof Number)
                {
                    Long id = ((Number) maybeId).longValue();
                    return updateHeatmap(id, dbr);
                }
                List<HeatmapEntity> found = findHeatmapByPayload(dbr);
                if (!found.isEmpty())
                {
                    return updateHeatmap(found.get(0).getId(), dbr);
                }

                throw new RuntimeException("Update failed!");
            }
        }
        return null;
    }

    /**
     * Executes a delete (DELETE) request by finding the target row either by explicit ID
     * or by matching the payload fields, then removing it from the database.
     *
     * @param   adr
     *          request carrying the delete criteria
     *
     * @return  the entity as it existed immediately before deletion
     *
     * @throws  RuntimeException
     *          when no matching entity is found
     */
    @Override
    public Object executeDeleteRequest(AbstractDatasetRequest adr)
    {
        DatasetData dd = getDatasetData();
        DatasetType dt = dd.getDatasetType();
        switch (dt)
        {
            case CLUSTERS ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                Object maybeId = extractIdFromBody(dbr);
                if (maybeId instanceof Number)
                {
                    Long id = ((Number) maybeId).longValue();
                    ClusterEntity before = readCluster(id);
                    deleteCluster(id);
                    return before;
                }

                List<ClusterEntity> deleted = deleteClusterByPayload(dbr);
                if (deleted.isEmpty())
                {
                    throw new RuntimeException("Delete has failed!");
                }
                else
                {
                    // what if multiple are deleted?
                    // note: the return value here is not used, but I'm leaving it as Object
                    // in case it might be useful what was deleted?
                    return deleted.get(0);
                }
            }
            case HEATMAP ->
            {
                DatasetBodyRequest dbr = (DatasetBodyRequest) adr;
                Object maybeId = extractIdFromBody(dbr);
                if (maybeId instanceof Number)
                {
                    Long id = ((Number) maybeId).longValue();
                    HeatmapEntity before = readHeatmap(id);
                    deleteHeatmap(id);
                    return before;
                }
                List<HeatmapEntity> deleted = deleteHeatmapByPayload(dbr);
                if (deleted.isEmpty())
                {
                    throw new RuntimeException("Delete has failed!");
                }
                else
                {
                    // what if multiple are deleted?
                    // note: the return value here is not used, but I'm leaving it as Object
                    // in case it might be useful what was deleted?
                    return deleted.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Persists one or more heatmap similarity pairs from the request payload.
     * Upserts existing pairs (same object pair) and registers the dataset.
     *
     * @param   dbr
     *          request body containing a {@code pairs} list with object and similarity fields
     *
     * @return  list of saved {@link HeatmapEntity} objects, or an {@link Exception} on failure
     */
    public Object createHeatmap(DatasetBodyRequest dbr)
    {
        try
        {
            String datasetName = getDatasetData().getDatasetName();
            String username = getUsername();

            // Always register the dataset, even if empty
            registerDatasetMetadata(username, datasetName, DatasetType.HEATMAP);

            // Accept empty or missing payload as a no-op (idempotent)
            if (dbr == null || dbr.getEntry() == null || dbr.getEntry().getData() == null) 
            {
                return List.of();
            }

            Map<String, Object> data = (Map<String, Object>) dbr.getEntry().getData();
            if (data == null) 
            {
                return List.of();
            }

            List<Map<String, Object>> payload = (List<Map<String, Object>>) data.get("pairs");
            if (payload == null || payload.isEmpty()) 
            {
                return List.of();
            }

            List<HeatmapEntity> entities = new ArrayList<>();
            for (Map<String, Object> row : payload)
            {
                String o1 = extractString(row, "object1");
                String o2 = extractString(row, "object2");
                Double sim = extractDouble(row, "similarity");
                if (o1 == null || o2 == null || sim == null)
                {
                    continue;
                }

                // canonicalize pair order so (A,B) == (B,A)
                String[] pair = canonicalPair(o1, o2);
                String co1 = pair[0];
                String co2 = pair[1];

                List<HeatmapEntity> existing = heatmapRepository.findByUsernameAndDatasetNameAndObject1AndObject2(username, datasetName, co1, co2);
                if (!existing.isEmpty())
                {
                    HeatmapEntity ex = existing.get(0);
                    ex.setSimilarity(sim);
                    entities.add(heatmapRepository.save(ex));
                }
                else
                {
                    HeatmapEntity e = new HeatmapEntity();
                    e.setUsername(username);
                    e.setDatasetName(datasetName);
                    e.setObject1(co1);
                    e.setObject2(co2);
                    e.setSimilarity(sim);
                    entities.add(heatmapRepository.save(e));
                }
            }
            return entities;
        }
        catch (Exception e)
        {
            LOG.error("Error creating heatmap", e);
            return e;
        }
    }

    /**
     * Retrieves a heatmap entity by its database ID, verifying ownership.
     *
     * @param   id
     *          primary key of the heatmap entity
     *
     * @return  matching {@link HeatmapEntity}, or {@code null} when not found or not owned by the current user
     */
    public HeatmapEntity readHeatmap(Long id)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        return heatmapRepository.findById(id)
                                .filter(e -> e.getUsername().equals(username) && e.getDatasetName().equals(datasetName))
                                .orElse(null);
    }

    /**
     * Returns all heatmap entities belonging to the current user and active dataset.
     *
     * @return  list of heatmap entities
     */
    public List<HeatmapEntity> listHeatmapsForDataset()
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        return heatmapRepository.findByUsernameAndDatasetName(username, datasetName);
    }

    /**
     * Finds heatmap entities that match the object pairs described in the request payload.
     *
     * @param   dbr
     *          request body carrying the match criteria
     *
     * @return  list of matching heatmap entities
     */
    public List<HeatmapEntity> findHeatmapByPayload(DatasetBodyRequest dbr)
    {
        if (dbr == null || dbr.getEntry() == null || dbr.getEntry().getData() == null) 
        {
            return List.of();
        }

        String datasetName = getDatasetData().getDatasetName();
        String username = getUsername();
        List<HeatmapEntity> result = new ArrayList<>();

        Object payload = dbr.getEntry().getData();
        Map<String, Object> map = objectMapper.convertValue(payload, Map.class);

        // Payload with explicit list of pairs
        if (map != null && map.containsKey("pairs"))
        {
            List<Map<String, Object>> pairs = (List<Map<String, Object>>) map.get("pairs");
            if (pairs != null)
            {
                for (Map<String, Object> item : pairs)
                {
                    String o1 = extractString(item, "object1");
                    String o2 = extractString(item, "object2");
                    if (o1 == null || o2 == null) 
                    {
                        continue;
                    }

                    result.addAll(heatmapRepository.findByUsernameAndDatasetNameAndObject1AndObject2(username, datasetName, o1, o2));
                }
            }
            return result;
        }

        // Single-pair style
        if (map != null)
        {
            String o1 = extractString(map, "object1");
            String o2 = extractString(map, "object2");
            Double sim = extractDouble(map, "similarity");
            if (o1 == null || o2 == null) return result;

            String[] pair = canonicalPair(o1, o2);
            String co1 = pair[0];
            String co2 = pair[1];

            if (sim != null)
            {
                result.addAll(heatmapRepository.findByUsernameAndDatasetNameAndObject1AndObject2AndSimilarity(username, datasetName, co1, co2, sim));
            }
            else
            {
                result.addAll(heatmapRepository.findByUsernameAndDatasetNameAndObject1AndObject2(username, datasetName, co1, co2));
            }
        }
        return result;
    }

    /**
     * Deletes all heatmap entities that match the object pairs in the request payload.
     *
     * @param   dbr
     *          request body carrying the match criteria
     *
     * @return  list of deleted heatmap entities
     */
    public List<HeatmapEntity> deleteHeatmapByPayload(DatasetBodyRequest dbr)
    {
        List<HeatmapEntity> toDelete = findHeatmapByPayload(dbr);
        for (HeatmapEntity e : toDelete)
        {
            heatmapRepository.deleteById(e.getId());
        }
        return toDelete;
    }

    /**
     * Updates an existing heatmap entity identified by its database ID.
     *
     * @param   id
     *          primary key of the heatmap entity to update
     * @param   dbr
     *          request body carrying the new field values
     *
     * @return  the saved {@link HeatmapEntity} after the update
     *
     * @throws  IllegalArgumentException
     *          when the entity is not found or is not owned by the current user
     */
    public Object updateHeatmap(Long id, DatasetBodyRequest dbr)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        HeatmapEntity e = heatmapRepository.findById(id)
                                           .filter(ent -> ent.getUsername().equals(username) && ent.getDatasetName().equals(datasetName))
                                           .orElseThrow(() -> new IllegalArgumentException("Heatmap not found or access denied: " + id));

        if (dbr.getEntry() != null && dbr.getEntry().getData() != null)
        {
            Object payload = dbr.getEntry().getData();
            Map<String, Object> map = objectMapper.convertValue(payload, Map.class);

            // Support payloads with a top-level "pairs" array (e.g. {"pairs": [{...}]})
            if (map != null && map.containsKey("pairs") && map.get("pairs") instanceof List)
            {
                List<?> pairs = (List<?>) map.get("pairs");
                if (!pairs.isEmpty() && pairs.get(0) instanceof Map)
                {
                    // use the first pair object as the map for updating
                    map = (Map<String, Object>) pairs.get(0);
                }
            }

            String o1 = extractString(map, "object1");
            String o2 = extractString(map, "object2");
            Double sim = extractDouble(map, "similarity");
            if (o1 != null && o2 != null)
            {
                String[] pair = canonicalPair(o1, o2);
                e.setObject1(pair[0]);
                e.setObject2(pair[1]);
            }
            else
            {
                if (o1 != null)
                {
                    e.setObject1(o1);
                }
                if (o2 != null)
                {
                    e.setObject2(o2);
                }
            }
            if (sim != null)
            {
                e.setSimilarity(sim);
            }
        }
        else
        {
            String message = String.format("The payload for this request is empty! %s, %s",
                    dbr.getRequestType(),
                    dbr.getDatasetName());
            LOG.warn(message);
            return new RuntimeException(message);
        }
        return heatmapRepository.save(e);
    }

    /**
     * Persists a cluster node tree from the request payload, supporting both
     * flat lists and nested hierarchies. Idempotent: existing nodes are updated.
     *
     * @param   dbr
     *          request body containing the cluster node tree
     *
     * @return  list of saved {@link ClusterEntity} objects, or an {@link Exception} on failure
     */
    private Object createCluster(DatasetBodyRequest dbr)
    {
        try
        {
            String datasetName = getDatasetData().getDatasetName();
            String username = getUsername();

            // Always register the dataset
            registerDatasetMetadata(username, datasetName, DatasetType.CLUSTERS);

            LOG.info("Starting cluster creation for dataset: {}", datasetName);
            DataEntry payload = dbr.getEntry();
            if (payload == null || payload.getData() == null)
            {
                LOG.warn("CREATE request with no payload.");
                return new ArrayList<>();
            }

            Object payloadData = payload.getData();
            List<Object> nodesToProcess = new ArrayList<>();
            if (payloadData instanceof List l)
            {
                nodesToProcess.addAll(l);
            }
            else
            {
                nodesToProcess.add(payloadData);
            }

            List<ClusterEntity> entitiesToSave = new ArrayList<>();
            Set<String> uniqueKeysInPayload = new HashSet<>();

            for (Object nodeObj : nodesToProcess)
            {
                processClusterNode(nodeObj, "Root", entitiesToSave, username, datasetName, uniqueKeysInPayload);
            }

            // Fetch existing entities to support idempotency/updates
            List<ClusterEntity> existingDB = clusterRepository.findByUsernameAndDatasetName(username, datasetName);
            Map<String, ClusterEntity> existingMap = new HashMap<>();
            for (ClusterEntity ex : existingDB)
            {
                String key = ex.getNodeName() + "::" + (ex.getParentName() == null ? "null" : ex.getParentName());
                existingMap.put(key, ex);
            }

            List<ClusterEntity> resultList = new ArrayList<>();
            for (ClusterEntity e : entitiesToSave)
            {
                String key = e.getNodeName() + "::" + (e.getParentName() == null ? "null" : e.getParentName());
                if (existingMap.containsKey(key))
                {
                    ClusterEntity ex = existingMap.get(key);
                    ex.setProbability(e.getProbability());
                    ex.setUri(e.getUri());
                    ex.setClusterName(e.getClusterName());
                    resultList.add(clusterRepository.save(ex));
                }
                else
                {
                    resultList.add(clusterRepository.save(e));
                }
            }

            LOG.info("Processed {} entities for user {}.", resultList.size(), username);
            return resultList;
        }
        catch (Exception e)
        {
            LOG.error("Error creating cluster", e);
            return e;
        }
    }

    /**
     * Recursively processes a cluster node object from the payload and adds a
     * corresponding {@link ClusterEntity} to the collector list.
     *
     * @param   nodeObj
     *          raw node object deserialized from the payload
     * @param   msgParentName
     *          parent name to fall back to when the node does not carry one
     * @param   collector
     *          accumulator list for built entities
     * @param   username
     *          owner of the dataset
     * @param   datasetName
     *          name of the dataset
     * @param   uniqueNames
     *          set used to detect duplicate node-parent combinations in the payload
     *
     * @throws  IllegalArgumentException
     *          when a duplicate cluster name is detected within the same payload
     */
    private void processClusterNode(Object nodeObj, 
                                    String msgParentName, 
                                    List<ClusterEntity> collector, 
                                    String username, String datasetName, 
                                    Set<String> uniqueNames)
    {
        Map<String, Object> node = objectMapper.convertValue(nodeObj, Map.class);
        String name = (String) node.get("name");
        String parent = (String) node.getOrDefault("parent", msgParentName);
        List<Object> children = (List) node.get("children");

        // Special case: Wrapper object without name but with children -> infer Root parent for children
        if (name == null && children != null)
        {
            for (Object child : children)
            {
                processClusterNode(child, "Root", collector, username, datasetName, uniqueNames);
            }
            return;
        }

        if (name == null) 
        {
            return;
        }

        // Check duplicates within the payload (scoped by parent to allow same name in diff branches)
        String uniqueKey = name + "::" + (parent == null ? "null" : parent);
        if (!uniqueNames.add(uniqueKey))
        {
            throw new IllegalArgumentException("Duplicate cluster name in payload: " + name);
        }

        ClusterEntity entity = new ClusterEntity();
        entity.setUsername(username);
        entity.setDatasetName(datasetName);
        entity.setNodeName(name);

        // If we are "Root", parent is null
        if ("Root".equalsIgnoreCase(name))
        {
            entity.setParentName(null);
            entity.setClusterName(name); // Root is its own cluster
        }
        else
        {
            entity.setParentName(parent);
            boolean isLeaf = node.containsKey("Probability") || node.containsKey("URI");
            if (isLeaf)
            {
                entity.setClusterName(parent); // Leaf nodes belong to parent cluster
                Double prob = extractDouble(node, "Probability");
                String uri = extractString(node, "URI");
                entity.setProbability(prob);
                entity.setUri(uri);
            }
            else
            {
                entity.setClusterName(name);
            }
        }

        collector.add(entity);

        if (children != null)
        {
            for (Object child : children)
            {
                processClusterNode(child, name, collector, username, datasetName, uniqueNames);
            }
        }
    }

    /**
     * Retrieves a cluster entity by its database ID, verifying ownership.
     *
     * @param   id
     *          primary key of the cluster entity
     *
     * @return  matching {@link ClusterEntity}, or {@code null} when not found or not owned by the current user
     */
    public ClusterEntity readCluster(Long id)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        return clusterRepository.findById(id)
                                .filter(e -> e.getUsername().equals(username) && e.getDatasetName().equals(datasetName))
                                .orElse(null);
    }

    /**
     * Returns all cluster entities belonging to the current user and active dataset.
     *
     * @return  list of cluster entities
     */
    public List<ClusterEntity> listClustersForDataset()
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        return clusterRepository.findByUsernameAndDatasetName(username, datasetName);
    }

    /**
     * Finds cluster entities that match the nodes described in the request payload.
     *
     * @param   dbr
     *          request body carrying the match criteria
     *
     * @return  list of matching cluster entities
     */
    private List<ClusterEntity> findClusterByPayload(DatasetBodyRequest dbr)
    {
        if (dbr == null || dbr.getEntry() == null || dbr.getEntry().getData() == null)
        {
            return List.of();
        }

        String datasetName = getDatasetData().getDatasetName();
        String username = getUsername();
        List<ClusterEntity> result = new ArrayList<>();
        Object payload = dbr.getEntry().getData();
        List<Object> nodes = new ArrayList<>();
        if (payload instanceof List)
        {
            nodes.addAll((List) payload);
        }
        else
        {
            Map<String, Object> root = objectMapper.convertValue(payload, Map.class);
            if (root == null)
            {
                return List.of();
            }
            if (root.containsKey("children") && root.get("children") instanceof List)
            {
                nodes.add(root);
                nodes.addAll((List) root.get("children"));
            }
            else
            {
                nodes.add(root);
            }
        }

        for (Object item : nodes)
        {
            Map<String, Object> map = objectMapper.convertValue(item, Map.class);
            String node = extractString(map, "name");
            String parent = extractString(map, "parent");
            if (node == null)
            {
                continue;
            }
            if (parent != null)
            {
                result.addAll(clusterRepository.findByUsernameAndDatasetNameAndNodeNameAndParentName(username, datasetName, node, parent));
            }
            else
            {
                result.addAll(clusterRepository.findByUsernameAndDatasetNameAndNodeName(username, datasetName, node));
            }
        }
        return result;
    }

    /**
     * Deletes all cluster entities that match the nodes in the request payload.
     *
     * @param   dbr
     *          request body carrying the match criteria
     *
     * @return  list of deleted cluster entities
     */
    public List<ClusterEntity> deleteClusterByPayload(DatasetBodyRequest dbr)
    {
        List<ClusterEntity> toDelete = findClusterByPayload(dbr);
        for (ClusterEntity e : toDelete)
        {
            deleteClusterRecursive(e);
        }
        return toDelete;
    }

    /**
     * Recursively deletes a cluster entity and all its descendants.
     *
     * @param   entity
     *          root entity to delete along with is children
     */
    private void deleteClusterRecursive(ClusterEntity entity)
    {
        String username = entity.getUsername();
        String datasetName = entity.getDatasetName();
        String nodeName = entity.getNodeName();

        // Recursively find and delete all children
        List<ClusterEntity> children = clusterRepository.findByUsernameAndDatasetNameAndParentName(username, datasetName, nodeName);
        for (ClusterEntity child : children)
        {
            deleteClusterRecursive(child);
        }

        clusterRepository.delete(entity);
    }

    /**
     * Deletes a heatmap entity by its database ID, verifying ownership.
     *
     * @param   id
     *          primary key of the heatmap entity to delete
     */
    public void deleteHeatmap(Long id)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        heatmapRepository.findById(id)
                         .ifPresent(e ->
        {
            if (e.getUsername().equals(username) && e.getDatasetName().equals(datasetName))
            {
                heatmapRepository.delete(e);
            }
        });
    }

    /**
     * Updates an existing cluster entity identified by its database ID.
     *
     * @param   id
     *          primary key of the cluster entity to update
     * @param   dbr
     *          request body carrying the new field values
     *
     * @return  the saved {@link ClusterEntity} after the update
     *
     * @throws  IllegalArgumentException
     *          when the entity is not found or is not owned by the current user
     */
    public ClusterEntity updateCluster(Long id, DatasetBodyRequest dbr)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        ClusterEntity e = clusterRepository.findById(id)
                                           .filter(ent -> ent.getUsername().equals(username) && ent.getDatasetName().equals(datasetName))
                                           .orElseThrow(() -> new IllegalArgumentException("Cluster not found or access denied: " + id));

        if (dbr != null && dbr.getEntry() != null && dbr.getEntry().getData() != null)
        {
            Object payload = dbr.getEntry().getData();
            Object item = null;
            if (payload instanceof List && !((List) payload).isEmpty())
            {
                item = ((List) payload).get(0);
            }
            else
            {
                item = payload;
            }

            Map<String, Object> map = objectMapper.convertValue(item, Map.class);
            String node = extractString(map, "name");
            String parent = extractString(map, "parent");
            Double prob = extractDouble(map, "probability");
            String uri = extractString(map, "uri");
            if (node != null)
            {
                e.setNodeName(node);
            }
            if (parent != null)
            {
                e.setParentName(parent);
            }
            if (prob != null)
            {
                e.setProbability(prob);
            }
            if (uri != null)
            {
                e.setUri(uri);
            }
        }
        return clusterRepository.save(e);
    }

    /**
     * Deletes a cluster entity and its descendants by ID, verifying ownership.
     *
     * @param   id
     *          primary key of the root cluster entity to delete
     */
    public void deleteCluster(Long id)
    {
        String username = getUsername();
        String datasetName = getDatasetData().getDatasetName();
        clusterRepository.findById(id)
                         .ifPresent(e ->
        {
            if (e.getUsername().equals(username) && e.getDatasetName().equals(datasetName))
            {
                deleteClusterRecursive(e);
            }
        });
    }

    /**
     * Extracts a string value from the map by key.
     *
     * @param   map
     *          source map
     * @param   key
     *          key to look up
     *
     * @return  value found, or {@code null} when not present
     */
    private String extractString(Map<String, Object> map, String key)
    {
        if (map == null || !map.containsKey(key) || map.get(key) == null)
        {
            return null;
        }

        return String.valueOf(map.get(key));
    }

    /**
     * Extracts a numeric value from the map by key.
     *
     * @param   map
     *          source map
     * @param   key
     *          key to look up
     *
     * @return  double value found, or {@code null} when not present
     */
    private Double extractDouble(Map<String, Object> map, String key)
    {
        if (map == null) 
        {
            return null;
        }

        Object v = map.get(key);
        if (v == null) 
        {
            return null;
        }

        if (v instanceof Number) 
        {
            return ((Number) v).doubleValue();
        }

        try
        {
            return Double.parseDouble(String.valueOf(v));
        }
        catch (NumberFormatException ignored)
        {
        }
        return null;
    }

    /**
     * Returns a canonical ordering of an object pair so that {@code (A, B)} and
     * {@code (B, A)} are stored under the same key.
     *
     * @param   a
     *          first object label
     * @param   b
     *          second object label
     *
     * @return  two-element array with the lexicographically smaller label first
     */
    private String[] canonicalPair(String a, String b)
    {
        if (a == null || b == null) 
        {
            return new String[]{a, b};
        }

        if (a.compareTo(b) <= 0) 
        {
            return new String[]{a, b};
        }

        return new String[]{b, a};
    }

    /**
     * Converts a list of {@link HeatmapEntity} rows into a {@link SimilarityDataset},
     * filling in a symmetric similarity matrix and defaulting the diagonal to {@code 1.0}.
     *
     * @param   rows
     *          heatmap entity rows to convert
     *
     * @return  assembled {@link SimilarityDataset}
     */
    private Object buildSimilarityFromRows(List<HeatmapEntity> rows)
    {
        SimilarityDataset sd = new SimilarityDataset();
        if (rows == null || rows.isEmpty())
        {
            sd.setObjects(List.of());
            sd.setMatrix(List.of());
            return sd;
        }

        // collect distinct objects
        Set<String> objs = new LinkedHashSet<>();
        for (HeatmapEntity r : rows)
        {
            objs.add(r.getObject1());
            objs.add(r.getObject2());
        }
        List<String> objects = new ArrayList<>(objs);
        int n = objects.size();
        List<java.util.List<Double>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            List<Double> row = new ArrayList<>();
            for (int j = 0; j < n; j++)
            {
                row.add(i == j ? 1.0 : 0.0);
            }
            matrix.add(row);
        }

        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < objects.size(); i++) idx.put(objects.get(i), i);

        for (HeatmapEntity r : rows)
        {
            Integer i = idx.get(r.getObject1());
            Integer j = idx.get(r.getObject2());
            if (i != null && j != null)
            {
                matrix.get(i).set(j, r.getSimilarity());
                matrix.get(j).set(i, r.getSimilarity());
            }
        }

        sd.setObjects(objects);
        sd.setMatrix(matrix);
        return sd;
    }

    /**
     * Converts a list of {@link ClusterEntity} rows into a {@link ClusterNode} tree.
     * Optionally restricts the assembled tree to the children of the named cluster.
     *
     * @param   rows
     *          cluster entity rows to convert
     * @param   clusterName
     *          optional cluster name filter; {@code null} includes all clusters
     *
     * @return  root {@link ClusterNode} with the assembled child tree
     */
    private Object buildClusterTree(List<ClusterEntity> rows, String clusterName)
    {
        Map<String, ClusterNode> nodeMap = new HashMap<>();
        ClusterNode root = new ClusterNode();
        root.setName("Root");
        root.setChildren(new ArrayList<>());
        nodeMap.put("Root", root);

        rows.removeIf((row) -> row.getNodeName().equalsIgnoreCase("root"));
        for (ClusterEntity r : rows)
        {
            ClusterNode node = new ClusterNode();
            node.setName(r.getNodeName());
            node.setProbability(r.getProbability());
            node.setUri(r.getUri());
            nodeMap.put(r.getNodeName(), node);
        }

        for (ClusterEntity r : rows)
        {
            ClusterNode node = nodeMap.get(r.getNodeName());
            ClusterNode parent = nodeMap.get(r.getParentName());
            if (parent != null)
            {
                if (clusterName == null || clusterName.equals(r.getNodeName()) || parent.getName().equals(clusterName))
                {
                    if (parent.getChildren() == null)
                    {
                        parent.setChildren(new ArrayList<>());
                    }
                    if (!parent.getChildren().contains(node))
                    {
                        parent.getChildren().add(node);
                    }
                }
            }
        }

        return root;
    }

    /**
     * Extracts the entity ID from the request payload, if present.
     * Note: updates may provide {@code id} directly in payload.
     *
     * @param   dbr
     *          request body to inspect
     *
     * @return  the ID value when found, {@code null} when absent, or a
     *          {@link RuntimeException} instance when the payload cannot be parsed
     */
    private Object extractIdFromBody(DatasetBodyRequest dbr)
    {
        if (dbr.getEntry() == null || dbr.getEntry().getData() == null)
        {
            String message = String.format("Payload is null for request %s, %s!",
                                           dbr.getRequestType(),
                                           dbr.getDatasetName());
            LOG.warn(message);
            return new RuntimeException(message);
        }

        Object payload = dbr.getEntry().getData();
        Object first = null;
        if (payload instanceof List && !((List) payload).isEmpty())
        {
            first = ((List) payload).get(0);
        }
        else
        {
            first = payload;
        }

        Map<String, Object> map = objectMapper.convertValue(first, Map.class);
        if (map == null)
        {
            String message = String.format("Issue converting payload for request %s, %s!",
                                           dbr.getRequestType(),
                                           dbr.getDatasetName());
            LOG.warn(message);
            return new RuntimeException(message);
        }
        if (map.containsKey("id"))
        {
            return map.get("id");
        }
        return null;
    }

    /**
     * Exports the dataset. When filters are honored the read request result is serialized
     * to JSON - otherwise a PostgreSQL INSERT dump is generated for all rows owned
     * by the current user.
     *
     * @param   exportRequest
     *          export configuration
     *
     * @return  exported data payload as a JSON string or SQL dump string
     *
     * @throws  RuntimeException
     *          when the export fails
     */
    @Override
    public Object exportData(ExportRequest exportRequest)
    {
        try
        {
            DatasetData dd = getDatasetData();
            DatasetType datasetType = dd.getDatasetType();
            String datasetName = dd.getDatasetName();

            if (exportRequest.isHonorFilters())
            {
                // Execute read request with filters to get filtered data
                Object filteredData = executeReadRequest();

                // Convert to JSON (clean escaped newlines / pretty-print)
                ObjectMapper mapper = new ObjectMapper();
                return serializeExportObject(filteredData, mapper);
            }
            else
            {
                // Generate SQL dump for all data in this dataset
                StringBuilder sqlDump = new StringBuilder();

                String username = getUsername();
                switch (datasetType)
                {
                    case CLUSTERS ->
                    {
                        List<ClusterEntity> allClusters = clusterRepository.findByUsernameAndDatasetName(username, datasetName);
                        sqlDump.append(generatePostgresDump("cluster_entity", allClusters, datasetName));
                    }
                    case HEATMAP ->
                    {
                        List<HeatmapEntity> allHeatmaps = heatmapRepository.findByUsernameAndDatasetName(username, datasetName);
                        sqlDump.append(generatePostgresDump("heatmap_entity", allHeatmaps, datasetName));
                    }
                }

                return sqlDump.toString();
            }
        }
        catch (Exception e)
        {
            LOG.error("Failed to export SQL data", e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a PostgreSQL INSERT dump for the given table and entities.
     * Creates INSERT statements that can be executed to restore the data.
     *
     * @param   tableName
     *          target table name
     * @param   entities
     *          list of entities to dump - must be {@link HeatmapEntity} or {@link ClusterEntity}
     * @param   datasetName
     *          dataset name included in the dump header comment
     *
     * @return  SQL dump string containing INSERT statements
     */
    private String generatePostgresDump(String tableName, List<?> entities, String datasetName)
    {
        if (entities == null || entities.isEmpty())
        {
            return String.format("-- No data found for dataset: %s\n", datasetName);
        }

        StringBuilder dump = new StringBuilder();
        dump.append("-- PostgreSQL dump for table: ").append(tableName).append("\n");
        dump.append("-- Dataset: ").append(datasetName).append("\n");
        dump.append("-- Generated: ").append(new Date()).append("\n\n");

        // Process entities based on type
        if (entities.get(0) instanceof HeatmapEntity)
        {
            dump.append("-- Table: heatmap_entity\n");
            dump.append("INSERT INTO heatmap_entity (id, dataset_name, object1, object2, similarity) VALUES\n");

            List<HeatmapEntity> heatmaps = (List<HeatmapEntity>) entities;
            for (int i = 0; i < heatmaps.size(); i++)
            {
                HeatmapEntity entity = heatmaps.get(i);
                dump.append(String.format("  (%d, '%s', '%s', '%s', %f)",
                            entity.getId(),
                            escapeSql(entity.getDatasetName()),
                            escapeSql(entity.getObject1()),
                            escapeSql(entity.getObject2()),
                            entity.getSimilarity()));

                if (i < heatmaps.size() - 1)
                {
                    dump.append(",\n");
                }
                else
                {
                    dump.append(";\n");
                }
            }
        }
        else if (entities.get(0) instanceof ClusterEntity)
        {
            dump.append("-- Table: cluster_entity\n");
            dump.append("INSERT INTO cluster_entity (id, dataset_name, node_name, parent_name, probability, uri) VALUES\n");

            List<ClusterEntity> clusters = (List<ClusterEntity>) entities;
            for (int i = 0; i < clusters.size(); i++)
            {
                ClusterEntity entity = clusters.get(i);
                dump.append(String.format("  (%d, '%s', '%s', '%s', %s, '%s')",
                            entity.getId(),
                            escapeSql(entity.getDatasetName()),
                            escapeSql(entity.getNodeName()),
                            escapeSql(entity.getParentName()),
                            entity.getProbability() != null ? entity.getProbability().toString() : "NULL",
                            escapeSql(entity.getUri())));

                if (i < clusters.size() - 1)
                {
                    dump.append(",\n");
                }
                else
                {
                    dump.append(";\n");
                }
            }
        }

        dump.append("\n-- End of dump\n");
        return dump.toString();
    }

    /**
     * Escapes single quotes in a SQL string value to prevent syntax errors.
     *
     * @param   value
     *          string value to escape
     *
     * @return  escaped string, or an empty string when {@code value} is {@code null}
     */
    private String escapeSql(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value.replace("'", "''");
    }
}
