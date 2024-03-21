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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a simple name-value pair plus additional (optionally categorized) synonyms.
 */
public class Tag {
    protected static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<String>(0));

    protected final String tagId;
    protected final String nameId;
    protected final String name;
    protected final String valueId;
    protected final String value;
    protected final Map<String, Set<String>> synonymsByFamily = new HashMap<>();
    protected final Set<String> synonymsWithoutFamily = new HashSet<>();

    public Tag(String tagId, String nameId, String name, String valueId, String value) {
        this.tagId = tagId;
        this.nameId = nameId;
        this.name = name;
        this.valueId = valueId;
        this.value = value;
    }

    public String getTagId() {
        return tagId;
    }

    public String getNameId() {
        return nameId;
    }

    public String getName() {
        return name;
    }

    public String getValueId() {
        return valueId;
    }

    public String getValue() {
        return value;
    }

    public void addSynonym(String family, String synonym) {
        if (family != null) {
            Set<String> synonyms = synonymsByFamily.get(family);
            if (synonyms == null) {
                synonyms = new HashSet<>();
                synonymsByFamily.put(family, synonyms);
            }
            synonyms.add(synonym);
        } else {
            synonymsWithoutFamily.add(synonym);
        }
    }

    public Set<String> getSynonyms(String family) {
        if (family == null) {
            return Collections.unmodifiableSet(synonymsWithoutFamily);
        }
        Set<String> synonyms = synonymsByFamily.get(family);
        if (synonyms == null) {
            return EMPTY_SET;
        }
        return Collections.unmodifiableSet(synonyms);
    }

    public String toString() {
        return name + "=" + value;
    }

}
