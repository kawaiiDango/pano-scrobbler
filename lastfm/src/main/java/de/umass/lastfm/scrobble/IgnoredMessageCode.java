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
package de.umass.lastfm.scrobble;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the ignored message code returned by scrobble and now playing requests.
 *
 * @author Adrian Woodhead
 */
public enum IgnoredMessageCode {

	ARTIST_IGNORED(1),
	TRACK_IGNORED(2),
	TIMESTAMP_TOO_OLD(3),
	TIMESTAMP_TOO_NEW(4),
	DAILY_SCROBBLE_LIMIT_EXCEEDED(5);

	/**
	 * The ignored message error id returned by the Last.fm API.
	 */
	private int codeId;

	/**
	 * A map which maps error codes against their corresponding enums for lookups by code.
	 */
	private static Map<Integer, IgnoredMessageCode> idToCodeMap = new HashMap<Integer, IgnoredMessageCode>();

	static {
		for (IgnoredMessageCode code : IgnoredMessageCode.values()) {
			idToCodeMap.put(code.getCodeId(), code);
		}
	}

	private IgnoredMessageCode(int code) {
		this.codeId = code;
	}

	/**
	 * Gets the IgnoredMessage enum value for the passed Last.fm error code.
	 *
	 * @param code The Last.fm error code.
	 * @return The appopriate IgnoredMessage enum value.
	 */
	public static IgnoredMessageCode valueOfCode(int code) {
		IgnoredMessageCode messageCode = idToCodeMap.get(code);
		if (messageCode != null) {
			return messageCode;
		}
		throw new IllegalArgumentException("No IgnoredMessageCode for code " + code);
	}

	private int getCodeId() {
		return codeId;
	}

}
