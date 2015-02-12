package de.mpg.mpdl.service.rest.dataviewer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration.Pathes;
import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;

public class DataViewerTest extends JerseyTest{

    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewerTest.class);


    final static String DVW_TEST_FILE_NAME = "HB060602_3ptSoma.swc";
    static String DVW_CONTENT = null;
    static String DVW_URL = null;
    static FormDataMultiPart DVW_MULTIPART = null;

    final static MediaType PNG_MEDIA_TYPE = new MediaType("image", "png");


    @Override
    protected Application configure() {
        return new MyApplication();
    }


   @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new de.mpg.mpdl.service.rest.dataviewer.MyTestContainerFactory();
    }

    /**
     * Initilize tests source file variables
     * */
    @BeforeClass
    public static void initilizeResources() {
        
        //initilize all test file-related global variables
        FileDataBodyPart filePart = null;
        URI uri = null;
        try {
            DVW_CONTENT = RestProcessUtils.getResourceAsString(DVW_TEST_FILE_NAME);
            uri = RestProcessUtils.getResourceAsURL(DVW_TEST_FILE_NAME).toURI();
            DVW_URL = uri.toURL().toString();
            filePart = new FileDataBodyPart("file1", new File(uri));
            DVW_MULTIPART = new FormDataMultiPart();
            DVW_MULTIPART.bodyPart(filePart);
            //DVW_MULTIPART.field(RestProcessUtils.PORTABLE_PARAMETER, "true");
            //DVW_MULTIPART.field(RestProcessUtils.MIMETYPE_PARAMETER, "swc");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //System.out.println("FileURL="+DVW_URL);
        assertNotNull("Cannot create URL to Data Viewer file from resources: " + DVW_TEST_FILE_NAME, DVW_URL);
        assertNotNull("Cannot read content from test resources: " + DVW_TEST_FILE_NAME, DVW_CONTENT);
        assertNotNull("Cannot create multipart body for DVW from test resources: " + DVW_TEST_FILE_NAME, filePart);

    }
    
    /**
     * File upload tests
     * */
    @Test
    public void testFileView() throws IOException {
    	 System.out.println("TESTING FILEUPLOAD ");
    	 testFile(DVW_MULTIPART
                 .field("mimetype", "swc"),
         Pathes.PATH_VIEW,
         MediaType.TEXT_HTML_TYPE
    			 );
    }
    
    @Test
    public void testUrlView() throws IOException {
    	System.out.println("TESTING URLVIEWER for NORMAL URL");
    	testUrl(
                target(Pathes.PATH_VIEW)
                .queryParam("mimetype", "swc")
                .queryParam("portable", "true")
                .queryParam("url", "http://research.mssm.edu/cnic/downloads/neurons/cnic_macaque_pyramidal.swc"),
                MediaType.TEXT_HTML_TYPE
        );
    }

    @Test
    public void testUrlViewWithSpaces() throws IOException {
    	System.out.println("TESTING URLVIEWER for URL with spaces");
    	testUrl(
                target(Pathes.PATH_VIEW)
                .queryParam("mimetype", "swc")
                .queryParam("portable", "true")
                .queryParam("url", "http://neuromorpho.org/neuroMorpho/dableFiles/de koninck/CNG version/frontal-rat-cell-118.CNG.swc"),
                MediaType.TEXT_HTML_TYPE
        );
    }

    @Test
    public void testUrlViewWithEncodedSpaces() throws IOException {
    	System.out.println("TESTING URLVIEWER for URL with encoded spaces");
    	testUrl(
                target(Pathes.PATH_VIEW)
                .queryParam("mimetype", "swc")
                .queryParam("portable", "true")
                .queryParam("url", "http://neuromorpho.org/neuroMorpho/dableFiles/de%20koninck/CNG%20version/frontal-rat-cell-118.CNG.swc"),
                MediaType.TEXT_HTML_TYPE
        );
    }

    @Test
    public void testExplain() throws IOException {
    	System.out.println("TESTING SERVICE EXPLAIN URL");
    	testUrl(
                target(Pathes.PATH_EXPLAIN),
                MediaType.APPLICATION_JSON_TYPE
        );
    }

    @Test
    public void testExplainFormats() throws IOException {
    	System.out.println("TESTING FORMATS EXPLAIN");
    	testUrl(
                target(Pathes.PATH_EXPLAIN_FORMATS),
                MediaType.APPLICATION_JSON_TYPE
        );
    }

    // HELPERS
    private void testUrl(WebTarget webTarget, MediaType responseMediaType) {

        Response response = webTarget
                .request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(responseMediaType)
                .get();
        
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(String.class), not(isEmptyOrNullString()));

    }


    private void testFile(FormDataMultiPart multipart, String path, MediaType responseMediaType) {

        Response response = target(path)
                .register(MultiPartFeature.class)
                .request(MediaType.MULTIPART_FORM_DATA_TYPE, responseMediaType)
                .post(Entity.entity(multipart, multipart.getMediaType()));

        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(String.class), not(isEmptyOrNullString()));
    }



}