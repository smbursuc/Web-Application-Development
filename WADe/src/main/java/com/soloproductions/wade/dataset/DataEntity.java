package com.soloproductions.wade.dataset;

/**
 * Marker interface for objects that represent a loaded dataset payload.
 * Implementations include {@link ClusterNode}, {@link SimilarityDataset},
 * and {@link RdfResultSetWrapper}. Those are mapped objects of the result sets
 * for the data types and are the response of each read request (or SPARQL query execution).
 */
public interface DataEntity
{
}
