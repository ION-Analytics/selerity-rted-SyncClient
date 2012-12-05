/*
 * (c) Copyright Selerity, Inc. 2009-2012. All rights reserved. This source code is confidential
 * and proprietary information of Selerity Inc. and may be used only by a recipient designated
 * by and for the purposes permitted by Selerity Inc. in writing.  Reproduction of, dissemination
 * of, modifications to or creation of derivative works from this source code, whether in source
 * or binary forms, by any means and in any form or manner, is expressly prohibited, except with
 * the prior written permission of Selerity Inc..  THIS CODE AND INFORMATION ARE PROVIDED "AS IS"
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may not be
 * removed from the software by any user thereof.
 */

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
