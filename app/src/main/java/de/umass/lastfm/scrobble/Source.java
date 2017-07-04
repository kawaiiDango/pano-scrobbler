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
 * The source of the track. See <a href="http://www.last.fm/api/submissions#subs">http://www.last.fm/api/submissions#subs</a> for more
 * information.
 *
 * @author Janni Kovacs
 * @deprecated The 1.2.x scrobble protocol has now been deprecated in favour of the 2.0 protocol which is part of the Last.fm web services
 *             API.
 */
@Deprecated
public enum Source {

	/**
	 * Chosen by the user (the most common value, unless you have a reason for choosing otherwise, use this).
	 */
	USER("P"),

	/**
	 * Non-personalised broadcast (e.g. Shoutcast, BBC Radio 1).
	 */
	NON_PERSONALIZED_BROADCAST("R"),

	/**
	 * Personalised recommendation except Last.fm (e.g. Pandora, Launchcast).
	 */
	PERSONALIZED_BROADCAST("E"),

	/**
	 * Last.fm (any mode). In this case, the 5-digit Last.fm recommendation key must be appended to this source ID
	 * to prove the validity of the submission (for example, "o[0]=L1b48a").
	 */
	LAST_FM("L"),

	/**
	 * Source unknown.
	 */
	UNKNOWN("U");

	private String code;

	Source(String code) {
		this.code = code;
	}

	/**
	 * Returns the corresponding code for this source.
	 *
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}
