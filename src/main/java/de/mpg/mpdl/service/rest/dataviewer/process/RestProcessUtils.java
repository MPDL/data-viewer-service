package de.mpg.mpdl.service.rest.dataviewer.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration;
import de.mpg.mpdl.service.vo.dataviewer.BadRequestExceptionDataViewer;
import de.mpg.mpdl.service.vo.dataviewer.ViewerServiceInfo;

public class RestProcessUtils {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RestProcessUtils.class);

	private static final ServiceConfiguration config = new ServiceConfiguration();
	public static final String MIMETYPE_PARAMETER = "mimetype";
	public static final String URL_PARAMETER = "url";

	public static Response generateViewFromFiles(HttpServletRequest request)
			throws IOException, FileUploadException, URISyntaxException, ServletException {
		
		String mimeTypeParam = getMimeParameter(request);
		if (mimeTypeParam== null || mimeTypeParam.isEmpty()) {
			LOGGER.info("BadRequestExceptionMime: Mimetype missing in incoming request (POST request)!");
		    BadRequestExceptionDataViewer.BadRequestExceptionMime();
		}
		
		ViewerServiceInfo selectedService = getViewerServiceforMimeType(mimeTypeParam);
		
		URI redirectLocation = new URI(selectedService.getServiceViewUrl());
		Response response = Response.temporaryRedirect(redirectLocation).build();

		return response;
	}


	public static Response generateViewFromUrl(HttpServletRequest request)
			throws IOException, URISyntaxException {

		//Validate the url as parameter
		String url = request.getParameter(URL_PARAMETER);
		String mimetype = request.getParameter(MIMETYPE_PARAMETER);
		
		if (url==null || url.isEmpty() ) {
			LOGGER.info("BadRequestExceptionUrl: Url missing in incoming request!");
			 BadRequestExceptionDataViewer.BadRequestExceptionUrl();
		}

		URI myURI = null;
		try {
		     myURI = new URI(UriBuilder.fromPath(url).toString());
		     System.out.println("MyURI = "+myURI);
		     
		} catch(URISyntaxException e) {
			LOGGER.info("BadRequestExceptionUrl: Invalid URL Syntax!");
		    BadRequestExceptionDataViewer.BadRequestExceptionUrl();
		}
		
		if (mimetype== null || mimetype.isEmpty()) {
			LOGGER.info("BadRequestExceptionMime: Mimetype missing in incoming request (GET request)!");
		    BadRequestExceptionDataViewer.BadRequestExceptionMime();
		}
		
		ViewerServiceInfo selectedService = getViewerServiceforMimeType(request.getParameter(MIMETYPE_PARAMETER));
		String newUrl = selectedService.getServiceViewUrl()+"?"+URL_PARAMETER+"="+URLEncoder.encode(myURI.toString(), "UTF-8")+"&"+MIMETYPE_PARAMETER+"="+request.getParameter(MIMETYPE_PARAMETER);
		URI redirectLocation = new URI(newUrl);
		Response response = Response.temporaryRedirect(redirectLocation).build();
		
		return response;

	}
	
	public static ViewerServiceInfo getViewerServiceforMimeType(String mimeType) {
		
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

	public static String getMimeParameter(HttpServletRequest request)
			throws IOException, FileUploadException {

		if (!ServletFileUpload.isMultipartContent(request)) {
			LOGGER.info("BadRequestExceptionFileUpload: is Not Multipart Request (POST request)!");
		    BadRequestExceptionDataViewer.BadRequestExceptionMultiPart();
		}

		//Use Streaming to parse through rather than downloading and file processing
		//Initialize the file upload handler
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iter = upload.getItemIterator(request);
		
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			String name = item.getFieldName();
			if (name.equals(MIMETYPE_PARAMETER)) {
				InputStream stream=item.openStream();
				String mimeValue = Streams.asString(stream);
				stream.close();
				return mimeValue;
			}
		}
		
		return "";
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

}
