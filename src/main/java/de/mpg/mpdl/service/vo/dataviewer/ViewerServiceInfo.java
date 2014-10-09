package de.mpg.mpdl.service.vo.dataviewer;

import java.util.Arrays;
import java.util.List;

public class ViewerServiceInfo {
	
	public ViewerServiceInfo (String viewServiceId, String viewUrl, String formats, String name, String description, String homeUrl ) {

		this.serviceDescription = description;
		this.supportedFormatsString = formats;
		setSupportedFormats(formats);
		this.serviceHomeUrl = homeUrl;
		this.serviceName = name;
		this.serviceViewUrl = viewUrl;
		this.serviceId = viewServiceId;
		
	}
	
	public ViewerServiceInfo (String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceHomeUrl() {
		return serviceHomeUrl;
	}
	
	public String getSupportedFormatsString() {
		return supportedFormatsString;
	}
	public void setServiceHomeUrl(String serviceHomeUrl) {
		this.serviceHomeUrl = serviceHomeUrl;
	}
	public String getServiceViewUrl() {
		return serviceViewUrl;
	}
	public void setServiceViewUrl(String serviceViewUrl) {
		this.serviceViewUrl = serviceViewUrl;
	}
	public List<String> getSupportedFormats() {
		return supportedFormats;
	}
	public void setSupportedFormats(List<String> supportedFormats) {
		this.supportedFormats = supportedFormats;
	}

	public void setSupportedFormats(String allSupportedFormats) {
		this.supportedFormats = Arrays.asList(allSupportedFormats.split(","));
	}
	
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getServiceDescription() {
		return serviceDescription;
	}
	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	private String serviceHomeUrl;
	private String serviceViewUrl; 
	private List<String> supportedFormats;
	private String serviceName;
	private String serviceDescription;
	private String serviceId;
	private String supportedFormatsString;
	
}
