package de.mpg.mpdl.service.vo.dataviewer;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class BadRequestExceptionDataViewer extends WebApplicationException {

	    private static final long serialVersionUID = 1L;
	 
	    public BadRequestExceptionDataViewer(String message, String realm)
	    {
	    	throw new BadRequestException(message, Response.status(Status.BAD_REQUEST)
	                .entity(message).build());
	    	//super(Response.status(Status.BAD_REQUEST)
	        //        .entity(message).build());
	    }

	    public static void BadRequestExceptionUrl() 
	    {
	    	throw new BadRequestExceptionDataViewer(" \"url\" parameter is mandatory, please provide valid url and specify correct mimetype as parameter!", "Data Viewer Service");
	    }
	 
	    public static void BadRequestExceptionFile() 
	    {
	    	throw new BadRequestExceptionDataViewer ("You did not upload a file, please upload a file and specify correct mimetype as parameter!", "Data Viewer Service");
	    }
	 
	    public static void BadRequestExceptionMime() 
	    {
	        throw new BadRequestExceptionDataViewer (" \"mimetype\" parameter is mandatory, please provide valid mimetype parameter and upload a file or specify file url as parameter!", "Data Viewer Service");
	    }

	    public static void BadRequestExceptionBadConfiguration() 
	    {
	    	throw new BadRequestExceptionDataViewer(" The service configuration does not support provided format with your mimetype parameter. Please check supported formats!", "Data Viewer Service");
	    }
	
	    public static void BadRequestExceptionJSON() 
	    {
	    	throw new BadRequestExceptionDataViewer(" Error with JSON generation. Please check your configuration file!", "Data Viewer Service");
	    }
}
