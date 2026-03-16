package com.soloproductions.wade.dataset;

/**
 * Sorting strategies available when ordering heatmap dataset results.
 */
public enum SimilaritySortCriteria
{
    /** Sort rows and columns by their average pairwise similarity value. */
    AVERAGE_SIMILARITY,

    /** Sort rows and columns by the highest single pairwise similarity value found. */
    STRONGEST_PAIR,

    /** No special similarity sorting; use the dataset's default ordering. */
    DEFAULT
}
