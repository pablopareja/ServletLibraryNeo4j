package com.era7.bioinfo.servletlibraryneo4j.servlet;

import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.util.ActiveSessions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.era7.lib.communication.util.SessionAttributes;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;

/**
 * Basic abstract class for servlet implementations, (except for Login servlet functionality --> see: {@link BasicLoginService}
 *
 * @author Pablo Pareja Tobes
 */
public abstract class BasicServletNeo4j extends HttpServlet {

    public static final String ACCESS_DENIED_MESSAGE = "Access denied";
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * Parameter name used in the GET/POST request to pass the xml request
     */
    public static String PARAMETER_NAME = "request";
    /**
     * Flag indicating whether the servlet should perform a session-check or not
     */
    public boolean checkSessionFlag = true;
    /**
     * Flag indicating whether the servlet should perform a permissions-check or not
     */
    public boolean checkPermissionsFlag = true;
    /**
     * Flag indicating whether the servlet should log the successful operations
     */
    public boolean loggableFlag = false;
    /**
     * Flag indicating whether the servlet should log the errors
     */
    public boolean loggableErrorsFlag = false;    
    /**
     *  Flag indicating whether the servlet encodes the request to utf-8 format or not
     */
    public boolean utf8CharacterEncodingRequest = false;

    public String neo4jDatabaseFolder = "";

    @Override
    public final void init() {

        checkPermissionsFlag = defineCheckPermissionsFlag();
        checkSessionFlag = defineCheckSessionFlag();
        loggableErrorsFlag = defineLoggableErrorsFlag();
        loggableFlag = defineLoggableFlag();
        utf8CharacterEncodingRequest = defineUtf8CharacterEncodingRequest();

        initServlet();

    }

    /** 
     * Logic for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request Servlet request
     * @param response Servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private void servletLogic(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (utf8CharacterEncodingRequest) {
            request.setCharacterEncoding("utf-8");
        }

        BasicSession session = null;
        Bio4jManager manager = null;

        boolean proceed = true;
        boolean noSessionFlag = false; //Flag indicating whether there is a valid session or not

        String requestString = (String) request.getParameter(PARAMETER_NAME);

        Request myRequest = null;
        Response myResponse = new Response();


        //------Objects stored in the session are declared here-------//
        // For example: User user = null;
        //---------------------------------------------------------------

        try {
            myRequest = new Request(requestString);



            //Getting the session
            session = ActiveSessions.getSession(myRequest.getSessionID());

            if (checkSessionFlag) {
                if (session == null) {
                    this.noSession(myRequest);
                    proceed = false;
                    noSessionFlag = true;
                } else {

                    if (!myRequest.getSessionID().equals(session.getAttribute(SessionAttributes.SESSION_ID_ATTRIBUTE))) {
                        this.noSession(myRequest);
                        noSessionFlag = true;
                        proceed = false;
                    } else {
                        if (checkPermissionsFlag) {
                            proceed = checkPermissions((ArrayList<?>) session.getAttribute(SessionAttributes.PERMISSIONS_ATTRIBUTE), myRequest);

                            if (!proceed) {
                                myResponse.setError(ACCESS_DENIED_MESSAGE);
                            }
                        }

                        //----Reseting the timeout timer
                        session.resetIdleTime();
                    }
                }
            }

            if (proceed) {

                manager = new Bio4jManager(neo4jDatabaseFolder);

                myResponse = processRequest(myRequest, session, manager, request);
                //--> Assigning the request id to its response
                myResponse.setId(myRequest.getId());
                //--> Assigning the request method to its response
                myResponse.setMethod(myRequest.getMethod());


                if (myResponse.getStatus().equals(Response.ERROR_RESPONSE)) {
                    myRequest.detach();
                    myResponse.setRequestSource(myRequest);
                } else {
                    myResponse.setStatus(Response.SUCCESSFUL_RESPONSE);
                }


                if (loggableFlag) {

                    if (myResponse.getStatus().equals(Response.SUCCESSFUL_RESPONSE)) {
                        /*
                         * The call to logSuccessfulOperation will include as many parameters as needed
                         * to perform the successful logging operation.
                         * (For example, the logged user could be passed as a parameter)
                         *
                         * this.logSuccessfulOperation(myRequest,myResponse,connection,user);
                         *
                         */
                        this.logSuccessfulOperation(myRequest, myResponse, manager, session);
                    } else if (myResponse.getStatus().equals(Response.ERROR_RESPONSE)) {
                        /*
                         * The call to logSuccessfulOperation will include as many parameters as needed
                         * to perform the error logging operation.
                         * (For example, the logged user could be passed as a parameter)
                         *
                         * this.logErrorResponseOperation(myRequest,myResponse,connection,user);
                         *
                         */
                        this.logErrorResponseOperation(myRequest, myResponse, manager, session);
                    }
                }

                //--------> Writing the response <---------------
                // set headers
                //--------------THIS PIECE OF CODE CORRESPONDS TO BINARY RESPONSES LIKE FILES-------------
                if (myResponse.isBinary()) {

                    response.setContentType("application/x-download");
                    String filename = myResponse.getRoot().getChild("file").getAttributeValue("name");
                    String fileContent = myResponse.getRoot().getChildText("file");
                    response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                    // write file
                    ServletOutputStream out = response.getOutputStream();

                    out.write(fileContent.getBytes());
                    response.setContentLength(fileContent.getBytes().length);
                    out.flush();
                    out.close();
                } //---------------------------------------------------------------------------------------
                //---------------> STANDARD RESPONSES---------------
                else {
                    response.setContentType("text/html");
                    // write response
                    PrintWriter writer = response.getWriter();
                    writer.println(myResponse.toString());
                    writer.close();
                }
                //------------------------------------------------


            } else {

                //--> Assigning the request id to its response
                myResponse.setId(myRequest.getId());
                //--> Assigning the request method to its response
                myResponse.setMethod(myRequest.getMethod());

                if (noSessionFlag) {
                    myResponse.setStatus(Response.NO_SESSION_RESPONSE);
                } else {
                    myResponse.setStatus(Response.ERROR_RESPONSE);
                }
                //--------> Writing the response <---------------
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                writer.println(myResponse.toString());
                writer.close();
                //------------------------------------------------
            }


        } catch (Throwable e) {
            e.printStackTrace();
            if (loggableErrorsFlag) {
                /*
                 * The call to logErrorExceptionOperation will include as many parameters as needed
                 * to perform the error exception logging operation.
                 * (For example, the logged user could be passed as a parameter)
                 *
                 * this.logErrorExceptionOperation(myRequest,myResponse, user, e,connection);
                 *
                 */
                this.logErrorExceptionOperation(myRequest, myResponse, e, manager);
            }
        } finally {

        }
    }

    /**
     * Method for the logic of the servlet
     */
    protected abstract Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest httpRequest) throws Throwable;

    /**
     * Method called when the operation has been performed successfully plus the flag
     * 'loggableFlag' is true
     */
    protected abstract void logSuccessfulOperation(Request request, Response response, Bio4jManager manager,
            BasicSession session);

    /**
     * Method called when the operation could not be performed because of an error ocurred
     * in the processRequest method
     * This method is called as long as 'loggableErrorsFlag' is true
     */
    protected abstract void logErrorResponseOperation(Request request, Response response, Bio4jManager manager,
            BasicSession session);

    /**
     * Method called when the operation could not be performed because of an exception ocurred
     * This method is called as long as 'loggableErrorsFlag' is true
     */
    protected abstract void logErrorExceptionOperation(Request request, Response response, Throwable e,
            Bio4jManager manager);

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        servletLogic(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        servletLogic(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Basic servlet neo4j";
    }

    /**
     * Method called when a valid session is needed for this servlet but it was not created when
     * servlet was called.
     */
    protected abstract void noSession(Request request);

    /**
     *
     * @param userPermissions
     * @return Whether the user has permissions or not
     */
    protected abstract boolean checkPermissions(ArrayList<?> userPermissions, Request request);

    /**
     * This method must be implemented in order to define the check session flag
     * @return True if a valid session is needed, false otherwise.
     */
    protected abstract boolean defineCheckSessionFlag();

    /**
     * This method must be implemented in order to define the check permissions flag
     * @return True if the permissions of the user must be checked, false otherwise.
     */
    protected abstract boolean defineCheckPermissionsFlag();

    /**
     * This method must be implemented in order to define the loggable flag
     * @return True if requests must be logged, false otherwise.
     */
    protected abstract boolean defineLoggableFlag();

    /**
     * This method must be implemented in order to define the loggable flag
     * @return True if error requests must be logged, false otherwise.
     */
    protected abstract boolean defineLoggableErrorsFlag();

    /**
     * This method must be implemented in order to define the
     * utf-8 character encoding flag
     * @return True if the request will be encoded, false otherwise.
     */
    protected abstract boolean defineUtf8CharacterEncodingRequest();

    protected abstract String defineNeo4jDatabaseFolder();

    /**
     * This method is equivalent to the method init() from the HttpServlet class
     */
    protected abstract void initServlet();
}
