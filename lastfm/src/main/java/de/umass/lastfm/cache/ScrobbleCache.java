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

package de.umass.lastfm.cache;

import java.io.IOException;
import java.util.Collection;

import de.umass.lastfm.scrobble.Scrobbler;
import de.umass.lastfm.scrobble.SubmissionData;

/**
 * A <code>ScrobbleCache</code> is able to cache {@link SubmissionData} instances for later submission
 * to the Last.fm servers.
 *
 * @author Janni Kovacs
 * @deprecated The 1.2.x scrobble protocol has now been deprecated in favour of the 2.0 protocol which is part of the Last.fm web services
 *             API.
 */
@Deprecated
public interface ScrobbleCache {

	/**
	 * Caches one or more {@link de.umass.lastfm.scrobble.SubmissionData}.
	 *
	 * @param submissions The submissions
	 */
	public void cacheScrobble(SubmissionData... submissions);

	/**
	 * Caches a collection of {@link SubmissionData}.
	 *
	 * @param submissions The submissions
	 */
	public void cacheScrobble(Collection<SubmissionData> submissions);

	/**
	 * Checks if the cache contains any scrobbles.
	 *
	 * @return <code>true</code> if this cache is empty
	 */
	public boolean isEmpty();

	/**
	 * Tries to scrobble all cached scrobbles. If it succeeds the cache will be empty afterwards.
	 * If this method fails an IOException is thrown and no entries are removed from the cache.
	 *
	 * @param scrobbler A {@link Scrobbler} instance
	 * @throws java.io.IOException on I/O errors
	 * @throws IllegalStateException if the {@link Scrobbler} is not fully initialized (i.e. no handshake performed)
	 */
	public void scrobble(Scrobbler scrobbler) throws IOException;

	/**
	 * Clears all cached scrobbles from this cache.
	 */
	public void clearScrobbleCache();
}
