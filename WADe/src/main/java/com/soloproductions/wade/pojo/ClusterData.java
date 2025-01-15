package com.soloproductions.wade.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ClusterData
{
    private String name;
    private List<Cluster> children;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<Cluster> getChildren()
    {
        return children;
    }

    public void setChildren(List<Cluster> children)
    {
        this.children = children;
    }

    public static class Cluster
    {
        private String name;
        private List<ObjectData> children;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List<ObjectData> getChildren()
        {
            return children;
        }

        public void setChildren(List<ObjectData> children)
        {
            this.children = children;
        }

        public static class ObjectData
        {
            private String name;

            @JsonProperty("Probability")
            private double probability;

            @JsonProperty("URI")
            private String uri;

            public String getName()
            {
                return name;
            }

            public void setName(String name)
            {
                this.name = name;
            }

            public double getProbability()
            {
                return probability;
            }

            public void setProbability(double probability)
            {
                this.probability = probability;
            }

            public String getUri()
            {
                return uri;
            }

            public void setUri(String uri)
            {
                this.uri = uri;
            }
        }
    }
}
