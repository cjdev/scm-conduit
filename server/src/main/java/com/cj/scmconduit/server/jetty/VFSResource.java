package com.cj.scmconduit.server.jetty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.mortbay.resource.FileResource;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceCollection;

@SuppressWarnings("serial")
public final class VFSResource extends ResourceCollection {
	private final List<Resource> set = new ArrayList<Resource>();
	
	private final String name;
	
	public VFSResource(String name) {
		super();
		this.name = name;
	}
	
	public void addVResource(final String vfsPath, final File localPath){
		set.add(vfsResource(vfsPath, localPath));
	}
	
	@Override
	public Resource addPath(String path) throws IOException, MalformedURLException {
		for(Resource r : set){
			String next = r.getName();
			if(path.startsWith(next)){
				String subPath = path.substring(next.length());
				return r.addPath(subPath);
			}
		}
		return null;
	}
	
	public void printContents(String padding){
		System.out.println(padding + getName());
		for(Resource next: set){
			if(next instanceof VFSResource){
				VFSResource s = (VFSResource) next;
				s.printContents(padding + "    ");
			}
		}
	}
	
	public void addChild(Resource r){
		System.out.println("Added " + r + " as " + r.getName() + " on " + getName());
		set.add(r);
	}
	
	@Override
	public boolean delete() throws SecurityException {
		throw new SecurityException();
	}
	
	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public File getFile() throws IOException {
		return null;
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException, SecurityException {
		throw new SecurityException();
	}
	
	@Override
	public URL getURL() {
		return null;
	}
	
	@Override
	public boolean isDirectory() {
		return true;
	}
	
	@Override
	public long lastModified() {
		return 0;
	}
	
	@Override
	public long length() {
		return 0;
	}
	@Override
	public String[] list() {
		List<String> names = new ArrayList<String>(set.size());
		for(Resource r : set){
			names.add(r.getName());
		}
		return names.toArray(new String[set.size()]);
	}
	
	@Override
	public void release() {
	}
	
	@Override
	public boolean renameTo(Resource dest) throws SecurityException {
		throw new SecurityException();
	}

	private static Resource vfsResource(final String vfsPath, final File localPath){
		try {
			return new FileResource(localPath.toURI().toURL()){
				@Override
				public String getName() {
					return vfsPath;
				}
			};
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}