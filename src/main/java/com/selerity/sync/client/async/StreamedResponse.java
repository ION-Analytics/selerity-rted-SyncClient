package com.selerity.sync.client.async;

import com.selerity.sync.client.Response;

/** 
 * © Copyrights Selerity, Inc. 2009-2011. All rights reserved. This source code is confidential 
 * and proprietary information of Selerity Inc. and may be used only by a recipient designated by 
 * and for the purposes permitted by Selerity Inc. in writing.  Reproduction of, dissemination of, 
 * modifications to or creation of derivative works from this source code, whether in source or 
 * binary forms, by any means and in any form or manner, is expressly prohibited, except with the 
 * prior written permission of Selerity Inc..  THIS CODE AND INFORMATION ARE PROVIDED ÒAS ISÓ 
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may not be 
 * removed from the software by any user thereof. 
 * 
 * Represents a sequence of one or more responses.  It has two conceptual status flags:
 * 
 * 'hasMore' which indicates that there may be more responses coming.  If 'hasMore' is false 
 * then no further responses are expected.
 * 
 * 'wantsMore' which indicates that the consumer is willing to accept more responses.  
 * If 'wantsMore' is false then no further responses will be accepted and the producer should
 * stop.
 * 
 * 
 * @author andrewbrook
 *
 */

public interface StreamedResponse {



	/** Returns true if there are responses pending (either already added or anticipated).
	 * 
	 *  Note that setting the 'more' flag to false does not make this method return false until the queue has been drained.
	 * 
	 * @return
	 */
	public boolean hasMore();
	
	/** Allows the producer to indicate that they intend to produce no more responses.  This does
	 *  not immediately cause hasMore() to return false since there may still be some responses enqueued.
	 * 
	 */
	public void setHasNoMore();
	
	/** Returns true if the consumer wants to receive more responses.  If false then
	 *  the producer should not send any further responses (and can discard the object).
	 * 
	 * @return
	 */
	public boolean wantsMore();
	
	/** Allows the consumer to indicate that they don't want any more responses.  This immediately
	 *  causes wantsMore() to return false.
	 * 
	 */
	public void setWantsNoMore();

	/** Blocks until the next response is available.  Returns null if no more responses are coming.
	 *  May block indefinitely if the writer doesn't set the 'more' flag to false.
	 * 
	 * @return
	 */
	public Response getNextResponse();

	/** Blocks until the next response is available.  Returns null if no more responses are coming.
	 *  May time out and return null if the wait time is exceeeded.
	 * 
	 * @return
	 */
	public Response getNextResponse(long timeoutMillis);
	

}