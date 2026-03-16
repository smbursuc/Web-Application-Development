package com.soloproductions.wade.dataset;

/**
 * Persistence or data-source type used to read and write dataset entries.
 */
public enum DataType
{
    /** Data is stored as a JSON file on disk. */
    JSON,

    /** Data is stored in an RDF triple store and accessed via SPARQL. */
    RDF,

    /** Data is stored in a relational SQL database. */
    SQL
}
