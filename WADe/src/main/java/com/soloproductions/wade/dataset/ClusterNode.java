package com.soloproductions.wade.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single node in the hierarchical cluster tree that represents a cluster dataset.
 * Each node may optionally carry a name (if not leaf), a classification probability, a URI, and
 * a list of child nodes forming the sub-tree beneath it. A leaf node is an object. Anything that 
 * is not a leaf is a cluster for said children leaf nodes.
 */
public class ClusterNode implements DataEntity
{
    /** Human-readable label identifying this cluster node. */
    private String name;

    /** 
     * Classification probability associated with this cluster node. 
     * Non-null only for leaf nodes.
    */
    @JsonProperty("Probability")
    private Double probability; // typo in the JSON... it has upper case P

    /** 
     * RDF URI identifying the resource represented by this cluster node. 
     * Non-null only for leaf nodes.
     */
    @JsonProperty("URI")
    private String URI; // Jackson doesn't like full uppercase variables?

    /** Child nodes nested beneath this node in the cluster tree. */
    private List<ClusterNode> children;

    /**
     * Returns the label of this cluster node.
     *
     * @return  node name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the label of this cluster node.
     *
     * @param   name
     *          node name to store
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the classification probability for this cluster node.
     *
     * @return  probability value
     */
    public Double getProbability()
    {
        return probability;
    }

    /**
     * Sets the classification probability for this cluster node.
     *
     * @param   probability
     *          probability value to store
     */
    public void setProbability(Double probability)
    {
        this.probability = probability;
    }

    /**
     * Returns the RDF URI associated with this cluster node.
     *
     * @return  URI string
     */
    public String getURI()
    {
        return URI;
    }

    /**
     * Sets the RDF URI associated with this cluster node.
     *
     * @param   uri
     *          URI string to store
     */
    public void setUri(String uri)
    {
        this.URI = uri;
    }

    /**
     * Returns the child nodes of this cluster node.
     *
     * @return  list of child nodes
     */
    public List<ClusterNode> getChildren()
    {
        return children;
    }

    /**
     * Sets the child nodes of this cluster node.
     *
     * @param   children
     *          list of child nodes to store
     */
    public void setChildren(List<ClusterNode> children)
    {
        this.children = children;
    }

    /**
     * Appends a child node to this cluster node's children list.
     *
     * @param   cn
     *          child node to append
     */
    public void addChild(ClusterNode cn)
    {
        children.add(cn);
    }
}
