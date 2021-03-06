package de.mpg.mpdl.service.rest.dataviewer;

import de.mpg.mpdl.service.vo.dataviewer.ViewerServiceInfo;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;

public class ServiceConfiguration {

	public class Pathes {
		public static final String PATH_EXPLAIN = "/explain/services";
		public static final String PATH_EXPLAIN_FORMATS = "/explain/formats";
		public static final String PATH_VIEW = "/view";
	}

	public static final String SERVICE_NAME = "data-viewer";
	private static final String PROPERTIES_FILENAME = "data-viewer-service.properties";
	public static final String SERVICE_PROPERTY_NAME = ".name";
	public static final String SERVICE_PROPERTY_FORMATS = ".formats";
	public static final String SERVICE_PROPERTY_VIEW_URL = ".url.view";
	public static final String SERVICE_PROPERTY_DESCRIPTION = ".description";
	public static final String SERVICE_PROPERTY_HOME_URL = ".home";
	
	private static List<ViewerServiceInfo> viewerServicesCollection;
	
	
	
	public static List<ViewerServiceInfo> getViewerServicesCollection() {
		return viewerServicesCollection;
	}



	public void setViewerServicesCollection(
			List<ViewerServiceInfo> viewerServicesCollection) {
		ServiceConfiguration.viewerServicesCollection = viewerServicesCollection;
	}
	
	private Properties properties = new Properties();

	public ServiceConfiguration() {
		load();
		
		//Service collection will actually read the properties file and fill-in the list of viewer services
		
		List <String> propertiesKeysList = Arrays.asList( properties.keySet().toArray(new String[0]));
		List<String> viewerIds = new ArrayList<String>();
		List <ViewerServiceInfo> viewers = new ArrayList<ViewerServiceInfo>();
		
		Collections.sort(propertiesKeysList);

		for (String iter:propertiesKeysList) {
			//Finding out the ID of the service and the name of the Property
			String propertyServiceId = iter.substring(0, iter.indexOf("."));
			if (! viewerIds.contains(propertyServiceId)){
				viewerIds.add(propertyServiceId);
			}
		}
		
		for (String viewer:viewerIds ) {
			 ViewerServiceInfo vService = new ViewerServiceInfo(
					viewer,
					properties.getProperty(viewer+SERVICE_PROPERTY_VIEW_URL, ""),
					properties.getProperty(viewer+SERVICE_PROPERTY_FORMATS, ""),
					properties.getProperty(viewer+SERVICE_PROPERTY_NAME, ""),
					properties.getProperty(viewer+SERVICE_PROPERTY_DESCRIPTION, ""),
					properties.getProperty(viewer+SERVICE_PROPERTY_HOME_URL, ""));
			 
			 //Clean up duplicate formats, not to cause issues
			 vService.setSupportedFormats(cleanUpDuplicateFormats(viewers, vService.getSupportedFormats()));
			 viewers.add(vService);
			
		}
		
		if (viewers.size()>0){
			setViewerServicesCollection(viewers);
		}
	}

	
	public String getServiceUrl() {
		if (properties.containsKey("service.url"))
			return normalizeServiceUrl((String) properties.get("service.url"));
		return "http://localhost:8080/" + SERVICE_NAME;
	}

	private String normalizeServiceUrl(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	public String getServiceApiUrl() {
		return getServiceUrl() + "/api";
	}

	/**
	 * Load the properties
	 */
	private void load() {

        String loc = "";
        try {
            if (System.getProperty("jboss.server.config.dir") != null) {
                loc = System.getProperty("jboss.server.config.dir");
            } else if (System.getProperty("catalina.home") != null) {
                loc = System.getProperty("catalina.home") + "/conf";
            } else  {

                //if no app server is defined, take props from WEB-INF
                //(this is the test case)
                properties.load(new RestProcessUtils().getResourceAsInputStream(PROPERTIES_FILENAME));
                return;
            }

            properties.load(new FileInputStream(new File(
                    FilenameUtils.concat(loc, PROPERTIES_FILENAME))));

        } catch (Exception e) {
            e.printStackTrace();
        }

	}
	
	public static List<String> cleanUpDuplicateFormats(List<ViewerServiceInfo> vsi, List<String> configuredFormats){
		
		List<String> allSupportedFormats = new ArrayList<String>();
		for (ViewerServiceInfo vsi1:vsi){
			allSupportedFormats.addAll(vsi1.getSupportedFormats());
		}
		
		List<String> supportedServiceFormatsCleanedUp = new ArrayList<String>(); 
		supportedServiceFormatsCleanedUp = new ArrayList<String>(); 

		for (String formatS:configuredFormats) {
				if (!allSupportedFormats.contains(formatS)) {
					allSupportedFormats.add(formatS);
					supportedServiceFormatsCleanedUp.add(formatS);
				}
			}
	
		return supportedServiceFormatsCleanedUp;

	}
	
	
	public static List <String> getAllSupportedFormatsList (){
		
		List<ViewerServiceInfo> vsi = getViewerServicesCollection()!=null?
							getViewerServicesCollection():
							new ArrayList<ViewerServiceInfo> ();
		
		List<String> supportedFormats = null;
		List<String> allSupportedFormats = new ArrayList<String>();  
		for (ViewerServiceInfo vsiService:vsi) {
			supportedFormats = new ArrayList<String>();
			for (String formatS:vsiService.getSupportedFormats()) {
				if (!allSupportedFormats.contains(formatS)) {
					supportedFormats.add(formatS);
					allSupportedFormats.add(formatS);
				}
			}
			vsiService.setSupportedFormats(supportedFormats);
		}
		
		Collections.sort(allSupportedFormats);
		return allSupportedFormats;
	}
	
	public static String getExplainFormatsJSON() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getAllSupportedFormatsList()));
		
	}
	
	public static String getExplainAllJSON() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getViewerServicesCollection()));
		
	}

}

