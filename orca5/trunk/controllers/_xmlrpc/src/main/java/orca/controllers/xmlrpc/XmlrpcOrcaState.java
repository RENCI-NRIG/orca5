package orca.controllers.xmlrpc;


import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import orca.network.InterCloudHandler;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;

public final class XmlrpcOrcaState implements Serializable{
		
	private	InterCloudHandler handler;
	private Map<String, InterCloudHandler> urnToHandler = new HashMap<String, InterCloudHandler>();
        private IServiceManager sm;
        private XmlrpcController controller;
        private ISlice slice;
        private Map<String, SliceID> urnToSlice = new HashMap<String, SliceID>();
        // use output compression
    	private static boolean compressOutput = true;

        private static final XmlrpcOrcaState fINSTANCE =  new XmlrpcOrcaState();

        private XmlrpcOrcaState(){
            // Can't call this constructor
        }

        public static XmlrpcOrcaState getInstance() {
            return fINSTANCE;
        }

        public synchronized InterCloudHandler getHandler() {
			return handler;
		}

		public synchronized void setHandler(InterCloudHandler handler) {
			this.handler = handler;
		}

		public synchronized IServiceManager getSM(){
            return sm;
        }

        public synchronized void setSM(IServiceManager sm){
            this.sm = sm;
        }

        public synchronized XmlrpcController getController(){
            return controller;
        }

        public synchronized void setController(XmlrpcController controller){
            this.controller = controller;
        }

        public synchronized ISlice getSlice(){
            return slice;
        }

        public synchronized void setSlice(ISlice slice){
            this.slice = slice;
        }

        // Manage URN to ORCA slice ID mappings
        public void mapUrn(String urn, SliceID slice) {
        	synchronized(urnToSlice) {
        		urnToSlice.put(urn, slice);
        	}
        }
        
        public SliceID getSliceID(String urn) {
        	synchronized(urnToSlice) {
        		return urnToSlice.get(urn);
        	}
        }
        
        public void unMapUrn(String urn) {
        	synchronized(urnToSlice) {
        		urnToSlice.remove(urn);
        	}
        }
        // Manage URN to ORCA slice ID mappings
        public void mapHandler(String urn, InterCloudHandler aHandler) {
                synchronized(urnToHandler) {
                        urnToHandler.put(urn, aHandler);
                }
        }

        public InterCloudHandler getHandler(String urn) {
                synchronized(urnToHandler) {
                        return urnToHandler.get(urn);
                }
        }

        public void unMapHandler(String urn) {
                synchronized(urnToHandler) {
                        urnToHandler.remove(urn);
                }
        }
        
        // manage state of compression of output 
        public boolean getCompression() {
        	return compressOutput;
        }
        
    	public synchronized void setCompression(boolean f) {
    		compressOutput = f;
    	}
        
        /**
        * If the singleton implements Serializable, then this
        * method must be supplied.
        */
        private Object readResolve() throws ObjectStreamException {
            return fINSTANCE;
        }

}
