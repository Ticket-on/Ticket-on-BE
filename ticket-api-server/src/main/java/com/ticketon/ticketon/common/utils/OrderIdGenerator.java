package com.ticketon.ticketon.common.utils;

import de.huxhorn.sulky.ulid.ULID;

public class OrderIdGenerator {

    private static final ULID ulid = new ULID();

    public static String nextId() {
        return ulid.nextULID();
    }
}
