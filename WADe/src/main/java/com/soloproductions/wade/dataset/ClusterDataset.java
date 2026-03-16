package com.soloproductions.wade.dataset;

import com.soloproductions.wade.dto.AbstractDatasetRequest;
import com.soloproductions.wade.dto.ClusterDatasetRequest;

import java.io.IOException;

/**
 * Dataset implementation for hierarchical cluster tree data. It represents the
 * semantic zoom component of the application. 
 * 
 * <p> The structure of the cluster tree is not
 * infinite in depth and actually has a fixed depth of 3, there is the root node, the cluster label nodes
 * (Cluster 1, Cluster 2, Cluster 3) and the leaf nodes which are the actual objects. </p>
 */
public class ClusterDataset extends AbstractDatasetData
{
    /** Name of the specific cluster node requested by the current read operation. */
    private String clusterName;

    /** Root node of the cluster tree. */
    private ClusterNode root;

    /**
     * Default constructor.
     */
    public ClusterDataset()
    {
        // used for Jackson when serializing the JSON dataset file or for empty datasets
    }

    /**
     * Constructs and loads a cluster dataset from the specified data source if the data type is JSON.
     * At the moment, JSON datasets are only available from the default dataset pool. If there is another data type,
     * it simply initializes the root node.
     *
     * @param   dataset
     *          dataset identifier used to resolve the source file path
     * @param   dataType
     *          persistence type of the source data
     *
     * @throws  IOException
     *          when the dataset file cannot be read or parsed
     */
    public ClusterDataset(String dataset, DataType dataType) throws IOException
    {
        ClusterNode root = new ClusterNode();
        if (dataType == DataType.JSON)
        {
            DataEntity data = loadFromFile(resolveFilePath(dataset), ClusterNode.class);
            ClusterNode cn = (ClusterNode) data;
            root = cn;
        }
        setRootNode(root);
    }

    /**
     * Returns dataset type (cluster/heatmap).
     *
     * @return  dataset type
     */
    @Override
    public DatasetType getDatasetType()
    {
        return DatasetType.CLUSTERS;
    }

    /**
     * Applies filter values from the request.
     *
     * @param   adr
     *          request whose filter values should be applied
     */
    public void applyFilters(AbstractDatasetRequest adr)
    {
        super.applyFilters(adr);

        if (!handleSpecialRequest(adr) && AbstractDatasetRequest.isRequestType(adr, DatasetRequestType.READ))
        {
            ClusterDatasetRequest cdr = (ClusterDatasetRequest) adr;
            setClusterName(cdr.getClusterName());
        }
    }

    /**
     * Returns the target cluster name for the current read request.
     *
     * @return  cluster name, or {@code null} if the root tree is requested
     */
    public String getClusterName()
    {
        return clusterName;
    }

    /**
     * Sets the target cluster name for the current read request.
     *
     * @param   clusterName
     *          cluster node name to filter by
     */
    public void setClusterName(String clusterName)
    {
        this.clusterName = clusterName;
    }

    /**
     * Returns the root node of this cluster tree.
     *
     * @return  root cluster node
     */
    public ClusterNode getRootNode()
    {
        return root;
    }

    /**
     * Sets the root node of this cluster tree.
     *
     * @param   rootNode
     *          root cluster node to store
     */
    public void setRootNode(ClusterNode rootNode)
    {
        this.root = rootNode;
    }
}
