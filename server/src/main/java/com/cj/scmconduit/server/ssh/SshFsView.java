package com.cj.scmconduit.server.ssh;

import java.io.File;

import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.SshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshFsView implements FileSystemView {

    private final Logger LOG = LoggerFactory.getLogger(SshFsView.class);


    // the first and the last character will always be '/'
    // It is always with respect to the root directory.
    private String currDir;

    private String userName;

    private boolean caseInsensitive = false;

    private final File root;
    
    protected SshFsView(String userName, File root) {
        this(userName, false, root);
    }

    public SshFsView(String userName, boolean caseInsensitive, File root) {
    	this.root = root;
    	
        if (root == null) throw new IllegalArgumentException("root not be null");
        if(!root.isAbsolute()) throw new IllegalArgumentException("not an absolute path: " + root.getAbsolutePath());
        if (!root.exists()) throw new IllegalArgumentException("no such directory: " + root.getAbsolutePath());
        if (!root.isDirectory()) throw new IllegalArgumentException("not a directory: " + root.getAbsolutePath());

        if (userName == null) {
            throw new IllegalArgumentException("user can not be null");
        }

        this.caseInsensitive = caseInsensitive;

        currDir = root.getAbsolutePath();
        this.userName = userName;

        // add last '/' if necessary
        LOG.debug("Native filesystem view created for user \"{}\" with root \"{}\"", userName, currDir);
    }

    
    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        return getFile(file);
    }
    @Override
    public SshFile getFile(String file) {
        
        // get actual file object
        String physicalName = NativeSshFile.getPhysicalName("/",
                currDir, file, caseInsensitive);
        File fileObj = new File(root, physicalName);

        // strip the root directory and return
        String userFileName = physicalName.substring("/".length() - 1);
        return new NativeSshFile(userFileName, fileObj, userName);
    }
}
