package orca.handlers.xcat.tasks;

/**
 * Intermediate class holds the modify index
 * @author ibaldin
 *
 */
public class XCatGenerateBashFileModTask extends XCatGenerateBashFileBaseTask {
	protected int modifyIndex;
	
	public void setModifyIndex(String mi) {
		try {
			modifyIndex = Integer.parseInt(mi);
		} catch(NumberFormatException nfe) {
			System.out.println("Unable to parse modify index " + mi + " in " + this.getClass().getCanonicalName());
			modifyIndex = -1;
		}
	}
	
	/**
	 * Returns modify.<modindex> prefix of property names
	 * @return
	 */
	protected String modifyPrefix() {
		return "modify." + modifyIndex;
	}
}
