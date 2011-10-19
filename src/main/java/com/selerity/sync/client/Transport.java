package com.selerity.sync.client;

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
 *  A Transport is an abstraction of the mechanism for carrying a Narwhal request from the client 
 *  to the server and then bringing the response back.  The transport abstracts away issues like
 *  physical connectivity, text encoding, compression, etc.
 * 
 *
 */
public interface Transport {
	
	public Response syncDispatch(FullRequest request) throws DispatchException;
	
	public Response[] boxcarDispatch(FullRequest[] requests) throws DispatchException;

}
