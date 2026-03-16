package com.soloproductions.wade.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soloproductions.wade.dataset.DataEntity;

import java.util.List;

/**
 * POJO representation of hierarchical cluster payloads.
 *
 * The structure is a three-level tree: root node, cluster nodes, and leaf
 * object nodes containing probability and URI values.
 */
public class ClusterData implements DataEntity
{
    /** Root name of the hierarchy. */
    private String name;

    /** Top-level cluster children for this root. */
    private List<Cluster> children;

    /**
     * Returns the root name.
     *
     * @return  root name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the root name.
     *
     * @param   name
     *          root name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns top-level cluster children.
     *
     * @return  cluster children
     */
    public List<Cluster> getChildren()
    {
        return children;
    }

    /**
     * Sets top-level cluster children.
     *
     * @param   children
     *          cluster children
     */
    public void setChildren(List<Cluster> children)
    {
        this.children = children;
    }

    /**
     * Cluster node under the root node.
     */
    public static class Cluster
    {
        /** Cluster label. */
        private String name;

        /** Leaf object entries belonging to this cluster. */
        private List<ObjectData> children;

        /**
         * Returns the cluster label.
         *
         * @return  cluster label
         */
        public String getName()
        {
            return name;
        }

        /**
         * Sets the cluster label.
         *
         * @param   name
         *          cluster label
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * Returns leaf object entries.
         *
         * @return  leaf object entries
         */
        public List<ObjectData> getChildren()
        {
            return children;
        }

        /**
         * Sets leaf object entries.
         *
         * @param   children
         *          leaf object entries
         */
        public void setChildren(List<ObjectData> children)
        {
            this.children = children;
        }

        /**
         * Leaf object entry with model score and source URI.
         */
        public static class ObjectData
        {
            /** Predicted object label. */
            private String name;

            /** Prediction probability value. */
            @JsonProperty("Probability")
            private double probability;

            /** Source URI associated with the prediction. */
            @JsonProperty("URI")
            private String uri;

            /**
             * Returns predicted object label.
             *
             * @return  object label
             */
            public String getName()
            {
                return name;
            }

            /**
             * Sets predicted object label.
             *
             * @param   name
             *          object label
             */
            public void setName(String name)
            {
                this.name = name;
            }

            /**
             * Returns prediction probability.
             *
             * @return  prediction probability
             */
            public double getProbability()
            {
                return probability;
            }

            /**
             * Sets prediction probability.
             *
             * @param   probability
             *          prediction probability
             */
            public void setProbability(double probability)
            {
                this.probability = probability;
            }

            /**
             * Returns source URI.
             *
             * @return  source URI
             */
            public String getUri()
            {
                return uri;
            }

            /**
             * Sets source URI.
             *
             * @param   uri
             *          source URI
             */
            public void setUri(String uri)
            {
                this.uri = uri;
            }
        }
    }
}
