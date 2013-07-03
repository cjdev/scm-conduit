package com.cj.scmconduit.server.data;

public interface KeyValueStore {

    public abstract String get(String key);

    public abstract void put(String key, String value);

}