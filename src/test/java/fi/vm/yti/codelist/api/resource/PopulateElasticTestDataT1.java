package fi.vm.yti.codelist.api.resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fi.vm.yti.codelist.api.AbstractTestBase;
import fi.vm.yti.codelist.api.PublicApiServiceApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PublicApiServiceApplication.class})
@ActiveProfiles({"test"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class PopulateElasticTestDataT1 extends AbstractTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceIntegrationT2.class);

    @Test
    public void populateData() {
        LOG.info("Indexing mock data to data ElasticSearch.");
        createAndIndexMockData();
        LOG.info("Indexing done.");
    }
}
