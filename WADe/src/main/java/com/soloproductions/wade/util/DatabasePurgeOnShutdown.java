package com.soloproductions.wade.util;

import com.soloproductions.wade.repository.ClusterRepository;
import com.soloproductions.wade.repository.HeatmapRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lifecycle component that optionally purges all heatmap and cluster rows from the
 * database when the Spring context shuts down.
 *
 * <p>Purging is controlled by the {@code wade.db.purge.on.shutdown} property
 * (default: {@code true}). Set it to {@code false} to retain data across restarts.
 */
@Component
public class DatabasePurgeOnShutdown
{
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePurgeOnShutdown.class);

    /** JPA repository for heatmap rows. */
    private final HeatmapRepository heatmapRepository;

    /** JPA repository for cluster rows. */
    private final ClusterRepository clusterRepository;

    /** Whether to delete all rows on shutdown. */
    private final boolean purgeOnShutdown;

    /**
     * @param   heatmapRepository   
     *          JPA repository for heatmap rows
     * @param   clusterRepository   
     *          JPA repository for cluster rows
     * @param   purgeOnShutdown     
     *          whether to delete all rows on shutdown
     */
    public DatabasePurgeOnShutdown(HeatmapRepository heatmapRepository,
                                   ClusterRepository clusterRepository,
                                   @Value("${wade.db.purge.on.shutdown:false}") boolean purgeOnShutdown)
    {
        this.heatmapRepository = heatmapRepository;
        this.clusterRepository = clusterRepository;
        this.purgeOnShutdown = purgeOnShutdown;
    }

    /**
     * Invoked by Spring before the context is destroyed. Deletes all heatmap and
     * cluster rows when {@code wade.db.purge.on.shutdown} is {@code true}.
     */
    @PreDestroy
    public void purgeDatabaseIfRequested()
    {
        if (!purgeOnShutdown)
        {
            LOG.info("Database purge on shutdown is disabled (wade.db.purge.on.shutdown=false).");
            return;
        }

        try
        {
            LOG.info("Purging heatmap table...");
            heatmapRepository.deleteAllInBatch();
            LOG.info("Purging cluster table...");
            clusterRepository.deleteAllInBatch();
            LOG.info("Database purge completed successfully.");
        }
        catch (Exception e)
        {
            LOG.error("Error while purging database on shutdown", e);
        }
    }
}
