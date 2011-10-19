package com.selerity.sync.client.async;

import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.FullRequest;

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
 * A transport that suppots an explicitly asynchronous call/response model with callbacks to an
 * AsyncTransportListener for responses.
 *
 */

public interface AsyncTransport{
	
	public void addAsyncTransportListener(AsyncTransportListener listener);

	public void asyncDispatch(FullRequest request) throws DispatchException;
	
}
