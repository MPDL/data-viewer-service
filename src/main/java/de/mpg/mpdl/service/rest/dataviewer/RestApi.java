package de.mpg.mpdl.service.rest.dataviewer;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileUploadException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration.Pathes;
import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;

@Singleton
@Path("/")
public class RestApi {

	//private static final Logger LOGGER = LoggerFactory.getLogger(RestApi.class);
 
	@POST
	@Path(Pathes.PATH_VIEW)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)
	public Response getViewFromFiles(@Context HttpServletRequest request
	) throws IOException, FileUploadException, URISyntaxException {
        return RestProcessUtils.generateViewFromFiles(request);
	}

	
	@GET
	@Path(Pathes.PATH_VIEW)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	public Response getViewFromUrl(@QueryParam("url") String url, @QueryParam("mimetype") String mimetype)
			throws IOException {
        return RestProcessUtils.generateViewFromUrl(url, mimetype);

	}
	
	@GET
	@Path(Pathes.PATH_EXPLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	
	public Response getExplainServices() throws JsonGenerationException, JsonMappingException, IOException
			 {
        return  RestProcessUtils.buildJSONResponse(RestProcessUtils.getExplainAllJSON(), Status.OK);

	}

	@GET
	@Path(Pathes.PATH_EXPLAIN_FORMATS)
	@Produces(MediaType.APPLICATION_JSON)
	
	public Response getExplainFormats() throws JsonGenerationException, JsonMappingException, IOException
			 {
        return  RestProcessUtils.buildJSONResponse(RestProcessUtils.getExplainFormatsJSON(), Status.OK);

	}
  }
