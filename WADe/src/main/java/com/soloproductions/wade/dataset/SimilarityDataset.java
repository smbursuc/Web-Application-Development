package com.soloproductions.wade.dataset;

import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.SimilarityDatasetDTO;
import com.soloproductions.wade.dto.SimilarityDatasetRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dataset implementation for pairwise similarity heatmap data.
 * 
 * <p> Holds the list of compared objects and the corresponding similarity matrix. </p>
 */
public class SimilarityDataset extends AbstractDatasetData implements DataEntity
{
    /** The set of objects compared in the similarity matrix, in row/column order. */
    private List<String> objects;

    /** Square matrix of pairwise similarity scores, indexed by object position. */
    private List<List<Double>> matrix;

    /** Active similarity sorting strategy for the current request. */
    private SimilaritySortCriteria sortCriteria;

    /** Cached similarity sorting strategy used to avoid recomputation. */
    private SimilaritySortCriteria sortCriteriaCache;

    /** Static dataset type constant for this class. */
    public static final DatasetType datasetType = DatasetType.HEATMAP;

    /**
     * Default constructor.
     */
    public SimilarityDataset()
    {
        // used for Jackson when serializing the JSON dataset file or for empty datasets
    }

    /**
     * Constructs and loads a similarity dataset if the dataset name is from a default dataset.
     *
     * @param   dataset
     *          dataset identifier used to resolve the source file path, if not from
     *          {@link Datasets} then an empty dataset is initialized
     * @param   dataType
     *          persistence type of the source data
     *
     * @throws  IOException
     *          when the dataset file cannot be read or parsed
     */
    public SimilarityDataset(String dataset, DataType  dataType) throws IOException
    {
        // ensure dataset metadata/controller resolution works
        setDatasetName(dataset);
        setDataType(dataType);

        List<String> objects = new ArrayList<>();
        List<List<Double>> matrix = new ArrayList<>();
        if (dataType == DataType.JSON)
        {
            DataEntity data = loadFromFile(resolveFilePath(dataset), SimilarityDataset.class);
            SimilarityDataset sd = (SimilarityDataset) data;
            objects = sd.getObjects();
            matrix = sd.getMatrix();
        }
        setObjects(objects);
        setMatrix(matrix);
    }

    /**
     * Applies filter values from the request and sets the similarity sort criteria for read requests.
     *
     * @param   adr
     *          request whose filter values should be applied
     */
    public void applyFilters(AbstractDatasetRequest adr)
    {
        super.applyFilters(adr);

        if (!handleSpecialRequest(adr) && AbstractDatasetRequest.isRequestType(adr, DatasetRequestType.READ))
        {
            SimilarityDatasetRequest sdr = (SimilarityDatasetRequest) adr;
            SimilaritySortCriteria ssc = AbstractDatasetData.resolveSimilaritySortCriteriaFromString(sdr.getSimilaritySortCriteria());
            setSimilaritySortCriteria(ssc);
        }
    }

    /**
     * Executes the request and wraps the result in a {@link SimilarityDatasetDTO} to ensure
     * clean JSON serialization on the client side.
     *
     * @param   adr
     *          request to execute
     *
     * @return  a {@link SimilarityDatasetDTO} with the result data, or the raw
     *          {@link RdfResultSetWrapper} when the underlying query returns raw RDF results
     */
    public Object executeRequest(AbstractDatasetRequest adr)
    {
        // use a DTO to prevent JSON parsing errors on the client side
        // maybe to be investigated why the whole class can't be parsed properly
        // when that works just do (use super):
        // return getDataTypeController().executeQuery();
        SimilarityDatasetDTO sdDTO = new SimilarityDatasetDTO();

        Object result = getDataTypeController().executeQuery(adr);
        if (result instanceof RdfResultSetWrapper)
        {
            return result;
        }
        else if (result instanceof SimilarityDataset)
        {
            SimilarityDataset sd;
            sd = (SimilarityDataset) result;
            sdDTO.setObjects(sd.getObjects());
            sdDTO.setMatrix(sd.getMatrix());
            return sdDTO;
        }

        return result;
    }

    /**
     * Returns dataset type (cluster/heatmap).
     *
     * @return  dataset type
     */
    @Override
    public DatasetType getDatasetType()
    {
        return DatasetType.HEATMAP;
    }

    /**
     * Returns the list of objects included in the similarity matrix.
     *
     * @return  list of object labels
     */
    public List<String> getObjects()
    {
        return objects;
    }

    /**
     * Sets the list of objects included in the similarity matrix.
     *
     * @param   objects
     *          list of object labels to store
     */
    public void setObjects(List<String> objects)
    {
        this.objects = objects;
    }

    /**
     * Returns the similarity matrix.
     *
     * @return  similarity matrix
     */
    public List<List<Double>> getMatrix()
    {
        return matrix;
    }

    /**
     * Sets the similarity matrix.
     *
     * @param   matrix
     *          similarity matrix to store
     */
    public void setMatrix(List<List<Double>> matrix)
    {
        this.matrix = matrix;
    }

    /**
     * Returns the active similarity sort criteria.
     *
     * @return  current similarity sort criteria
     */
    public SimilaritySortCriteria getSimilaritySortCriteria()
    {
        return sortCriteria;
    }

    /**
     * Sets the active similarity sort criteria.
     *
     * @param   sortCriteria
     *          similarity sort criteria to store
     */
    public void setSimilaritySortCriteria(SimilaritySortCriteria sortCriteria)
    {
        this.sortCriteria = sortCriteria;
    }

    /**
     * Returns the cached similarity sort criteria.
     *
     * @return  cached similarity sort criteria
     */
    public SimilaritySortCriteria getSimilaritySortCriteriaCache()
    {
        return sortCriteriaCache;
    }

    /**
     * Sets the cached similarity sort criteria.
     *
     * @param   sortCriteriaCache
     *          cached similarity sort criteria to store
     */
    public void setSimilaritySortCriteriaCache(SimilaritySortCriteria sortCriteriaCache)
    {
        this.sortCriteriaCache = sortCriteriaCache;
    }
}
