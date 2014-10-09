package de.mpg.mpdl.service.rest.dataviewer;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.mpdl.service.rest.dataviewer.ServiceConfiguration.Pathes;
import de.mpg.mpdl.service.rest.dataviewer.process.RestProcessUtils;

@Singleton
@Path("/")
public class RestApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestApi.class);


	/**
	 * The static explain is resolved by UrlRewriteRule
	 * @throws URISyntaxException 
	 * 
	 * @GET @Path(Pathes.PATH_EXPLAIN)
	 * @Produces(MediaType.TEXT_HTML) public Response getExplain() { return
	 *                                RestProcessUtils.getExplain(); }
	 */

	@POST
	@Path(Pathes.PATH_VIEW)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)
	public Response getViewFromFiles(@Context HttpServletRequest request
	) throws IOException, FileUploadException, URISyntaxException {
		System.out.println("HERE I AM PATH VIEW POST");
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
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON_TYPE)
	public Response getExplainAll()
			throws IOException {
        return ServiceConfiguration.getExplainAllJSON();

	}

  }
