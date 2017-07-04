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

import java.io.*;
import java.sql.*;

/**
 * <p>Generic class for caching into a database. Its constructor takes a {@link Connection} instance, which must be opened and closed by the
 * client. SQL code used in this class should work with all common databases (which support the varchar, timestamp and text
 * datatypes).</p>
 * For more specialized versions of this class for different databases one may extend this class and override methods as needed. In
 * most cases overriding {@link #createTable()} will be sufficient.<br/>
 * The following databases are supported and tested with this class:
 * <ul>
 *     <li>MySQL 5</li>
 *     <li>H2 1.3</li>
 * </ul>
 * Not supported by this base class:
 * <ul>
 *     <li>Apache Derby/JavaDB - a long varchar in Derby can only hold up to 32700 characters of text, which is too little for some requests</li>
 *     <li>HSQLDB - does not support the text datatype</li>
 * </ul>
 *
 * @author Janni Kovacs
 */
public class DatabaseCache extends Cache {

	protected static final String DEFAULT_TABLE_NAME = "LASTFM_CACHE";
	protected String tableName;

	protected Connection connection;

	public DatabaseCache(Connection connection) throws SQLException {
		this(connection, DEFAULT_TABLE_NAME);
	}

	/**
	 * Creates a new <code>DatabaseCache</code> with the supplied database {@link Connection} and the specified table name. A new table with
	 * <code>tableName</code> will be created in the constructor, if none exists. Note that <code>tableName</code> will <b>not</b> be
	 * sanitized for usage in SQL, so in the rare case you're using user input for a table name make sure to sanitize the input before
	 * passing it on to prevent SQL injections.
	 *
	 * @param connection The database connection
	 * @param tableName the name for the database table to use
	 * @throws SQLException When initializing/creating the table fails
	 * @see #createTable()
	 */
	public DatabaseCache(Connection connection, String tableName) throws SQLException {
		this.connection = connection;
		this.tableName = tableName;
		// We need this, because some databases do not support CREATE TABLE IF NOT EXISTS, which is a non standard addition
		ResultSet tables = this.connection.getMetaData().getTables(null, null, tableName, null);
		if (!tables.next()) {
			createTable();
		}
	}

	/**
	 * This internal method creates a new table in the database for storing XML responses. You can override this method in a subclass if this
	 * generic method does not work with the database server you are using, given that the following table columns are present:
	 * <ul>
	 *     <li><code>id</code> - The primary key, which is used to identify cache entries (see {@link Cache#createCacheEntryName}</li>
	 *     <li><code>expiration_date</code> - A timestamp field for this cache entry's expiration date</li>
	 *     <li><code>response</code> - The actual response XML</li>
	 * </ul>
	 *
	 * If you choose to use a different schema in your table you'll most likely have to override the other methods in this class, too.
	 *
	 * @throws SQLException When the generic SQL code in this method is not compatible with the database
	 */
	protected void createTable() throws SQLException {
		PreparedStatement stmt = connection.prepareStatement(
				"CREATE TABLE " + tableName + " (id VARCHAR(200) PRIMARY KEY, expiration_date TIMESTAMP, response TEXT)");
		stmt.execute();
		stmt.close();
	}

	public boolean contains(String cacheEntryName) {
		try {
			PreparedStatement stmt = connection.prepareStatement("SELECT id FROM " + tableName + " WHERE id = ?");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			boolean b = result.next();
			stmt.close();
			return b;
		} catch (SQLException e) {
			return false;
		}
	}

	public InputStream load(String cacheEntryName) {
		try {
			PreparedStatement stmt = connection.prepareStatement("SELECT response FROM " + tableName + " WHERE id = ?");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				String s = result.getString("response");
				stmt.close();
				return new ByteArrayInputStream(s.getBytes("UTF-8"));
			}
			stmt.close();
		} catch (SQLException e) {
			// ignore
		} catch (UnsupportedEncodingException e) {
			// won't happen
		}
		return null;
	}

	public void remove(String cacheEntryName) {
		try {
			PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + tableName + " WHERE id = ?");
			stmt.setString(1, cacheEntryName);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			// ignore
		}
	}

	public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
					"INSERT INTO " + tableName + " (id, expiration_date, response) VALUES(?, ?, ?)");
			stmt.setString(1, cacheEntryName);
			stmt.setTimestamp(2, new Timestamp(expirationDate));
			stmt.setCharacterStream(3, new InputStreamReader(inputStream, "UTF-8"), -1);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			// ignore
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
//			won't happen
		}
	}

	public boolean isExpired(String cacheEntryName) {
		try {
			PreparedStatement stmt = connection.prepareStatement("SELECT expiration_date FROM " + tableName + " WHERE id = ?");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				Timestamp timestamp = result.getTimestamp("expiration_date");
				long expirationDate = timestamp.getTime();
				stmt.close();
				return expirationDate < System.currentTimeMillis();
			}
		} catch (SQLException e) {
			// ignore
		}
		return false;
	}

	public void clear() {
		try {
			PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + tableName);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			// ignore
		}
	}
}
