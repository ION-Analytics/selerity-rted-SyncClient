package com.selerity.sync.client;

import java.net.MalformedURLException;

import com.google.gson.JsonElement;

public abstract class AbstractSyncClient {

	private Session session;
	private final Dispatcher dispatcher;
	protected final String clientAppName;
	
	/** Initialize the dispatcher and start a session.
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public AbstractSyncClient(String host, int port, String clientAppName) throws MalformedURLException, DispatchException{
		// initialized the transport and method dispatcher
		String rhinoURL = "http://" + host + ":" + port + "/rhino-1.0-SNAPSHOT/rpc.do";
		Transport transport = new RhinoHTTPTransport(rhinoURL, false);
		dispatcher = new RhinoDispatcher(transport);
		
		this.clientAppName = clientAppName;
	}


	/** Dispatch the request using the current session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatch(Request request) throws DispatchException{
		if (session == null){
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "Session not started", null);
		}
		return dispatcher.dispatch(request, session); 	
	}
	
	/** Dispatch the array of requests as a single boxcar using the current session.
	 * 
	 * @param requests
	 * @return
	 * @throws DispatchException
	 */
	public Response[] boxcarDispatch(Request[] requests) throws DispatchException{
		if (session == null){
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "Session not started", null);
		}
		return dispatcher.boxcarDispatch(requests, session); 	
	}
	
	/** Starts a session in normal mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public void startSession(String user, String password) throws DispatchException{
		session = new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, null, user, password);
	}
	
	/** Starts a session in the given mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public void startSession(String user, String password, String mode) throws DispatchException{
		session = new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, mode, user, password);
	}
	
	/** Starts a session in "extension" mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public void startSessionExtensionMode(String user, String password) throws DispatchException{
		session = new RhinoSessionFactory().getInstance(dispatcher,
				clientAppName, "extension", user, password);
	}
	
	/** Extends the current session.
	 * 
	 * @throws DispatchException
	 */
	public void extendSession() throws DispatchException{
		Request extendRequest = new Request("AuthenticationHandler.extend");
		dispatch(extendRequest);
	}
	
	
	/** Closes the current session.
	 * 
	 * @throws DispatchException
	 */
	public void closeSession() throws DispatchException{
		Request logoutRequest = new Request("AuthenticationHandler.invalidate");
		dispatch(logoutRequest);
	}
	
}
