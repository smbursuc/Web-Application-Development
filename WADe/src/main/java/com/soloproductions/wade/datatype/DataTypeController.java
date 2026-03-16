package com.soloproductions.wade.datatype;

import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.ExportRequest;
import com.soloproductions.wade.metadata.DatasetMetadata;

/**
 * Contract for executing dataset queries and CRUD operations against a specific data source.
 * 
 * <p> Concrete implementations handle JSON files, RDF triple stores, and SQL databases. </p>
 */
public interface DataTypeController
{
    /**
     * Dispatches the request to the appropriate CRUD operation handler.
     *
     * @param   adr
     *          request to dispatch
     *
     * @return  result produced by the concrete operation handler
     */
    public Object executeQuery(AbstractDatasetRequest adr);

    /**
     * Executes a read (GET) request on the dataset.
     *
     * @return  dataset result for the current read request
     */
    public Object executeReadRequest();

    /**
     * Executes an update (PUT) request on the dataset.
     *
     * @param   adr
     *          request carrying the update payload
     *
     * @return  result of the update operation
     */
    public Object executeUpdateRequest(AbstractDatasetRequest adr);

    /**
     * Executes a delete (DELETE) request on the dataset.
     *
     * @param   adr
     *          request carrying the delete criteria
     *
     * @return  result of the delete operation
     */
    public Object executeDeleteRequest(AbstractDatasetRequest adr);

    /**
     * Executes a create (POST) request on the dataset.
     *
     * @param   adr
     *          request carrying the creation payload
     *
     * @return  result of the create operation
     */
    public Object executeCreateRequest(AbstractDatasetRequest adr);

    /**
     * Returns metadata for the current dataset (e.g. the number of rows).
     *
     * @return  dataset metadata
     */
    public DatasetMetadata getMetadata();

    /**
     * Exports dataset data according to the provided export configuration.
     *
     * @param   exportRequest
     *          export configuration
     *
     * @return  exported data payload
     */
    public Object exportData(ExportRequest exportRequest);
}
