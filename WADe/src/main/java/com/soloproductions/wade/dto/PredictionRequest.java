package com.soloproductions.wade.dto;

import java.util.List;

import lombok.Data;

/**
 * DTO for semantic prediction requests. Used by label/relationship prediction endpoints 
 * where one or two objects, an optional URI context, and a candidate list can be provided.
 */
@Data
public class PredictionRequest
{
    /**
     * First object/class identifier used as prediction context.
     */
    private String object1;

    /**
     * Second object/class identifier used for pairwise prediction context.
     */
    private String object2;

    /**
     * Optional URI to identify the primary entity in knowledge-graph flows.
     */
    private String uri;

    /**
     * Optional restricted candidate labels the model should choose from.
     */
    private List<String> candidates;

    /**
     * Returns the first object identifier.
     *
     * @return  first object identifier
     */
    public String getObject1()
    {
        return object1;
    }

    /**
     * Sets the first object identifier.
     *
     * @param   object1
     *          first object identifier
     */
    public void setObject1(String object1)
    {
        this.object1 = object1;
    }

    /**
     * Returns the second object identifier.
     *
     * @return  second object identifier
     */
    public String getObject2()
    {
        return object2;
    }

    /**
     * Sets the second object identifier.
     *
     * @param   object2
     *          second object identifier
     */
    public void setObject2(String object2)
    {
        this.object2 = object2;
    }
    
    /**
     * Returns the optional entity URI.
     *
     * @return  entity URI
     */
    public String getUri() 
    {
        return uri;
    }
    
    /**
     * Sets the optional entity URI.
     *
     * @param   uri
     *          entity URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the optional candidate list.
     *
     * @return  candidate labels
     */
    public List<String> getCandidates()
    {
        return candidates;
    }

    /**
     * Sets the optional candidate list.
     *
     * @param   candidates
     *          candidate labels
     */
    public void setCandidates(List<String> candidates)
    {
        this.candidates = candidates;
    }
}
