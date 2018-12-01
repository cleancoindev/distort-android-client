package com.unix4all.rypi.distort;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpfsHash {

    private static final Pattern IS_IPFS_HASH_PATTERN = Pattern.compile("[a-km-zA-HJ-NP-Z1-9]+");

    static boolean isIpfsHash(String hash) {
        return IS_IPFS_HASH_PATTERN.matcher(hash).matches();
    }
}
