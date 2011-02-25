package com.selerity.sync.client;

import java.net.MalformedURLException;

import com.google.gson.JsonElement;

public abstract class AbstractSyncClient {

	protected final String clientAppName;
	private Dispatcher dispatcher;
	private String user;
	private String password;
	
	/** Initialize the dispatcher and start a session.
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public AbstractSyncClient(String host, int port, String user, String password, String clientAppName) throws MalformedURLException, DispatchException{
		// initialized the transport and method dispatcher
		String rhinoURL = "http://" + host + ":" + port + "/rhino-1.0-SNAPSHOT/rpc.do";
		Transport transport = new RhinoHTTPTransport(rhinoURL, false);
		dispatcher = new RhinoDispatcher(transport);
		
		this.clientAppName = clientAppName;
		this.user = user;
		this.password = password;
	}


	/** Dispatch the request using the current session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		return dispatcher.dispatch(request, session); 	
	}
	
	/** Create a paginated response iterator using the current session.  Requires the name of the parameter object that
	 *  will carry the limit and offset parameters.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public PaginatedResponseIterator paginatedDispatch(Request request, Session session, String optionObjectName, int limit) throws DispatchException{
		return new PaginatedResponseIterator(dispatcher, session, request, optionObjectName, limit);
	}
	
	/** Dispatch the array of requests as a single boxcar using the current session.
	 * 
	 * @param requests
	 * @return
	 * @throws DispatchException
	 */
	public Response[] boxcarDispatch(Request[] requests, Session session) throws DispatchException{
		return dispatcher.boxcarDispatch(requests, session); 	
	}
	
	/** Updates the user name to be used in future sessions
	 * 
	 * @param user
	 */
	public void setUser(String user){
		this.user = user;
	}
	
	/** Updates the password to be used in future sessions
	 * 
	 * @param password
	 */
	public void setPassword(String password){
		this.password = password;
	}
	
	/** Starts a session in normal mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession() throws DispatchException{
		return new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, null, user, password);
	}
	
	/** Starts a session in the given mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession(String mode) throws DispatchException{
		return new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, mode, user, password);
	}
	
	/** Starts a session in "extension" mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSessionExtensionMode() throws DispatchException{
		return new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, "extension", user, password);
	}
	
	/** Extends the current session.
	 * 
	 * @throws DispatchException
	 */
	public void extendSession(Session session) throws DispatchException{
		Request extendRequest = new Request("AuthenticationHandler.extend");
		dispatch(extendRequest, session);
	}
	
	
	/** Closes the current session.
	 * 
	 * @throws DispatchException
	 */
	public void closeSession(Session session) throws DispatchException{
		Request logoutRequest = new Request("AuthenticationHandler.invalidate");
		dispatch(logoutRequest, session);
	}
	
}
