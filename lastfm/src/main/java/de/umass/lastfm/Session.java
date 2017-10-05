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

import de.umass.xml.DomElement;

/**
 * Contains Session data relevant for making API calls which require authentication.
 * A <code>Session</code> instance is passed to all methods requiring previous authentication.
 *
 * @author Janni Kovacs
 * @see de.umass.lastfm.Authenticator
 */
public class Session {

	private String apiKey;
	private String secret;
	private String username;
	private String key;
	private boolean subscriber;

	private Session() {
	}

	/**
	 * Restores a Session instance with the given session key.
	 *
	 * @param apiKey An api key
	 * @param secret A secret
	 * @param sessionKey The previously obtained session key
	 * @return a Session instance
	 */
	public static Session createSession(String apiKey, String secret, String sessionKey) {
		return createSession(apiKey, secret, sessionKey, null, false);
	}

	/**
	 * Restores a Session instance with the given session key.
	 *
	 * @param apiKey An api key
	 * @param secret A secret
	 * @param sessionKey The previously obtained session key
	 * @param username A Last.fm username
	 * @param subscriber Subscriber status
	 * @return a Session instance
	 */
	public static Session createSession(String apiKey, String secret, String sessionKey, String username,
										boolean subscriber) {
		Session s = new Session();
		s.apiKey = apiKey;
		s.secret = secret;
		s.key = sessionKey;
		s.username = username;
		s.subscriber = subscriber;
		return s;
	}

	public String getSecret() {
		return secret;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getKey() {
		return key;
	}

	public boolean isSubscriber() {
		return subscriber;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public String toString() {
		return "Session[" +
				"apiKey=" + apiKey +
				", secret=" + secret +
				", username=" + username +
				", key=" + key +
				", subscriber=" + subscriber +
				']';
	}

	static Session sessionFromElement(DomElement element, String apiKey, String secret) {
		if (element == null)
			return null;
		String user = element.getChildText("name");
		String key = element.getChildText("key");
		boolean subsc = element.getChildText("subscriber").equals("1");
		return createSession(apiKey, secret, key, user, subsc);
	}
}
