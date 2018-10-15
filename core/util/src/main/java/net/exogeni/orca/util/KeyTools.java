/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;


public class KeyTools
{
    public static final String Separator = ",";

    public static PublicKey decodePublicKey(String data) throws Exception
    {
        int i = data.indexOf(Separator);

        if ((i < 0) || (i == (data.length() - 1))) {
            throw new Exception("Invalid key string");
        }

        String algorithm = data.substring(0, i);
        String rest = data.substring(i + 1);

        byte[] encodedKey = Base64.decode(rest);

        // this is an X.509 encoded key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);

        // obtain the key factory and decode the key
        KeyFactory kf = KeyFactory.getInstance(algorithm.toUpperCase());

        return kf.generatePublic(spec);
    }

    public static String encode(PublicKey key)
    {
        String a = key.getAlgorithm();
        String k = Base64.encodeBytes(key.getEncoded());

        return a + Separator + k;
    }
}
