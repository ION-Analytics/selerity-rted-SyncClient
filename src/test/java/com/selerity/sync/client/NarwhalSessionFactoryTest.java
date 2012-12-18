/** 
 * (C) Copyright Selerity, Inc. 2009-2012. All rights reserved. This source code
 * is confidential and proprietary information of Selerity Inc. and may be used
 * only by a recipient designated by and for the purposes permitted by Selerity
 * Inc. in writing. Reproduction of, dissemination of, modifications to or
 * creation of derivative works from this source code, whether in source or
 * binary forms, by any means and in any form or manner, is expressly
 * prohibited, except with the prior written permission of Selerity Inc.. THIS
 * CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may
 * not be removed from the software by any user thereof.
 */

package com.selerity.sync.client;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;


/**
 * Tests of the NarwhalSessionFactory, specifically logic related to creating, expiring, extending and invalidating sessions.
 */

public class NarwhalSessionFactoryTest {

	@Test
	public void testCreateSessionAndExpiry() throws DispatchException, InterruptedException {
		MockNarwhalServiceImpl mockService = new MockNarwhalServiceImpl("mock");
		// a response that's valid for 5 minutes and 200 ms	
		mockService.addResponse("{\"id\":\"1\",\"result\":{\"leaseExpiration\":" + getExpiryPlus(200) + ",\"id\":\"2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929\"},\"error\":null,\"header\":null}");
		NarwhalSessionFactory factory = new NarwhalSessionFactory(mockService, "testClient", "testUser", "testPassword");
		
		// this should force it to look up a session which is valid for only another 100 ms (not including the 5 minute buffer)
		Session session = factory.getInstance();
		assertEquals("2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929", session.getHeaderParameter(Session.TOKEN));
		Thread.sleep(10);
		
		// the session should still be valid
		session = factory.getInstance();
		assertEquals("2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929", session.getHeaderParameter(Session.TOKEN));
		
		Thread.sleep(200);  // sleep through expiry
		
		// show that old session can't be extended
		mockService.addResponse("{id\":\"2\",\"result\":null,\"error\":{\"code\":-1000,\"message\":\"Session not valid for user testUser with token 2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929\"},\"header\":null}");

		// and a new session
		mockService.addResponse("{\"id\":\"3\",\"result\":{\"leaseExpiration\":" + getExpiryPlus(200) + ",\"id\":\"001595da-93e9-441e-aba3-d28e2d518846\"},\"error\":null,\"header\":null}");
		
		// the old session should be expired, we should have the new session now
		session = factory.getInstance();
		assertEquals("001595da-93e9-441e-aba3-d28e2d518846", session.getHeaderParameter(Session.TOKEN));
		
		NarwhalSessionFactory.clearAllSessions();
		
	}
	
	
	@Test
	public void testInvalidation() throws DispatchException, InterruptedException {
		MockNarwhalServiceImpl mockService = new MockNarwhalServiceImpl("mock");
		// a response that's valid for 1 minute		
		mockService.addResponse("{\"id\":\"1\",\"result\":{\"leaseExpiration\":" + getExpiryPlus(60000) + ",\"id\":\"2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929\"},\"error\":null,\"header\":null}");
		NarwhalSessionFactory factory = new NarwhalSessionFactory(mockService, "testClient", "testUser", "testPassword");
		
		// this should force it to look up a session 
		Session session = factory.getInstance();
		assertEquals("2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929", session.getHeaderParameter(Session.TOKEN));
		Thread.sleep(10);
		
		// the session should still be valid
		session = factory.getInstance();
		assertEquals("2c2c01ab-9fa4-4db1-b6e0-ce5858ad2929", session.getHeaderParameter(Session.TOKEN));
		
		// now explicity invalidate it.
		factory.invalidateSession(session);
		
		// get ready for a new session
		mockService.addResponse("{\"id\":\"2\",\"result\":{\"leaseExpiration\":" + getExpiryPlus(200) + ",\"id\":\"001595da-93e9-441e-aba3-d28e2d518846\"},\"error\":null,\"header\":null}");
		// the old session was invalidated so we should have a new session now
		session = factory.getInstance();
		assertEquals("001595da-93e9-441e-aba3-d28e2d518846", session.getHeaderParameter(Session.TOKEN));
		
		NarwhalSessionFactory.clearAllSessions();
	}
	
	private long getExpiryPlus(long millisPastMinimum){
		return new Date().getTime() + NarwhalSessionFactory.SYNC_SESSION_MIN_VALID_TIME_MILLIS + millisPastMinimum;
	}

}
