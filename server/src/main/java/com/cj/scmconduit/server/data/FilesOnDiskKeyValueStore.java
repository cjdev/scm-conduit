package com.cj.scmconduit.server.data;

import java.io.File;

import org.apache.commons.io.FileUtils;


public class FilesOnDiskKeyValueStore implements KeyValueStore {
    private final File root;
    
    public FilesOnDiskKeyValueStore(File root) {
        super();
        this.root = root;
    }
    
    private File fileForKey(String key){
        if(key.contains("/") || key.contains("..")){
            throw new RuntimeException("bad key ... you tryin' to hack me?");
        }
        return new File(root, key);
    }
    
    @Override
    public String get(String key){
        try {
            final File file = fileForKey(key);
            if(file.exists()){
                return FileUtils.readFileToString(file);
            }else{
                System.out.println("No such file: " + file.getAbsolutePath());
                return null;
            }
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
    @Override
    public void put(String key, String value){
        try {
            FileUtils.writeStringToFile(fileForKey(key), value);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
    
}
