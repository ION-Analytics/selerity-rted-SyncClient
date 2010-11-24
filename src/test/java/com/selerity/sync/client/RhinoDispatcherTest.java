package com.selerity.sync.client;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoDispatcher;
import com.selerity.sync.client.RhinoSession;
import com.selerity.sync.client.Session; 

public class RhinoDispatcherTest {

	
	@Test
	public void testSingleRequestNoParams() throws DispatchException{
		Request request = new Request("myHandler.fooMethod");
		
		String expectedRequestJson = "{\"method\":\"myHandler.fooMethod\","
			+ "\"params\":{},"
			+ "\"header\":{\"user\":\"testUser\",\"token\":\"TEST-TOKEN\",\"client\":\"testClient\",\"mode\":\"extension\"},"
			+ "\"id\":\"0\"}";
		
		
		assertDispatchRequest(request, expectedRequestJson);
	}
	
	@Test
	public void testSingleRequestStringParam() throws DispatchException{
		Request request = new Request("myHandler.fooMethod");
		request.setMethodParameter("testString", "stringValue");
		
		String expectedRequestJson = "{\"method\":\"myHandler.fooMethod\","
			+ "\"params\":{\"testString\":\"stringValue\"},"
			+ "\"header\":{\"user\":\"testUser\",\"token\":\"TEST-TOKEN\",\"client\":\"testClient\",\"mode\":\"extension\"},"
			+ "\"id\":\"0\"}";
		
		
		assertDispatchRequest(request, expectedRequestJson);
	}
	
	@Test
	public void testSingleRequestNumParam() throws DispatchException{
		Request request = new Request("myHandler.fooMethod");
		request.setMethodParameter("testNumber", 42);
		
		String expectedRequestJson = "{\"method\":\"myHandler.fooMethod\","
			+ "\"params\":{\"testNumber\":42},"
			+ "\"header\":{\"user\":\"testUser\",\"token\":\"TEST-TOKEN\",\"client\":\"testClient\",\"mode\":\"extension\"},"
			+ "\"id\":\"0\"}";
		
		
		assertDispatchRequest(request, expectedRequestJson);
	}
	
	@Test
	public void testSingleRequestBoolParam() throws DispatchException{
		Request request = new Request("myHandler.fooMethod");
		request.setMethodParameter("testBool", false);
		
		String expectedRequestJson = "{\"method\":\"myHandler.fooMethod\","
			+ "\"params\":{\"testBool\":false},"
			+ "\"header\":{\"user\":\"testUser\",\"token\":\"TEST-TOKEN\",\"client\":\"testClient\",\"mode\":\"extension\"},"
			+ "\"id\":\"0\"}";
		
		
		assertDispatchRequest(request, expectedRequestJson);
	}
	
	@Test
	public void testSingleRequestObjParam() throws DispatchException{
		Request request = new Request("myHandler.fooMethod");
		JsonObject obj = new JsonObject();
		obj.addProperty("stringProp", "value1");
		obj.addProperty("numProp", 2);
		obj.addProperty("boolProp", true);
		request.setMethodParameter("testObj", obj);
		
		String expectedRequestJson = "{\"method\":\"myHandler.fooMethod\","
			+ "\"params\":{\"testObj\":{\"stringProp\":\"value1\",\"numProp\":2,\"boolProp\":true}},"
			+ "\"header\":{\"user\":\"testUser\",\"token\":\"TEST-TOKEN\",\"client\":\"testClient\",\"mode\":\"extension\"},"
			+ "\"id\":\"0\"}";
		
		
		assertDispatchRequest(request, expectedRequestJson);
	}
	
	public void assertDispatchRequest(Request request, String expectedRequestJson) throws DispatchException{
		MockTransport transport = new MockTransport();
		RhinoDispatcher dispatcher = new RhinoDispatcher(transport);
		Session session = getSession();
		transport.addResponse("{}", "null");
		
		JsonElement result = dispatcher.dispatch(request, session);
	
		String actualRequestJson = transport.getRequest(0);
		String actualResultJson = result.toString();
		
		assertEquals(expectedRequestJson, actualRequestJson);
		assertEquals("{}", actualResultJson);
		
	}
	
	
	protected Session getSession(){
		return new RhinoSession("testUser", "testClient", "TEST-TOKEN", "extension");
	}
	
}
