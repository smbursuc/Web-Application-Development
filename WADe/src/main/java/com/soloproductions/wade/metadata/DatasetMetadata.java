package com.soloproductions.wade.metadata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloproductions.wade.dataset.DatasetType;
import com.soloproductions.wade.service.DatasetService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregated metadata returned for dataset-related requests.
 */
public class DatasetMetadata
{
    /** Dynamic size value calculated for the requested dataset operation. */
    private int size;

    /** Maximum allowed range value exposed to clients so they can cap the range slider. */
    private int maxRange;

    /** Default metadata configuration path. */
    public static final String CONFIG_PATH = "dataset_metadata/dataset_metadata.json";

    /** Static metadata blocks keyed by dataset name. */
    private Map<String, StaticDatasetMetadata> staticMetadata = new HashMap<>();

    /**
     * Returns the size value associated with the current metadata response.
     *
     * @return  size value
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Sets the size value associated with the current metadata response.
     *
        * @param   size
     *          size value
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Returns the maximum allowed range value.
     *
     * @return  maxRange value
     */
    public int getMaxRange()
    {
        return maxRange;
    }

    /**
     * Sets the maximum allowed range value.
     *
     * @param   maxRange
     *          max range to store
     */
    public void setMaxRange(int maxRange)
    {
        this.maxRange = maxRange;
    }

    /**
     * Loads static metadata configuration from the provided JSON file path.
     *
        * @param   path
     *          path to metadata JSON
     *
     * @throws  IOException when metadata file cannot be read
     */
    public void loadMetadataFile(String path) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> rawMetadata = mapper.readValue(
                new File(path),
                new TypeReference<Map<String, Object>>()
                {
                }
        );

        Map<String, StaticDatasetMetadata> datasets = new HashMap<>();
        StaticDatasetMetadata commonMetadata = null;

        if (rawMetadata.containsKey("common"))
        {
            commonMetadata = new StaticDatasetMetadata();
            try
            {
                String json = mapper.writeValueAsString(rawMetadata.get("common"));
                mapper.readerForUpdating(commonMetadata).readValue(json);
            }
            catch (Exception e)
            {
                // ignore malformed optional common block and continue with dataset-specific metadata
            }
            rawMetadata.remove("common");
        }

        for (Map.Entry<String, Object> entry : rawMetadata.entrySet())
        {
            StaticDatasetMetadata datasetMetadata = mapper.convertValue(entry.getValue(), StaticDatasetMetadata.class);
            if (commonMetadata != null)
            {
                if (datasetMetadata.getSortOptions() == null)
                {
                    datasetMetadata.setSortOptions(commonMetadata.getSortOptions());
                }
            }
            datasets.put(entry.getKey(), datasetMetadata);
        }

        if (commonMetadata != null)
        {
            datasets.put("common", commonMetadata);
        }

        setStaticMetadata(datasets);
    }

    /**
     * Adds user-created datasets to {@link #staticMetadata} when they are not
     * present in the static configuration file.
     */
    public void addDynamicDatasets()
    {
        String[] datasetNames = DatasetService.getDatasetNames();
        for (int i = 0; i < datasetNames.length; i++)
        {
            String dataset = datasetNames[i];
            // If a dataset is already defined in the static metadata file, do not overwrite it
            if (staticMetadata.containsKey(dataset))
            {
                continue;
            }

            StaticDatasetMetadata sdm = new StaticDatasetMetadata();
            Set<DatasetType> datasetTypeSet = DatasetService.getDatasetTypes(dataset);
            DatasetInfo di = new DatasetInfo();
            Map<String, Set<String>> diActual = new HashMap<>();
            for (DatasetType dt : datasetTypeSet)
            {
                diActual.put(dt.name(), DatasetService.getDatasetDataTypes(dataset, dt));
            }
            di.setDatasetTypeAndDataTypes(diActual);
            sdm.setDatasetInfo(di);

            FeatureTuple ft1 = new FeatureTuple();
            ft1.setName("heatmaps");
            ft1.setDisplayValue("Heatmaps");

            FeatureTuple ft2 = new FeatureTuple();
            ft2.setName("clusters");
            ft2.setDisplayValue("Clusters");

            sdm.setDatasetTypes(List.of(ft1, ft2));
            staticMetadata.put(dataset, sdm);
        }
    }

    /**
     * Returns static metadata blocks.
     *
     * @return  static metadata map keyed by dataset name
     */
    public Map<String, StaticDatasetMetadata> getStaticMetadata()
    {
        return staticMetadata;
    }

    /**
     * Sets static metadata blocks.
     *
        * @param   staticMetadata
     *          static metadata map keyed by dataset name
     */
    public void setStaticMetadata(Map<String, StaticDatasetMetadata> staticMetadata)
    {
        this.staticMetadata = staticMetadata;
    }
}
