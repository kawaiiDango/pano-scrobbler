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

import android.annotation.TargetApi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.lastfm.scrobble.Scrobbler;
import de.umass.lastfm.scrobble.SubmissionData;
import de.umass.util.StringUtilities;

/**
 * Standard {@link Cache} implementation which is used by default by the {@link de.umass.lastfm.Caller} class.
 * This implementation caches all responses in the file system. In addition to the raw responses it stores a
 * .meta file which contains the expiration date for the specified request.
 *
 * @author Janni Kovacs
 */
@TargetApi(26)
public class FileSystemCacheNio extends Cache implements ScrobbleCache {

    private static final String SUBMISSIONS_FILE = "submissions.txt";

    private String cacheDir;

    public FileSystemCacheNio() {
        this(System.getProperty("user.home") + "/.last.fm-cache");
    }

    public FileSystemCacheNio(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean contains(String cacheEntryName) {
        return Files.exists(Paths.get(cacheDir, cacheEntryName + ".xml"));
    }

    public void remove(String cacheEntryName) {
        try {
            Files.deleteIfExists(Paths.get(cacheDir, cacheEntryName + ".xml"));
        } catch (IOException e) {
        }

        try {
            Files.deleteIfExists(Paths.get(cacheDir, cacheEntryName + ".meta"));
        } catch (IOException e) {
        }
    }

    public boolean isExpired(String cacheEntryName) {
        Path pt = Paths.get(cacheDir, cacheEntryName + ".meta");
        FileLock lock = null;

        if (!Files.exists(pt))
            return false;
        try {
            Properties p = new Properties();
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.READ);
            lock = channel.lock(0L, Long.MAX_VALUE, true);
            InputStream is = Channels.newInputStream(channel);
            p.load(is);
            channel.close();

            long expirationDate = Long.parseLong(p.getProperty("expiration-date"));
            return expirationDate < System.currentTimeMillis();
        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            remove(cacheEntryName);
            return false;
        } finally {
            tryRelease(lock);
        }
    }

    public void clear() {
        final List<Path> pathsToDelete;
        try {
            pathsToDelete = Files.walk(Paths.get(cacheDir)).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        } catch (IOException e) {
            return;
        }
        for (Path path : pathsToDelete) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public InputStream load(String cacheEntryName) {
        FileLock lock = null;
        try {
            File file = new File(cacheDir, cacheEntryName + ".xml");
            FileInputStream fis = new FileInputStream(file);
            // close in FileInputStream calls close for the channel
            lock = fis.getChannel().lock(0L, Long.MAX_VALUE, true);
            return fis;
        } catch (IOException e) {
            tryRelease(lock);
            return null;
        }
    }

    public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
        createCache();
        Path pt = Paths.get(cacheDir, cacheEntryName + ".xml");
        FileLock lock = null;

        try {
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.lock();
            BufferedInputStream is = new BufferedInputStream(inputStream);
            BufferedOutputStream os = new BufferedOutputStream(Channels.newOutputStream(channel));
            int read;
            byte[] buffer = new byte[4096];
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.close();
            is.close();
            channel.close();

            FileChannel channelMeta = FileChannel.open(Paths.get(cacheDir, cacheEntryName + ".meta"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channelMeta.lock();
            OutputStream osm = Channels.newOutputStream(channelMeta);
            Properties p = new Properties();
            p.setProperty("expiration-date", Long.toString(expirationDate));
            p.store(osm, null);
            osm.close();
            channelMeta.close();
        } catch (IOException e) {
            // we ignore the exception. if something went wrong we just don't cache it.
        } catch (AssertionError e) {
		    /* on Android 8.1
                java.lang.AssertionError:
                  at android.icu.impl.TimeZoneNamesImpl$ZNames.getNameTypeIndex (TimeZoneNamesImpl.java:724)
                  at android.icu.impl.TimeZoneNamesImpl$ZNames.getName (TimeZoneNamesImpl.java:790)
                  at android.icu.impl.TimeZoneNamesImpl.getTimeZoneDisplayName (TimeZoneNamesImpl.java:183)
                  at android.icu.text.TimeZoneNames.getDisplayName (TimeZoneNames.java:261)
                  at java.util.TimeZone.getDisplayName (TimeZone.java:405)
                  at java.util.Date.toString (Date.java:1066)
                  at java.util.Properties.store0 (Properties.java:828)
                  at java.util.Properties.store (Properties.java:817)
                  at de.umass.lastfm.cache.FileSystemCache.store (FileSystemCache.java:2)
		     */
        } finally {
            tryRelease(lock);
        }
    }

    private void createCache() {
        Path pt = Paths.get(cacheDir);
        try {
            if (Files.notExists(pt)) {
                Files.createDirectories(pt);
                if (!Files.isDirectory(pt)) {
                    this.cacheDir = pt.getParent().toAbsolutePath().toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cacheScrobble(Collection<SubmissionData> submissions) {
        cacheScrobble(submissions.toArray(new SubmissionData[0]));
    }

    public void cacheScrobble(SubmissionData... submissions) {
        createCache();
        FileLock lock = null;
        try {
            Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            lock = channel.lock();

            BufferedWriter w = new BufferedWriter(Channels.newWriter(channel, "UTF-8"));
            for (SubmissionData submission : submissions) {
                w.append(submission.toString());
                w.newLine();
            }
            w.close();
            channel.close();
        } catch (IOException e) {
            // huh ?
            //	e.printStackTrace();
        } finally {
            tryRelease(lock);
        }
    }

    public boolean isEmpty() {
        Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
        FileLock lock = null;

        if (!Files.exists(pt)) {
            return true;
        }
        try {
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.READ);
            lock = channel.lock(0L, Long.MAX_VALUE, true);
            BufferedReader r = new BufferedReader(Channels.newReader(channel, "UTF-8"));
            String line = r.readLine();
            r.close();
            return line == null || "".equals(line);
        } catch (IOException e) {
            // huh
            //	e.printStackTrace();
        } finally {
            tryRelease(lock);
        }
        return true;
    }

    public void scrobble(Scrobbler scrobbler) throws IOException {
        FileLock lock = null;
        Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
        try {
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.READ);
            if (Files.exists(pt)) {
                lock = channel.lock(0L, Long.MAX_VALUE, true);
                BufferedReader r = new BufferedReader(Channels.newReader(channel, "UTF-8"));
                List<SubmissionData> list = new ArrayList<SubmissionData>(50);
                String line;
                while ((line = r.readLine()) != null) {
                    SubmissionData d = new SubmissionData(line);
                    list.add(d);
                    if (list.size() == 50) {
                        scrobbler.submit(list);
                        list.clear();
                    }
                }
                if (list.size() > 0)
                    scrobbler.submit(list);
                r.close();
                channel.close();

                channel = FileChannel.open(pt, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                lock = channel.lock();
                channel.truncate(0);
                channel.close();
            }
        } catch (IOException e) {
            tryRelease(lock);
            throw e;
        }
    }

    public void clearScrobbleCache() {
        Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
        try {
            Files.deleteIfExists(pt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cacheScrobbles(Collection<ScrobbleData> scrobbles) {
        cacheScrobbles(scrobbles.toArray(new ScrobbleData[0]));
    }

    public void cacheScrobbles(ScrobbleData... scrobbles) {
        createCache();
        FileLock lock = null;
        try {
            Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            lock = channel.lock();
            BufferedWriter w = new BufferedWriter(Channels.newWriter(channel, "UTF-8"));
            for (ScrobbleData scrobble : scrobbles) {
                w.append(encodeScrobbleData(scrobble));
                w.newLine();
            }
            w.close();
            channel.close();
        } catch (IOException e) {
            tryRelease(lock);
            // huh ?
            //	e.printStackTrace();
        }
    }

    public List<ScrobbleResult> scrobble(Session session) throws IOException {
        FileLock lock = null;
        Path pt = Paths.get(cacheDir, SUBMISSIONS_FILE);
        List<ScrobbleResult> result = new ArrayList<ScrobbleResult>();

        try {
            FileChannel channel = FileChannel.open(pt, StandardOpenOption.READ);
            lock = channel.lock(0L, Long.MAX_VALUE, true);
            if (Files.exists(pt)) {
                BufferedReader r = new BufferedReader(Channels.newReader(channel, "UTF-8"));
                List<ScrobbleData> list = new ArrayList<ScrobbleData>(50);
                String line;
                while ((line = r.readLine()) != null) {
                    ScrobbleData d = decodeScrobbleData(line);
                    list.add(d);
                    if (list.size() == 50) {
                        result.addAll(Track.scrobble(list, session));
                        list.clear();
                    }
                }
                if (list.size() > 0)
                    result.addAll(Track.scrobble(list, session));
                r.close();
                channel.close();

                channel = FileChannel.open(pt, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                lock = channel.lock();
                channel.truncate(0);
                channel.close();
            }
        } catch (IOException e) {
            tryRelease(lock);
        }
        return result;
    }

    private static String encodeScrobbleData(ScrobbleData d) {
        String artist = StringUtilities.encode(d.getArtist());
        String track = StringUtilities.encode(d.getTrack());
        String album = StringUtilities.encode(d.getAlbum());
        String albumArtist = StringUtilities.encode(d.getAlbumArtist());
        return String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s;%b", artist, track, d.getTimestamp(), d.getDuration(), album, albumArtist, d.getMusicBrainzId(),
                d.getTrackNumber(), d.getStreamId(), d.isChosenByUser());
    }

    private static ScrobbleData decodeScrobbleData(String s) {
        String[] parts = s.split(";", 10);
        return new ScrobbleData(StringUtilities.decode(parts[0]), StringUtilities.decode(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                StringUtilities.decode(parts[4]), StringUtilities.decode(parts[5]), parts[6], Integer.parseInt(parts[7]), parts[8], Boolean.parseBoolean(parts[9]));
    }

    private static void tryRelease(FileLock lock) {
        if (lock != null) {
            try {
                lock.release();
            } catch (IOException e) {
                e.printStackTrace();
                // ignore
            }
        }
    }

    class InputStreamWithLock {
        private final InputStream in;
        private final FileLock lock;

        public InputStreamWithLock(InputStream in, FileLock lock) {
            this.in = in;
            this.lock = lock;
        }

        public InputStream getInputStream() {
            return in;
        }

        public void close() {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
            tryRelease(lock);
        }
    }
}
