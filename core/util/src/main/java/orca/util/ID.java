/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;

import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.UUID;

/**
 * This class represents an identifier. It can be used to create a globally
 * unique identifier or to wrap an existing string to be used as an identifier.
 */
public class ID implements Cloneable, Comparable<ID> {
    /**
     * The underlying string representing this identifier.
     */
    protected final String id;

    /**
     * Creates a new globally unique identifier.
     */
    public ID() {
        UUID uuid = UUID.randomUUID();
        id = uuid.toString();

    }

    /**
     * Loads the specified string as an identifier.
     * @param id identifier
     */
    public ID(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        this.id = id;
    }

    @Override
    public Object clone() {
        return new ID(new String(id));
    }

    public int compareTo(ID id) {
        return this.id.compareTo(id.id);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ID)) {
            return false;
        }

        ID otherID = (ID) other;

        if (otherID == null) {
            return false;
        }

        return id.equals(otherID.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    public String toHashString() {
        return FNVHash.hash(id);
    }

    public String toSha1HashString() throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] id_bytes = sha1.digest(id.getBytes());

        Formatter fmt = new Formatter(new StringWriter());
        for (byte b : id_bytes)
            fmt.format("%02x", b);

        return fmt.out().toString();
    }
}
