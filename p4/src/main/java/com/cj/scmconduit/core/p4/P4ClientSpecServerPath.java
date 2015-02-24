package com.cj.scmconduit.core.p4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P4ClientSpecServerPath {

    public static P4ClientSpecServerPath parse(String line){
        System.out.println(line);
       
            final Matcher m = Pattern.compile("-?//([a-zA-Z0-9]*)(.*)").matcher(line);
            if(m.matches()){
                try {
                    final String depotName = m.group(1);
                    final String path = m.group(2);
                    return new P4ClientSpecServerPath(depotName, path);
                } catch (Exception e) {
                    throw new RuntimeException("Error parsing '" + line + "'", e);
                }
            }else{
                throw new RuntimeException("This isn't a valid server path line: '" + line + "'");
            }
    }

    public final String depotName;
    public final String path;
    
    public P4ClientSpecServerPath(String depotName, String path) {
        super();
        this.depotName = depotName;
        this.path = path;
    } 


}
