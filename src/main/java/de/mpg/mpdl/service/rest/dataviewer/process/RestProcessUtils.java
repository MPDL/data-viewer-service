package de.mpg.mpdl.service.rest.dataviewer.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration;
import de.mpg.mpdl.service.vo.dataviewer.BadRequestExceptionDataViewer;
import de.mpg.mpdl.service.vo.dataviewer.ViewerServiceInfo;

public class RestProcessUtils {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RestProcessUtils.class);

	private static final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
	private static final ServiceConfiguration config = new ServiceConfiguration();
	public static final String MIMETYPE_PARAMETER = "mimetype";
	public static final String PORTABLE_PARAMETER = "portable";

	public static Response generateViewFromFiles(HttpServletRequest request)
			throws IOException, FileUploadException, URISyntaxException {

		// get the multipart request and process parameters and files of the
		// multipart request
		List<FileItem> fileItems = uploadFiles(request);

		// get the file which was uploaded
		FileItem uploadedFileItem = getFirstFileItem(fileItems);
		
		//Check parameters and throw exception 
		if (uploadedFileItem == null || uploadedFileItem.getSize() == 0) {
			LOGGER.info("BadRequestExceptionFile: File missing in incoming request!");
			BadRequestExceptionDataViewer.BadRequestExceptionFile();
		}
			
		File uploadedFile = getInputStreamAsFile(uploadedFileItem
				.getInputStream());
		
		

		// get the mimetype from provided parameters
		String selectedMimeType = "";
		//necessary to check the existence of the parameter
		boolean foundMimeTypeParam = false;
		
		for (FileItem fit : fileItems) {
			if (fit.isFormField()
					&& fit.getFieldName().equalsIgnoreCase(MIMETYPE_PARAMETER)) {
				selectedMimeType = fit.getString();
				foundMimeTypeParam = true;
				break;
			}
		}
		
		if (!foundMimeTypeParam) {
			LOGGER.info("BadRequestExceptionMime: Mimetype missing in incoming request (multipart form)!");
			BadRequestExceptionDataViewer.BadRequestExceptionMime();
		}

		// parse fileItems get MimeType and FileItem (uploaded image)
		ViewerServiceInfo selectedService = getViewerServiceforMimeType(selectedMimeType);

		//
		// create PostRequest to the viewerservice as portable

		// Create REST Jersey POST request
		Client client = ClientBuilder.newClient();

		WebTarget target = client.target(selectedService.getServiceViewUrl());

		// URI uriUploadedFile =
		// RestProcessUtils.getResourceAsURL(uploadedFile.getName()).toURI();
		FileDataBodyPart filePart = new FileDataBodyPart("file1", uploadedFile);
		FormDataMultiPart dataViewerServiceForm = new FormDataMultiPart();
		dataViewerServiceForm.bodyPart(filePart);
		dataViewerServiceForm.field(PORTABLE_PARAMETER, String.valueOf(true));

		Response response = target
				.register(MultiPartFeature.class)
				.request(MediaType.MULTIPART_FORM_DATA_TYPE, MediaType.TEXT_HTML_TYPE)
				.post(Entity.entity(dataViewerServiceForm,
						dataViewerServiceForm.getMediaType()));

		return response;
	}


	public static Response generateViewFromUrl(String url, String mimetype, String load)
			throws IOException {

		//Validate the url as parameter
		if (url==null || url.isEmpty() ) {
			LOGGER.info("BadRequestExceptionUrl: Url missing in incoming request!");
			 BadRequestExceptionDataViewer.BadRequestExceptionUrl();
		}

		try {
		    String someStuff = new URI(url).toString();
		} catch(URISyntaxException e) {
			LOGGER.info("BadRequestExceptionUrl: Invalid URL Syntax!");
		    BadRequestExceptionDataViewer.BadRequestExceptionUrl();
		}
		
		if (mimetype== null || mimetype.isEmpty()) {
			LOGGER.info("BadRequestExceptionMime: Mimetype missing in incoming request (GET request)!");
		    BadRequestExceptionDataViewer.BadRequestExceptionMime();
		}
		
		
		ViewerServiceInfo selectedService = getViewerServiceforMimeType(mimetype);
//		System.out.println("MimeType = "+mimetype+ " and Service = "+selectedService.getServiceId());
//		System.out.println("URL= "+url);
//		System.out.println("URL Encoded= "+UriBuilder.fromPath(url));

		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(selectedService.getServiceViewUrl());
		
//		System.out.println("ServiceURL= "+selectedService.getServiceViewUrl());

		Response response = target
				.queryParam("url", UriBuilder.fromPath(url))
				.queryParam("load", load)
				.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE, MediaType.TEXT_HTML_TYPE)
				.get();

		return response;

	}

	public static ViewerServiceInfo getViewerServiceforMimeType(String mimeType) {
		
/*		if (ServiceConfiguration.getViewerServicesCollection().size()==0) {
			ServiceConfiguration sc = new ServiceConfiguration();
		}
*/		
		List<ViewerServiceInfo> viewerServices = ServiceConfiguration
				.getViewerServicesCollection();
		
		for (ViewerServiceInfo viewerService : viewerServices) {
			if (viewerService.getSupportedFormats().contains(mimeType)) {
				return viewerService;
			}
		}

		BadRequestExceptionDataViewer.BadRequestExceptionBadConfiguration();
		return null;
	}

	// Helpers

	public static List<FileItem> uploadFiles(HttpServletRequest request)
			throws FileUploadException {
		List<FileItem> items = null;
		if (ServletFileUpload.isMultipartContent(request)) {
			ServletContext servletContext = request.getServletContext();
			File repository = (File) servletContext
					.getAttribute(JAVAX_SERVLET_CONTEXT_TEMPDIR);
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setRepository(repository);
			ServletFileUpload fileUpload = new ServletFileUpload(factory);
			items = fileUpload.parseRequest(request);
		}
		return items;
	}

	public static Response buildHtmlResponse(String str) {
		return buildHtmlResponse(str, Status.OK);
	}

	public static Response buildHtmlResponse(String str, Status status) {
		return Response.status(status).entity(str).type(MediaType.TEXT_HTML)
				.build();
	}

	public static Response buildJSONResponse(String str, Status status) {
		return Response.status(status).entity(str)
				.type(MediaType.APPLICATION_JSON).build();
	}
	
	public static String getExplainAllJSON() {
		try {
			
			return ServiceConfiguration.getExplainAllJSON();
			
		} catch (Exception e) {
			String returningStr = "BadRequestExceptionJSON: getExplainAllJSON made a mistake!";
			LOGGER.info(returningStr);
			BadRequestExceptionDataViewer.BadRequestExceptionJSON();
			return returningStr;

		}
	}

	public static String getExplainFormatsJSON() {
		try {
			
			return ServiceConfiguration.getExplainFormatsJSON();
			
		} catch (Exception e) {
			String returningStr = "BadRequestExceptionJSON: getExplainFormatsJSON made a mistake!";
			LOGGER.info(returningStr);
			BadRequestExceptionDataViewer.BadRequestExceptionJSON();
			return returningStr;

		}
	}

	public static String getResourceAsString(String fileName)
			throws IOException {
		return getInputStreamAsString(getResourceAsInputStream(fileName));
	}

	public static InputStream getResourceAsInputStream(String fileName)
			throws IOException {
		return new RestProcessUtils().getClass().getClassLoader()
				.getResourceAsStream(fileName);
	}

	public static URL getResourceAsURL(String fileName) throws IOException {
		return new RestProcessUtils().getClass().getClassLoader()
				.getResource(fileName);
	}

	private static String getInputStreamAsString(InputStream stream)
			throws IOException {
		Closer closer = Closer.create();
		closer.register(stream);
		String string = null;
		try {
			string = CharStreams.toString(new InputStreamReader(stream,
					StandardCharsets.UTF_8));
		} catch (Throwable e) {
			closer.rethrow(e);
		} finally {
			closer.close();
		}
		return string;
	}

	private static File getInputStreamAsFile(InputStream stream)
			throws IOException {
		Closer closer = Closer.create();
		closer.register(stream);
		File f = File.createTempFile("dataViewer", ".dataViewer");
		try {
			ByteStreams.copy(stream, new FileOutputStream(f));
		} catch (Throwable e) {
			closer.rethrow(e);
		} finally {
			closer.close();
		}
		return f;
	}

	// get only first processed file!
	public static FileItem getFirstFileItem(List<FileItem> fileItems)
			throws IOException {

		if (LOGGER.isDebugEnabled()) {
			for (FileItem fileItem : fileItems) {
				if (fileItem.isFormField()) {
					LOGGER.debug("fileItem.getFieldName():"
							+ fileItem.getFieldName());
					LOGGER.debug("value:" + fileItem.getString());
				}
			}
		}

		for (FileItem fileItem : fileItems) {
			if (!fileItem.isFormField()) {
				return fileItem;
			}
		}
		return null;
	}

	private static int getNumberOfBins(List<FileItem> items) {
		for (FileItem item : items)
			if (item.isFormField()
					&& "numberOfBins".equals(item.getFieldName()))
				return Integer.parseInt(item.getString());
		return 0;
	}

	private static boolean getWidthOfBins(List<FileItem> items) {
		for (FileItem item : items)
			if (item.isFormField() && "typeOfBins".equals(item.getFieldName()))
				return item.getString().equals("width");
		return false;
	}

	private static String getQuery(List<FileItem> items) {
		for (FileItem item : items)
			if (item.isFormField() && "query".equals(item.getFieldName()))
				return item.getString();
		return null;
	}
}
