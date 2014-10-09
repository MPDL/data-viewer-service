package de.mpg.mpdl.service.rest.dataviewer.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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

import de.mpg.mpdl.service.connector.FitsViewService;
import de.mpg.mpdl.service.connector.SWC3DViewService;
import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration;
import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration.Pathes;
import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;
import de.mpg.mpdl.service.vo.dataviewer.ViewerServiceInfo;

public class RestProcessUtils {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RestProcessUtils.class);

	private static final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";
	private static final String SERVICE_VIEW_HTML_TEMPLATE_FILE_NAME = "service_view_template.html";
	private static final String SERVICE_VIEW_THUMB_HTML_TEMPLATE_FILE_NAME = "service_view_template_thumb.html";
	private static final String JS_LIBS_LINKED_FILE_NAME = "js-linked.html";
	private static final String JS_LIBS_PORTABLE_FILE_NAME = "js-portable.html";
	private static final ServiceConfiguration config = new ServiceConfiguration();
	public static final String MIMETYPE_PARAMETER = "mimetype";
	public static final String PORTABLE_PARAMETER = "portable";

	public static Response generateViewFromFiles(HttpServletRequest request)
			throws IOException, FileUploadException, URISyntaxException {

		// Java connector

		SWC3DViewService swcService = new SWC3DViewService();
		swcService.generateFromFile("", null, true);
		
		FitsViewService fitsService = new FitsViewService();	
		fitsService.generateFromFile("", null);

		// get the multipart request and process parameters and files of the
		// multipart request
		List<FileItem> fileItems = uploadFiles(request);

		// get the file which was uploaded
		FileItem uploadedFileItem = getFirstFileItem(fileItems);
		System.out.println("I uploaded File " + uploadedFileItem.getName()
				+ " big " + uploadedFileItem.getSize());
		File uploadedFile = getInputStreamAsFile(uploadedFileItem
				.getInputStream());

		// get the mimetype from provided parameters
		String selectedMimeType = "";
		for (FileItem fit : fileItems) {
			if (fit.isFormField()
					&& fit.getFieldName().equalsIgnoreCase(MIMETYPE_PARAMETER)) {
				selectedMimeType = fit.getString();
				break;
			}
		}

		System.out.println("MimeType is " + selectedMimeType);
		// parse fileItems get MimeType and FileItem (uploaded image)
		ViewerServiceInfo selectedService = getViewerServiceforMimeType(selectedMimeType);

		//
		// create PostRequest to the viewerservice as portable

		System.out.println("Selected Service is "
				+ selectedService.getServiceViewUrl());

		// Create REST Jersey POST request
		Client client = ClientBuilder.newClient();

		WebTarget target = client.target(selectedService.getServiceViewUrl());

		System.out.println(uploadedFile.getName());
		if (RestProcessUtils.getResourceAsURL(uploadedFile.getName()) == null) {
			System.out.println("Can not find resource on classpath");
		}
		// URI uriUploadedFile =
		// RestProcessUtils.getResourceAsURL(uploadedFile.getName()).toURI();
		FileDataBodyPart filePart = new FileDataBodyPart("file1", uploadedFile) /*
																				 * new
																				 * File
																				 * (
																				 * uriUploadedFile
																				 * )
																				 * )
																				 */;
		FormDataMultiPart dataViewerServiceForm = new FormDataMultiPart();
		dataViewerServiceForm.bodyPart(filePart);
		dataViewerServiceForm.field(PORTABLE_PARAMETER, String.valueOf(true));

		Response response = target
				.register(MultiPartFeature.class)
				.request(MediaType.MULTIPART_FORM_DATA_TYPE)
				/* , MediaType.TEXT_HTML_TYPE) */
				.post(Entity.entity(dataViewerServiceForm,
						dataViewerServiceForm.getMediaType()));

		System.out.println(response.getStatus());
		return response;
	}

	public static Response generateViewFromUrl(String url, String mimetype)
			throws IOException {
		// TODO: jersey-client

		ViewerServiceInfo selectedService = getViewerServiceforMimeType(mimetype);

		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(selectedService.getServiceViewUrl());

		Response response = target.queryParam("url", url)
				.queryParam("portable", String.valueOf(true))
				.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
				.accept(MediaType.TEXT_HTML_TYPE).get();

		// String myBeanXML = response.readEntity(String.class);
		// System.out.println("PRINTING RESPONSE" );
		// System.out.println(myBeanXML);
		System.out.println("RESPONSE STATUS IS " + response.getStatus());
		return response;

	}

	public static ViewerServiceInfo getViewerServiceforMimeType(String mimeType) {
		List<ViewerServiceInfo> viewerServices = ServiceConfiguration
				.getViewerServicesCollection();
		ViewerServiceInfo selectedService = null;
		System.out.println("Found so many viewers " + viewerServices.size());
		for (ViewerServiceInfo viewerService : viewerServices) {
			System.out.println("Checking service "
					+ viewerService.getServiceId() + " formats= "
					+ viewerService.getSupportedFormatsString());
			if (viewerService.getSupportedFormats().contains(mimeType)) {
				return viewerService;
			}
		}
		System.out.println("Returning NULL service");

		return null;
	}

	// public static String generateHtmlFromFiles(HttpServletRequest request)
	// throws FileUploadException, IOException {
	// List<FileItem> fileItems = uploadFiles(request);
	// LOGGER.info("files uploaded..." + request.toString());
	// return generateResponseHtml(
	// getInputStreamAsString(getFirstFileItem(fileItems)
	// .getInputStream()), isPortable(fileItems), false);
	// }

	private static boolean isPortable(List<FileItem> fileItems) {
		for (FileItem fileItem : fileItems) {
			if (fileItem.isFormField()
					&& "portable".equals(fileItem.getFieldName())) {
				return fileItem.getString() != null
						&& Boolean.parseBoolean(fileItem.getString());
			}
		}
		return false;
	}

	// public static Response generateThumbnailFromFiles(HttpServletRequest
	// request)
	// throws FileUploadException, IOException {
	//
	// // upload files
	// List<FileItem> fileItems = uploadFiles(request);
	// LOGGER.info("files uploaded...");
	// // Get only the 1st item (multiple items is not relevant so far)
	// FileItem item = getFirstFileItem(fileItems);
	//
	// return generateThumbnail(item.getInputStream());
	//
	// }

	// public static Response generateThumbnailFromUrl(String url)
	// throws IOException {
	//
	// // get de.mpg.mpdl.service.rest.dataViewer from url (input stream)
	// URLConnection dataViewerSourceConnection = URI.create(url).toURL()
	// .openConnection();
	//
	// return generateThumbnail(dataViewerSourceConnection.getInputStream());
	//
	// }

	// public static Response generateThumbnailFromTextarea(String dataViewer)
	// throws IOException {
	// // get de.mpg.mpdl.service.rest.dataViewer from String
	// return generateThumbnail(new ByteArrayInputStream(
	// dataViewer.getBytes(StandardCharsets.UTF_8)));
	// }

	/*
	 * private static Response generateThumbnail(InputStream inputStream) throws
	 * IOException { Closer closer = Closer.create();
	 * closer.register(inputStream);
	 * 
	 * URLConnection screenshotConn = null; byte[] bytes = null; try { // TODO:
	 * jersey-client // screenshot service connection screenshotConn = URI
	 * .create(config.getScreenshotServiceUrl() +
	 * "/take?useFireFox=true").toURL().openConnection();
	 * screenshotConn.setDoOutput(true);
	 * 
	 * // build response entity directly from .dataViewer inputStream bytes =
	 * generateResponseHtml(getInputStreamAsString(inputStream), true,
	 * true).getBytes(StandardCharsets.UTF_8);
	 * 
	 * InputStream dataViewerResponseInputStream = closer .register(new
	 * ByteArrayInputStream(bytes));
	 * 
	 * ByteStreams.copy(dataViewerResponseInputStream,
	 * closer.register(screenshotConn.getOutputStream()));
	 * 
	 * bytes = ByteStreams.toByteArray(screenshotConn.getInputStream());
	 * 
	 * // TODO: jersey-client
	 * 
	 * 
	 * } catch (Throwable e) { throw closer.rethrow(e); } finally {
	 * closer.close(); }
	 * 
	 * // Return the response of the screenshot service return
	 * Response.status(Status.OK).entity(bytes).type("image/png") .build(); }
	 */

	/*
	 * public static String generateResponseHtml(String dataViewer, boolean
	 * portable, boolean thumb) throws IOException { // get js libs block String
	 * chunk = getResourceAsString(portable ? JS_LIBS_PORTABLE_FILE_NAME :
	 * JS_LIBS_LINKED_FILE_NAME); // insert js libs block chunk =
	 * getResourceAsString( SERVICE_VIEW_HTML_TEMPLATE_FILE_NAME).replace(
	 * "%JS_LIBS_PLACEHOLDER%", chunk); // repalce other placeholders return
	 * chunk.replace("%SWC_CONTENT_PLACEHOLDER%", dataViewer).replace(
	 * "%SWC_SERVICE_PLACEHOLDER%", config.getServiceUrl()); }
	 */
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
