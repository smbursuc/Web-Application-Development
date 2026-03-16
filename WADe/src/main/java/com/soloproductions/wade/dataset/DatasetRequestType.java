package com.soloproductions.wade.dataset;

/**
 * CRUD operation types that can be issued against a dataset.
 * Maps directly to HTTP verbs via {@link AbstractDatasetData#resolveRequestType(String)}.
 */
public enum DatasetRequestType
{
    /** Create a new dataset entry (HTTP POST with creation flag). */
    CREATE,

    /** Overwrite an existing dataset entry (HTTP PUT). */
    UPDATE,

    /** Remove an existing dataset entry (HTTP DELETE). */
    DELETE,

    /** Fetch dataset data (HTTP GET). */
    READ
}
