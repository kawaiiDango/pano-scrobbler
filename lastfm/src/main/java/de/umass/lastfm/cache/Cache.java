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

import java.io.InputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.umass.util.StringUtilities;

/**
 * The <code>Cache</code> handles storing and loading to a permanent storage for last.fm api requests. This could be
 * a file system or a sql database.
 *
 * @author Janni Kovacs
 * @see de.umass.lastfm.Caller#setCache(Cache)
 * @see de.umass.lastfm.cache.ExpirationPolicy
 */
public abstract class Cache {

	private static boolean hashCacheEntryNames = true;

	private ExpirationPolicy expirationPolicy;

	protected Cache() {
		expirationPolicy = new DefaultExpirationPolicy();
	}

	/**
	 * Returns the active {@link de.umass.lastfm.cache.ExpirationPolicy}
	 *
	 * @return the ExpirationPolicy
	 */
	public ExpirationPolicy getExpirationPolicy() {
		return expirationPolicy;
	}

	/**
	 * Sets the active {@link de.umass.lastfm.cache.ExpirationPolicy}.
	 *
	 * @param expirationPolicy An ExpirationPolicy, not <code>null</code>
	 */
	public void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
		if (expirationPolicy == null)
			throw new NullPointerException("policy == null");
		this.expirationPolicy = expirationPolicy;
	}

	/**
	 * Checks if the cache contains an entry with the given name.
	 *
	 * @param cacheEntryName An entry name
	 * @return <code>true</code> if the cache contains this entry
	 */
	public abstract boolean contains(String cacheEntryName);

	/**
	 * Loads the specified entry from the cache and returns an InputStream to be read from. Returns <code>null</code>
	 * if the cache does not contain the specified cacheEntryName.
	 *
	 * @param cacheEntryName An entry name
	 * @return an InputStream or <code>null</code>
	 */
	public abstract InputStream load(String cacheEntryName);

	/**
	 * Removes the specified entry from the cache if available. Does nothing if no such entry is
	 * available.
	 *
	 * @param cacheEntryName An entry name
	 */
	public abstract void remove(String cacheEntryName);

	/**
	 * Stores a request in the cache.
	 *
	 * @param cacheEntryName The entry name to be stored to
	 * @param inputStream An InputStream containing the data to be cached
	 * @param expirationDate The date of expiration represented in milliseconds since 1.1.1970
	 */
	public abstract void store(String cacheEntryName, InputStream inputStream, long expirationDate);

	/**
	 * Checks if the specified entry is expired.
	 *
	 * @param cacheEntryName An entry name
	 * @return <code>true</code> if the entry is expired
	 */
	public abstract boolean isExpired(String cacheEntryName);

	/**
	 * Clears the cache by effectively removing all cached data.
	 */
	public abstract void clear();

	/**
	 * Finds the expiration date, returned as a unix timestamp, for a given method/parameters combination, or -1 if
	 * there's no expiration time found in this Cache's {@link ExpirationPolicy}.<br/>
	 * It uses this cache's {@link ExpirationPolicy} and the current timestamp to calculate the expiration date.
	 *
	 * @param method The method called
	 * @param params The parameters sent
	 * @return The expiration date for this specific API call, or -1 if no expiration date was found
	 * @see ExpirationPolicy#getExpirationTime(String, java.util.Map) 
	 */
	public final long findExpirationDate(String method, Map<String, String> params) {
		long expirationTime = this.getExpirationPolicy().getExpirationTime(method, params);
		long expirationDate = -1;
		if (expirationTime > 0) {
			if (expirationTime == Long.MAX_VALUE) {
				expirationDate = Long.MAX_VALUE;
			} else {
				expirationDate = System.currentTimeMillis() + expirationTime;
			}
		}
		return expirationDate;
	}

	/**
	 * Creates a unique entry name string for a request. It consists of the method name and all the parameter names
	 * and values concatenated in alphabetical order. It is used to identify cache entries in the backend storage.
	 * If <code>hashCacheEntryNames</code> is set to <code>true</code> this method will return a MD5 hash of
	 * the generated name.
	 *
	 * @param method The request method
	 * @param params The request parameters
	 * @return a cache entry name
	 */
	public static String createCacheEntryName(String method, Map<String, String> params) {
		if (!(params instanceof SortedMap)) {
			params = new TreeMap<String, String>(params);
		}
		StringBuilder b = new StringBuilder(100);
		b.append(method.toLowerCase());
		b.append('.');
		for (Map.Entry<String, String> e : params.entrySet()) {
			b.append(e.getKey());
			b.append(e.getValue());
		}
		String name = b.toString();
		if (hashCacheEntryNames)
			return StringUtilities.md5(name);
		return StringUtilities.cleanUp(name);
	}

	/**
	 * If <code>hashCacheEntryNames</code> is set to true the {@link #createCacheEntryName} method  will
	 * return a hash of the original entry name instead of the name itself.
	 *
	 * @param hashCacheEntryNames <code>true</code> to generate hashes
	 */
	public static void setHashCacheEntryNames(boolean hashCacheEntryNames) {
		Cache.hashCacheEntryNames = hashCacheEntryNames;
	}
}
