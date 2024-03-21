/*
 * (C) Copyright Selerity, Inc. 2009-2019. All rights reserved. This source code
 * is confidential and proprietary information of Selerity Inc. and may be used
 * only by a recipient designated by and for the purposes permitted by Selerity
 * Inc. in writing. Reproduction of, dissemination of, modifications to or
 * creation of derivative works from this source code, whether in source or
 * binary forms, by any means and in any form or manner, is expressly
 * prohibited, except with the prior written permission of Selerity Inc.. THIS
 * CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may
 * not be removed from the software by any user thereof.
 */
package com.selerity.sync.client.examples.refdata;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractNarwhalClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.NarwhalMetaServiceImpl;
import com.selerity.sync.client.NarwhalService;
import com.selerity.sync.client.PaginatedResponseIterator;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.Session;

/**
 * Loads TagCache instances either on demand or with periodic refresh.
 */
public class TagCacheFactory extends AbstractNarwhalClient {

    private static final Log log = LogFactory.getLog(TagCacheFactory.class);

    // this is a reasonable size for each page of results when querying lightweight objects like tags.
    protected static final int PAGINATION_LIMIT = 1000;

    protected TagCache currentInstance = null;

    /**
     * Initializes a new instance of the TagCacheFactory based on a NarwhalService
     * that supports the <code>TagHandler.getTagNames<code>
     * and <code>SynonymHandler.getTagsByName</code> methods.
     *
     * @param service
     * @param user
     * @param password
     * @param clientAppName
     * @throws Exception
     */
    public TagCacheFactory(final NarwhalService service, final String user, final String password,
                           final String clientAppName) throws Exception {
        super(service, user, password, clientAppName);
    }

    /**
     * Gets the current instance of the TagCache.
     * If no cache has been loaded then the cache will be loaded first.
     *
     * @throws DispatchException
     */
    public synchronized TagCache getInstance() throws DispatchException {
        if (currentInstance == null) {
            reload();
        }
        return currentInstance;
    }

    /**
     * Forces the cache to be reloaded.
     *
     * @throws DispatchException
     */
    public void reload() throws DispatchException {
        log.debug("reloading tags...");
        long startMillis = System.currentTimeMillis();
        int tagCount = 0;
        int synonymCount = 0;
        Session session = this.startSession();

        TagCacheImpl newTagCache = new TagCacheImpl();

        // first, look up tag names, then for each tag name look up the values.
        Request tagNameRequest = new Request("TagHandler.getTagNames");
        JsonElement tagNameResultElem = dispatch(tagNameRequest, session);
        if ((tagNameResultElem == null) || (tagNameResultElem.isJsonNull())) {
            throw new DispatchException(DispatchException.PARSE_ERROR, "Expected a non-null result for " + tagNameRequest.getMethod());
        }
        JsonArray tagNameArray = tagNameResultElem.getAsJsonArray();
        for (JsonElement tagNameElem : tagNameArray) {
            String tagName = tagNameElem.getAsString();
            log.debug("looking for tags with name = " + tagName);

            // now look up all the tags and synonyms with the given name
            Request tagsRequest = new Request("SynonymHandler.getTagsByName");
            tagsRequest.setMethodParameter("name", tagName);
            PaginatedResponseIterator tagResponseIt = paginatedDispatch(tagsRequest, session, "paginationOption", PAGINATION_LIMIT);
            while (tagResponseIt.hasNextResult()) {
                JsonElement tagElem = tagResponseIt.nextResult();
                if ((tagElem == null) || (tagElem.isJsonNull())) {
                    throw new NullPointerException("cannot parse a tag from a null element");
                }
                JsonObject tagObj = tagElem.getAsJsonObject();
                String tagId = MiscUtils.getString(tagObj, "tagId", null);

                Tag tag = newTagCache.getTagByTagId(tagId);
                if (tag == null) {
                    // this is a new tag, need to create it
                    String nameId = MiscUtils.getString(tagObj, "nameId", null);
                    String name = MiscUtils.getString(tagObj, "name", null);
                    String valueId = MiscUtils.getString(tagObj, "valueId", null);
                    String value = MiscUtils.getString(tagObj, "value", null);

                    tag = new Tag(tagId, nameId, name, valueId, value);
                    newTagCache.addTag(tag);
                    tagCount++;
                }

                // now add in the synonym
                String synonym = MiscUtils.getString(tagObj, "synonym", null);
                if (synonym != null) {
                    String family = MiscUtils.getString(tagObj, "family", null);  // note, family may be legitimately null
                    tag.addSynonym(family, synonym);
                    synonymCount++;
                }
            }
        }

        // clean up this session
        this.closeSession(session);

        // now update the cache
        synchronized (this) {
            currentInstance = newTagCache;
        }

        // all done, just print out some pretty stats
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        double elapsedSeconds = ((double) elapsedMillis) / 1000.0;
        log.debug("loaded " + tagCount + " tags and " + synonymCount + " synonyms in " + elapsedSeconds + " seconds");
    }

    /**
     * Starts a thread to reload the cache periodically.
     *
     * @param refreshInterval
     * @param refreshIntervalTimeUnit
     */
    public void startRefresh(final long refreshInterval, final TimeUnit refreshIntervalTimeUnit) {

        final long refreshIntervalMillis = TimeUnit.MILLISECONDS.convert(refreshInterval, refreshIntervalTimeUnit);

        final Thread refreshThread = new Thread("tag_refresh") {
            @Override
            public void run() {
                while (true) {
                    try {
                        log.debug("refresh thread sleeping for " + refreshIntervalMillis + " ms...");
                        Thread.sleep(refreshIntervalMillis);
                        log.debug("refresh thread reloading...");
                        reload();
                    } catch (Exception ex) {
                        log.error("caught " + ex + " during tag refresh, skipping", ex);
                    }
                }
            }
        };
        refreshThread.setDaemon(true);
        refreshThread.start();

        log.debug("started refresh thread");
    }


    /**
     * Purely for integration testing purposes.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("arguments: urls user password");
                System.exit(1);
            }

            // read in the arguments
            String urls = args[0];
            String user = args[1];
            String password = args[2];

            NarwhalService service = new NarwhalMetaServiceImpl(urls);
            TagCacheFactory factory = new TagCacheFactory(service, user, password, "TagCacheFactoryTest");

            while (true) {
                long startNanos = System.nanoTime();
                TagCache tagCache = factory.getInstance();
                Tag tag = tagCache.getTagByTagId("880886c9-e851-11e0-9138-001517bbdc12");
                Tag tag2 = tagCache.getTagByTagId("0c33f34e-cb30-4f13-aa3b-18a098e7b6ae");
                long elapsedNanos = System.nanoTime() - startNanos;
                log.debug("elapsed lookup time is: " + (elapsedNanos / 1000) + " microseconds");
                if (tag != null) {
                    log.debug("got tag " + tag.getName() + " = " + tag.getValue());
                    Set<String> synonyms = tag.getSynonyms(null);
                    for (String synonym : synonyms) {
                        log.debug("tag also has synonym: " + synonym);
                    }
                }

                log.debug("tag2 = " + tag2.getValue());

                Thread.sleep(10000);
            }

        } catch (Exception ex) {
            log.error("caught exception " + ex + " in main loop, exiting", ex);
        }
    }

}
