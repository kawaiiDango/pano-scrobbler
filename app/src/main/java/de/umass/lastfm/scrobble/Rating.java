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

/**
 * The rating of the track. See <a href="http://www.last.fm/api/submissions#subs">http://www.last.fm/api/submissions#subs</a>
 * for more information.
 *
 * @author Lukasz Wisniewski
 * @deprecated The 1.2.x scrobble protocol has now been deprecated in favour of the 2.0 protocol which is part of the Last.fm web services API.
 */
@Deprecated
public enum Rating {

	/**
	 * Love (on any mode if the user has manually loved the track). This implies a listen.
	 */
	LOVE("L"),

	/**
	 * Ban (only if source=L). This implies a skip, and the client should skip to the next track when a ban happens
	 */
	BAN("B"),

	/**
	 * Skip (only if source=L)
	 */
	SKIP("S");

	private String code;

	Rating(String code) {
		this.code = code;
	}

	/**
	 * Returns the corresponding code for this rating.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}