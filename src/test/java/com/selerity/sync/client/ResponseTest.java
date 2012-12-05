package com.selerity.sync.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

public class ResponseTest {

	@Test
	public void testErrorAndResult() {
		Response response = new Response(new JsonPrimitive("foo"), new DispatchException(DispatchException.OTHER_ERROR,
						"some error"), "id123", "fooAPI", "fooType", false);
		assertEquals(true, response.isError());  // this is an error since the error field is non-null (even though result is also not null);
		assertEquals("foo", response.getResult().getAsString());
	}

}
