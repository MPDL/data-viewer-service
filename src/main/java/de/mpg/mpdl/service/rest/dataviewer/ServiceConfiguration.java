package de.mpg.mpdl.service.rest.dataviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;
import de.mpg.mpdl.service.vo.dataviewer.ViewerServiceInfo;

public class ServiceConfiguration {

	public class Pathes {
		public static final String PATH_EXPLAIN = "/explain/services";
		public static final String PATH_EXPLAIN_FORMATS = "/explain/formats";
		public static final String PATH_VIEW = "/api/view";
	}

	public static final String SERVICE_NAME = "data-viewer";
	private static final String PROPERTIES_FILENAME = "data-viewer-service.properties";
	public static final String SERVICE_PROPERTY_NAME = ".name";
	public static final String SERVICE_PROPERTY_FORMATS = ".formats";
	public static final String SERVICE_PROPERTY_VIEW_URL = ".url.view";
	public static final String SERVICE_PROPERTY_DESCRIPTION = ".description";
	public static final String SERVICE_PROPERTY_HOME_URL = ".home";
	
	private static  List<ViewerServiceInfo> viewerServicesCollection;
	
	
	
	public static List<ViewerServiceInfo> getViewerServicesCollection() {
		return viewerServicesCollection;
	}



	public void setViewerServicesCollection(
			List<ViewerServiceInfo> viewerServicesCollection) {
		this.viewerServicesCollection = viewerServicesCollection;
	}

	private Properties properties = new Properties();

	public ServiceConfiguration() {
		load();
		
		
		List <String> propertiesKeysList = Arrays.asList( properties.keySet().toArray(new String[0]));
		List<String> viewerIds = new ArrayList<String>();
		List <ViewerServiceInfo> viewers = new ArrayList<ViewerServiceInfo>();
		
		Collections.sort(propertiesKeysList);
		String viewerId ="";

		for (String iter:propertiesKeysList) {
			//Finding out the ID of the service and the name of the Property
			String propertyServiceId = iter.substring(0, iter.indexOf("."));
			if (! viewerIds.contains(propertyServiceId)){
				viewerIds.add(propertyServiceId);
				System.out.println("Adding property Service "+propertyServiceId);
			}
		}
		
		for (String viewer:viewerIds ) {
			/*
			 * 
			 * 	fits.url.view=http://fits
				fits.formats=fits,someother
				fits.name=fits Viewer Service
				fits.description=Whatever fits
				fits.home=http://fits
			 * 
			 *  (String viewServiceId, String viewUrl, String formats, String name, String description, String homeUrl ) {
			 * 
			 */
			 ViewerServiceInfo vService = new ViewerServiceInfo(
					viewer,
					properties.getProperty(viewer+SERVICE_PROPERTY_VIEW_URL),
					properties.getProperty(viewer+SERVICE_PROPERTY_FORMATS),
					properties.getProperty(viewer+SERVICE_PROPERTY_NAME),
					properties.getProperty(viewer+SERVICE_PROPERTY_DESCRIPTION),
					properties.getProperty(viewer+SERVICE_PROPERTY_HOME_URL));
			 
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

	public String getScreenshotServiceUrl() {
		if (properties.containsKey("screenshot.service.url"))
			return (String) properties.get("screenshot.service.url");
		return "http://localhost:8080/screenshot";
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
                //properties.load(RestProcessUtils.getResourceAsInputStream(PROPERTIES_FILENAME));
            	loc = "C:/Users/natasab/hub-dev/tomcat-7.0.54/conf";
                //return;
            }

            System.out.println("Loading property file from "+loc+" = "+PROPERTIES_FILENAME);
            properties.load(new FileInputStream(new File(
                    FilenameUtils.concat(loc, PROPERTIES_FILENAME))));

        } catch (Exception e) {
            e.printStackTrace();
        }

	}
	
	public static String getExplainAllJSON() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		List<ViewerServiceInfo> vsi = getViewerServicesCollection();
		//File jsonOutput = new Fil
		mapper.writeValue(new File("file2"), vsi);
		return (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(vsi));
		
	}
	
	public static void main(String [] args)
	{
		ServiceConfiguration sc = new ServiceConfiguration();
		System.out.println(sc.getServiceUrl());
		
		try {
			System.out.println(getExplainAllJSON());
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}

