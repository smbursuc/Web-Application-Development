package com.soloproductions.wade.metadata;

import com.soloproductions.wade.dataset.AbstractDatasetData;
import com.soloproductions.wade.dataset.DatasetData;
import com.soloproductions.wade.dataset.DataType;
import com.soloproductions.wade.dataset.SimilarityDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatasetMetadataTest
{

    private DatasetMetadata metadata;

    @BeforeEach
    void setUp() throws IOException
    {
        metadata = new DatasetMetadata();
        AbstractDatasetData.setMetadataStatic(metadata);
    }

    @Test
    void testPredefinedDatasetMetadata()
    {
        Map<String, StaticDatasetMetadata> staticMetadata = metadata.getStaticMetadata();
        assertTrue(staticMetadata.containsKey("bsds300"));
        StaticDatasetMetadata bsds300Metadata = staticMetadata.get("bsds300");
        assertEquals("BSDS300", bsds300Metadata.getDisplayValue());
        assertNotNull(bsds300Metadata.getFeatures());
        assertNotNull(bsds300Metadata.getSortOptions());
        assertEquals(3, bsds300Metadata.getSortOptions().getCommon().size());
    }

    @Test
    void testDynamicDatasetInheritsCommonMetadata() throws IOException
    {
        // Use the statically loaded metadata (no Spring context required)
        Map<String, StaticDatasetMetadata> staticMetadata = metadata.getStaticMetadata();
        assertNotNull(staticMetadata, "staticMetadata should be loaded");
        assertTrue(staticMetadata.containsKey("common"), "staticMetadata should contain 'common' key");

        StaticDatasetMetadata common = staticMetadata.get("common");
        assertNotNull(common, "common metadata should not be null");
        // Check that it has the common sort options
        assertNotNull(common.getSortOptions(), "common.sortOptions should not be null");
        assertEquals(3, common.getSortOptions().getCommon().size());
        assertEquals(2, common.getSortOptions().getHeatmaps().size());

        // Check that it has the common data models
        assertNotNull(common.getDataModels(), "common.dataModels should not be null");
        assertEquals(3, common.getDataModels().size());
    }

    @Test
    void testCommonMetadataIsLoaded()
    {
        Map<String, StaticDatasetMetadata> staticMetadata = metadata.getStaticMetadata();
        assertTrue(staticMetadata.containsKey("common"));
        StaticDatasetMetadata commonMetadata = staticMetadata.get("common");
        assertNotNull(commonMetadata.getSortOptions());
        assertNotNull(commonMetadata.getDataModels());
        assertEquals(3, commonMetadata.getDataModels().size());
    }
}
