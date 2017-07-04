/*
 * Copyright (c) 2012, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.umass.xml.DomElement;

import static de.umass.util.StringUtilities.isMD5;
import static de.umass.util.StringUtilities.map;
import static de.umass.util.StringUtilities.md5;

/**
 * Provides bindings for the authentication methods of the last.fm API.
 * See <a href="http://www.last.fm/api/authentication">http://www.last.fm/api/authentication</a> for
 * authentication methods.
 *
 * @author Janni Kovacs
 * @see Session
 */
public class Authenticator {

	private Authenticator() {
	}

	/**
	 * Create a web service session for a user. Used for authenticating a user when the password can be inputted by the user.
	 *
	 * @param username last.fm username
	 * @param password last.fm password in cleartext or 32-char md5 string
	 * @param apiKey The API key
	 * @param secret Your last.fm API secret
	 * @return a Session instance
	 * @see Session
	 */
	public static Session getMobileSession(String username, String password, String apiKey, String secret) {
		if (!isMD5(password))
			password = md5(password);
		String authToken = md5(username + password);
		Map<String, String> params = map("api_key", apiKey, "username", username, "authToken", authToken);
		String sig = createSignature("auth.getMobileSession", params, secret);
		Result result = Caller.getInstance()
				.call("auth.getMobileSession", apiKey, "username", username, "authToken", authToken, "api_sig", sig);
		DomElement element = result.getContentElement();
		return Session.sessionFromElement(element, apiKey, secret);
	}

	/**
	 * Fetch an unathorized request token for an API account.
	 *
	 * @param apiKey A last.fm API key.
	 * @return a token
	 */
	public static String getToken(String apiKey) {
		Result result = Caller.getInstance().call("auth.getToken", apiKey);
		return result.getContentElement().getText();
	}

	/**
	 * Fetch a session key for a user.
	 *
	 * @param token A token returned by {@link #getToken(String)}
	 * @param apiKey A last.fm API key
	 * @param secret Your last.fm API secret
	 * @return a Session instance
	 * @see Session
	 */
	public static Session getSession(String token, String apiKey, String secret) {
		String m = "auth.getSession";
		Map<String, String> params = new HashMap<String, String>();
		params.put("api_key", apiKey);
		params.put("token", token);
		params.put("api_sig", createSignature(m, params, secret));
		Result result = Caller.getInstance().call(m, apiKey, params);
		return Session.sessionFromElement(result.getContentElement(), apiKey, secret);
	}

	static String createSignature(String method, Map<String, String> params, String secret) {
		params = new TreeMap<String, String>(params);
		params.put("method", method);
		StringBuilder b = new StringBuilder(100);
		for (Entry<String, String> entry : params.entrySet()) {
			b.append(entry.getKey());
			b.append(entry.getValue());
		}
		b.append(secret);
		return md5(b.toString());
	}
}
