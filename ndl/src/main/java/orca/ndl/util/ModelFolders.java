package orca.ndl.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDB;

/**
 * Singleton that knows all about model folder storage
 * @author ibaldin
 *
 */
public class ModelFolders {
	// store mapping from model to its TDB folder
	protected Map<OntModel, ModelFolder> modelFolders = new HashMap<OntModel, ModelFolder>();
	protected Set<Dataset> datasets = new HashSet<Dataset>();

	protected boolean noMoreFolders = false;
	
	private static class ModelFolder {
		private boolean ephemeral;
		private String folderPath;
		private Dataset dataset;
		
		public ModelFolder(String p, Dataset ds, boolean e) {
			ephemeral = e;
			folderPath = p;
			dataset = ds;
		}
		
		public boolean isEphemeral() {
			return ephemeral;
		}
		
		public String getFolderPath() {
			return folderPath;
		}
		
		public Dataset getDataset() {
			return dataset;
		}
		
		public void closeDataset() {
			dataset.end();
		}
	}
	
	private ModelFolders() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Closing datasets");
				ModelFolders.getInstance().closeDatasets();
				System.out.println("Deleting ephemeral TDB folders");
				ModelFolders.getInstance().deleteTDBOnShutDown();
			}
		});
	}
	
	private static ModelFolders instance = new ModelFolders();
	
	public static ModelFolders getInstance() {
		return instance;
	}

	/**
	 * Invoked on shut down, deletes all ephemeral TDB folders
	 */
	public synchronized void deleteTDBOnShutDown() {
		ModelFolders.getInstance().noMoreFolders = true;
		for(Map.Entry<OntModel, ModelFolder> entry: modelFolders.entrySet()) {
			if (entry.getValue().isEphemeral()) {
				deleteFolder(entry.getValue().getFolderPath());
			}
		}
	}
	
	/** 
	 * Create a temporary directory, that if empty will be deleted on exit
	 * Thank you StackOverflow 
	 * http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
	 * @return
	 * @throws IOException
	 */
	private File _createTempDirectory() throws IOException
	{
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if(!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}
	
	/**
	 * Create a temporary directory with no exit hooks
	 * @param prefix - optional prefix
	 * @return
	 */
	public synchronized File createTempDirectory(String prefix) {
		File tmpDir = null;
		if (noMoreFolders)
			return null;
		// create an ephemeral directory that goes away after JVM exits
		String tmpDirName = (prefix == null ? System.getProperty("java.io.tmpdir") : prefix) + 
				System.getProperty("file.separator") + "jenatdb" + UUID.randomUUID();
		tmpDir = new File(tmpDirName);
		if (!tmpDir.mkdir())
			return null;
		return tmpDir;
	} 

	/**
	 * Return a named directory (create if needed) with no exit hooks
	 * @param p
	 * @return
	 */
	public synchronized File createNamedDirectory(String p) {
		if (noMoreFolders)
			return null;
		
		// create a named directory that is not meant to be  removed after JVM exits
		File dir = new File(p);
		if (!dir.exists())
			if (!dir.mkdirs())
				return null;
		return dir;
	}
	
	
	/**
	 * Recursively delete folder/directory. Thank you StackOverflow 
	 * http://stackoverflow.com/questions/7768071/java-delete-a-folder-content
	 * @param folder
	 */
	private static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}
	
	/**
	 * Remove a folder recursively
	 * @param p
	 */
	public static void deleteFolder(String p) {
		deleteFolder(new File(p));
	}
	
	/**
	 * Check if a model folder exists
	 * @param folder
	 * @return
	 */
	public static boolean folderExists(File folder) {
		if (folder == null)
			return false;
		return folder.exists();
	}
	
	/**
	 * Check if a model folder exists
	 * @param p
	 * @return
	 */
	public static boolean folderExists(String p) {
		if (p == null)
			return false;
		return new File(p).exists();
	}
	
	/**
	 * Add a new TDB model entry for bookkeeping
	 * @param m - the model
	 * @param ds - its dataset
	 * @param path - the path where it will be kept
	 * @param e - whether it is ephemeral or permanent
	 */
	public synchronized void put(OntModel m, Dataset ds, String path, boolean e) throws Exception {
		assert((path != null) && (m != null) && (ds != null));
		datasets.add(ds);
		if (modelFolders.containsKey(m))
			throw new Exception("ModelFolders already contain a mapping for model " + m);
		modelFolders.put(m, new ModelFolder(path, ds, e));
	}
	
	/**
	 * Remove a folder belonging to the model (hopefully after it is closed).
	 * The method returns false if the model is still open or if the model
	 * didn't have a folder.
	 * @param m
	 */
	public synchronized boolean remove(OntModel m) {
		assert(m != null);
		if (!m.isClosed())
			return false;

		if (modelFolders.containsKey(m)) {
			// close the dataset
			ModelFolder mf = modelFolders.get(m);
			// make sure a dataset isn't closed twice. this is a bit
			// dangerous, because some non-closed models sharing the dataset
			// may end up with a closed dataset. /ib 09/16/2013
			if ((mf.getDataset() != null) && (datasets.contains(mf.getDataset()))) {
				mf.getDataset().close();
				datasets.remove(mf.getDataset());
			}
			deleteFolder(mf.getFolderPath());
			modelFolders.remove(m);
			return true;
		}
		return false;
	}
	
	public synchronized String getPath(OntModel m) {
		assert(m != null);
		if (modelFolders.containsKey(m)) {
			ModelFolder mf = modelFolders.get(m);
			return mf.getFolderPath();
		}
		return null;
	}
	
	public synchronized boolean isEphemeral(OntModel m) {
		assert(m != null);
		if (modelFolders.containsKey(m)) {
			ModelFolder mf = modelFolders.get(m);
			return mf.isEphemeral();
		}
		return true;
	}
	
	public synchronized boolean isInTDB(OntModel m) {
		assert(m != null);
		if (modelFolders.containsKey(m))
			return true;
		return false;
	}
	
	/**
	 * Close all datasets (e.g. on shutdown)
	 */
	public void closeDatasets() {
		for(Map.Entry<OntModel, ModelFolder> entry: modelFolders.entrySet()) {
			//entry.getValue().closeDataset();
			if (!entry.getValue().isEphemeral()) {
				System.out.println("Syncing dataset " + entry.getValue().getFolderPath());
				try {
					TDB.sync(entry.getValue().getDataset());
				} catch (Exception e) {
					System.out.println("Failed to sync " + entry.getValue().getFolderPath() + ", trying again in .5s ");
					try {
						Thread.sleep(500);
						TDB.sync(entry.getValue().getDataset());
					} catch (Exception ee) {
						System.out.println("Failed again to sync " + entry.getValue().getFolderPath() + ", continuing");
					}
				}
			}
		}
	}
}
