package orca.ndl.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orca.ndl.NdlCommons;
import orca.ndl.NdlException;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Class to build sequenced paths of NDL links and elements
 * Elements can be added randomly and in the end a head-to-tail
 * sequence can be extracted.
 * @author ibaldin
 *
 */
public class NdlPath extends NdlCommons {
	public static final boolean debug = false;
	
	Set<Resource> unattached = new HashSet<Resource>();
	
	// the expected ends of the path
	Set<Resource> ends = new HashSet<Resource>();
	
	// roots of multicast subtrees
	Set<Resource> roots = new HashSet<Resource>();
	
	/** individual path elements are links, crossconnects or other resources
	 * that 'hasInterface' on interfaces. The way to sequence them is to look
	 * at what interfaces they have. A path is a linked list of path elements
	 * @author ibaldin
	 *
	 */
	public static class PathElement {
		final Resource r; 
		Resource headInterface = null, tailInterface = null;
		
		PathElement(Resource i) {
			r = i;
		}
		
		PathElement(Resource i, Resource h, Resource t) {
			r = i; headInterface = h; tailInterface = t;
		}
		
		void setHead(Resource i) {
			headInterface = i;
		}
		
		void setTail(Resource i) {
			tailInterface = i;
		}
		
		Resource getHead() {
			return headInterface;
		}
		
		Resource getTail() {
			return tailInterface;
		}
		
		Resource getResource() {
			return r;
		}
		
		@Override
		public String toString() {
			//return "" + headInterface + " <-- " + r + " --> " + tailInterface;
			return r.toString();
		}
	}
	
	/**
	 * Is the path complete, or are there left over elements that didn't fit.
	 * Leftover elements may be a normal occurrence.
	 * @return
	 */
	public boolean pathComplete() {
		return unattached.size() == 0;
	}
	
	/** 
	 * Recursive version of tryPath - does a search of all possible paths with
	 * backtracking.
	 * @param end
	 * @param path
	 * @return
	 */
	private boolean tryPathRecursive(PathElement end, List<PathElement> path) {

		// find a list of resources that share the interface with current path 
		// and recursively invoke on them. Stop when reaching the end element
		// or when no more resources share an interface

		if (debug) {
			System.out.println("Looking for " + end.getResource());
			System.out.println("Candidate path is ");
			for(PathElement pe: path) {
				System.out.println("- " + pe.getResource());
			}
		}
		
		List<PathElement> candidates = new ArrayList<PathElement>(); 
		PathElement last = path.get(path.size() - 1);
		
		// termination condition - found the desired end
		for (Resource endInt: NdlCommons.getResourceInterfaces(end.getResource())) {
			// if interface leads to compute element or stitchport, skip it. this is a hack dealing with
			// interface naming in multipoint connections 10/11/16 /ib
			if (NdlCommons.attachedToCompute(endInt) || (NdlCommons.attachedToStitchPort(endInt))) {
				if (debug)
					System.out.println("  Interface " + endInt + " leads to compute element or stitchport, skipping");
				continue;
			}
			if (endInt.equals(last.getTail())) {
				end.setHead(endInt);
				path.add(end);
				return true;
			}
		}
			
		for (Resource u: unattached) {
			// exclude repeats
			boolean contFlag = false;
			for (PathElement pe: path) {
				if (u.equals(pe.getResource())) {
					contFlag = true;
					break;
				}
			}
			if (contFlag)
				continue;
			// all elements of unattached have only 2 interfaces
			List<Resource> unInt = NdlCommons.getResourceInterfaces(u);
			if (debug)
				System.out.println("    Inspecting " + u + " with interfaces\n      " + unInt.get(0) + "\n      " + unInt.get(1));
			if (unInt.get(0).equals(last.getTail())) {
				if (debug)
					System.out.println("    Attaching " + u + " via " + unInt.get(0));
				PathElement pe = new PathElement(u, unInt.get(0), unInt.get(1));
				candidates.add(pe);
			} else 
				if (unInt.get(1).equals(last.getTail())) {
					if (debug)
						System.out.println("    Attaching " + u + " via " + unInt.get(1));
					PathElement pe = new PathElement(u, unInt.get(1), unInt.get(0));
					candidates.add(pe);
				}
		}
		
		if (candidates.size() == 0) {
			if (debug)
				System.out.println("  No candidates");
			return false;
		}
		
		if (debug) {
			System.out.println("  Will try candidates");
			for(PathElement pe: candidates) {
				System.out.println("  - " + pe.getResource());
			}
		}
		
		for(PathElement pe: candidates) {
			List<PathElement> newPath = new ArrayList<PathElement>(path);
			newPath.add(pe);
			if (tryPathRecursive(end, newPath)) { 
				path.clear();
				for (PathElement ppe: newPath) 
					path.add(ppe);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Try to build path to the specified end
	 * @param end
	 * @param path
	 * @return
	 */
	private boolean tryPath(PathElement end, List<PathElement> path) {
		
		boolean attached = true;
		PathElement current = path.get(0);
		
		Set<Resource> unattachedTmp = new HashSet<Resource>(unattached);
		while(attached) {
			//System.out.println("  Trying " + current.getResource() + " via " +  current.getTail());
			attached = false;
			for (Resource u: unattachedTmp) {
				// all elements of unattached have only 2 interfaces
				List<Resource> unInt = NdlCommons.getResourceInterfaces(u);
				if (debug) {
					System.out.println("    Inspecting " + u + " with interfaces\n      " + unInt.get(0) + "\n      " + unInt.get(1));
					System.out.println("Inspecting " + u);
				}
				if (unInt.get(0).equals(current.getTail())) {
					if (debug)
						System.out.println("Attaching[0] " + u);
					attached = true;
					current = new PathElement(u, unInt.get(0), unInt.get(1));
					path.add(current);
					unattachedTmp.remove(u);
					if (debug)
						System.out.println("    Attaching " + u + " via " + unInt.get(0));
				} else 
					if (unInt.get(1).equals(current.getTail())) {
						if (debug)
							System.out.println("Attaching[1] " + u);
						attached = true;
						current = new PathElement(u, unInt.get(1), unInt.get(0));
						path.add(current);
						unattachedTmp.remove(u);
						if (debug)
							System.out.println("    Attaching " + u + " via " + unInt.get(1));
					}
				if (attached)
					break;
			}
		}

		boolean finished = false;
		for (Resource endInt: NdlCommons.getResourceInterfaces(end.getResource())) {
			if (endInt.equals(current.getTail())) {
				finished = true;
				end.setHead(endInt);
				path.add(end);
			}
		}
		
		return finished;
	}
	
	/**
	 * assemble path elements in proper sequence without gaps. 
	 * @param start
	 * @param end
	 * @return 
	 */
	
	private List<PathElement> assemblePath(Resource start, Resource end) throws NdlException {
		//System.out.println("Assembling path from " + start + " to " + end);
		
		// inspect the unattached pool, locate end elements, then
		// try to build a path from one to another avoiding dead ends.
		
		List<PathElement> path = new LinkedList<PathElement>();
		
		if (unattached.size() == 0)
			return null;

		Resource tmp = start;
		start = end;
		end = tmp;
		
		// try to grow the path from one end to the other
		List<Resource> headInterfaces = NdlCommons.getResourceInterfaces(start);
		
		// start the path
		PathElement pathHead = new PathElement(start);
		PathElement pathTail = new PathElement(end);
		path.add(pathHead);
		
		for(Resource i: headInterfaces) {
			if (debug)
				System.out.println("  Trying build a path from " + pathHead + " to " + pathTail + " using interface " + i);
			
			// change the outgoing interface and try again
			pathHead.setTail(i);
			if (tryPathRecursive(pathTail, path)) {
				// we're done
				break;
			}
			path.clear();
			path.add(pathHead);
		}
		if (debug) {
			System.out.print("Assembled a path: ");
			for(PathElement pe: path) {
				System.out.print(pe + " ");
			}
			System.out.println();
		}
		return path;
	}
	
	/**
	 * Convert path from PathElements to resources
	 * @param path
	 * @return
	 * @throws NdlException
	 */
	private List<Resource> convertPath(List<PathElement> path) throws NdlException {
		// walk the linked list to assemble the array
		List<Resource> arrayPath = new ArrayList<Resource>(path.size());

		int index = 0;
		while(index < path.size() - 1) {
			PathElement elem = path.get(index);
			PathElement nextElem = path.get(index + 1);
			arrayPath.add(index++, elem.getResource());
			if (!elem.getTail().equals(nextElem.getHead()))
				throw new NdlException("Path not continuous between " + elem + " and " + nextElem);
		}

		arrayPath.add(index, path.get(index).getResource());
		
		return arrayPath;
	}
	
	/**
	 * Get a list of paths in this graph. Simple for point-to-point connections.
	 * For multipoint it is a list of paths from ends to multicast roots followed
	 * by the list of paths between roots. This method is destructive! Calling
	 * it second time will not provide correct results.
	 * @return
	 * @throws NdlException
	 */
	public List<List<Resource>> getPaths() throws NdlException {
		//if (unattached.size() > 0)
		//	throw new NdlException("Path invalid - unattached elements remain " + unattached);
		if (ends.size() == 0)
			return null;
		
		List<List<Resource>> ret = new ArrayList<List<Resource>>();
		
		// if this is a point-to-point path try to put it together
		if (roots.size() == 0) {
			if (ends.size() != 2) {
				StringBuilder sb = new StringBuilder();
				for(Resource e: ends) {
					sb.append(e.toString());
					sb.append(" ");
				}
				sb.append(" : ");
				for(Resource u: unattached) {
					sb.append(u);
					sb.append(" ");
				}
				throw new NdlException("Path has " + ends.size() + " (odd number) of endpoints: [" + sb.toString() + "] - expected 2");
			}
			
			Iterator<Resource> riter = ends.iterator();
			Resource pathStart = riter.next();
			Resource pathEnd = riter.next();
			List<PathElement> path = assemblePath(pathStart, pathEnd);

			if ((path == null ) || (path.size() == 0)) 
				throw new NdlException("Unable to find path between " + pathStart + " and " + pathEnd);

			ret.add(convertPath(path));
		} else {
			// Trace a part from each root to each end node if possible 
			// if there is a gap, it means it isn't reachable
			// Ugly double loop
			for (Resource root: roots) {
				Set<Resource> endsToRemove = new HashSet<Resource>();
				for (Resource end: ends) {
					if (debug)
						System.out.println("Assembling path between root " + root + " and " + end);
					List<PathElement> partPath = assemblePath(root, end);

					// this is possible and OK
					if ((partPath == null ) || (partPath.size() == 0))
						continue;
					
					endsToRemove.add(end);
					ret.add(convertPath(partPath));
				}
				ends.removeAll(endsToRemove);
			}
			
			// find all paths between roots
			if (roots.size() > 1) {
				Set<Resource> tmpRoots = new HashSet<Resource>(roots);
				for(Resource root: roots) {
					Set<Resource> rootsToRemove = new HashSet<Resource>();
					for(Resource otherRoot: tmpRoots) {
						List<PathElement> partPath = assemblePath(root, otherRoot);

						if ((partPath == null ) || (partPath.size() == 0))
							throw new NdlException("Unable to find path between multicast roots " + root + " and " + otherRoot);
						
						rootsToRemove.add(otherRoot);
						ret.add(convertPath(partPath));
					}
					tmpRoots.removeAll(rootsToRemove);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Force this element to be an end element (otherwise it is
	 * inferred by the quantity of the interfaces)
	 * @param r
	 * @throws NdlException
	 */
	public void addEndElement(Resource r) throws NdlException {
		if (debug)
			System.out.println("adding end node " + r);
		ends.add(r);
	}
	
	/**
	 * Add new element into path
	 * @param r
	 */
	public void addElement(Resource r) throws NdlException {
		List<Resource> interfaces = getResourceInterfaces(r);
		
		// link connections that link to request items will have
		// 3 interfaces (2 to neighbors, one to VM) so they will
		// be end points. Stitch nodes are accounted for separately
		// using the addEndElement() method above
		if (interfaces.size() == 2) {
			if (debug)
				System.out.println("adding unattached node " + r);
			unattached.add(r);
		} else {
			if (debug)
				System.out.println("adding end node with " + interfaces.size() + " interfaces " + r);
			ends.add(r);
		}
	}
	
	/**
	 * Add a multicast root node
	 * @param r
	 * @throws NdlException
	 */
	public void addRoot(Resource r) throws NdlException {
		if (debug)
			System.out.println("adding root node " + r);
		roots.add(r);
	}
	
	/**
	 * Return known roots (if any - null if none)
	 * @return
	 * @throws NdlException
	 */
	public List<Resource> getRoots() throws NdlException {
		if (roots.size() == 0)
			return null;
		return new LinkedList<Resource>(roots);
	}
	
	// 04/11/12 - BECAUSE THE CURRENT MANIFEST FOR NETWORK CONNECTION HAS
	// ELEMENTS OF THE REQUEST, THE END ELEMENTS OF THE PATH TYPICALLY HAVE 3 INTERFACES
	// SO IT IS DIFFICULT TO BUILT THE PATH ON THE FLY (DUE TO POSSIBLE DEAD
	// ENDS). SO NOW WE THROW EVERYTHING INTO UNATTACHED POOL AND TRY TO MAKE
	// SENSE OF IT WHEN THE USER ASKS FOR A COMPLETE PATH /ib
	
//	public void addElement(Resource r) throws NdlException {
//		// add new element into the path in its proper place
//		
//		if (!addSingleElement(r)) {
//			unattached.add(r);
//		}
//		
//		// now check all unattached
//		boolean inserted = true;
//		while(inserted) {
//			inserted = false;
//			Resource insRes = null;
//			for(Resource res: unattached) {
//				if (addSingleElement(res)) {
//					inserted = true;
//					insRes = res;
//					break;
//				}
//			}
//			if (inserted)
//				unattached.remove(insRes);
//		}
//	}
//	
//	private boolean addSingleElement(Resource r) throws NdlException {
//		// valid element has two interfaces. End elements can
//		// have 1 or more than two. There can only
//		// be two end elements on the path
//		
//		// get the interfaces of this resource 
//		List<Resource> interfaces = getResourceInterfaces(r);
//		
//		if (interfaces.size() == 0)
//			throw new NdlException("Resource " + r + " has no interfaces");
//		
//		PathElement elem = new PathElement(r);
//		// first element
//		if (path.size() == 0) {
//			elem.setHead(interfaces.get(0));
//			if (interfaces.size() == 2)
//				elem.setTail(interfaces.get(1));
//			path.add(elem);
//			return true;
//		}
//		
//		boolean dual = false;
//		if (interfaces.size() == 2)
//			dual = true;
//		
//		// otherwise try to attach to existing path or throw into unattached bucket
//		// for later inspection
//		PathElement pathHead = path.getFirst();
//		PathElement pathTail = path.getLast();
//		boolean inserted = false;
//		int index = 0;
//		for(; index < interfaces.size(); index++) {
//			Resource intR = interfaces.get(index);
//			if ((pathHead.getHead() != null) &&
//					(pathHead.getHead().equals(intR))) {
//				elem.setTail(intR);
//				if (dual)
//					elem.setHead(interfaces.get((index + 1) % 2));
//				path.addFirst(elem);
//				inserted = true;
//				break;
//			}
//			if ((pathTail.getTail() != null) &&
//					(pathTail.getTail().equals(intR))) {
//				elem.setHead(intR);
//				if (dual)
//					elem.setTail(interfaces.get((index + 1) %2));
//				path.addLast(elem);
//				inserted = true;
//				break;
//			}
//		}
//		
//		return inserted;
//	}
}
