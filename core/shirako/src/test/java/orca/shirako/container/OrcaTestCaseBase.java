package orca.shirako.container;

import junit.framework.TestCase;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class OrcaTestCaseBase extends TestCase {
    public static final String MySqlDatabaseName = "orca_test";
    public static final String MySqlDatabaseHost = "localhost";
    public static final String MySqlDatabaseUser = "orca_test";

    static {
        //System.out.println(System.getProperty("user.dir"));

        System.setProperty("ORCA_HOME", "../../core/shirako/orca/");
        OrcaTestSettings.TestMode = true;
        fixClassPath();
        Term.SetCycles = false;
        Globals.start();
    }

    public static void fixClassPath() {
        try {
            Globals.start(OrcaTestSettings.StartClean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Rule public TestName name = new TestName();
    
    @Before
    public void beforeTest() {
    	Globals.Log.info("************ Starting test: " + name.getMethodName() + " ******************");
    }
    
    @After
    public void afterTest() {
    	Globals.Log.info("************ Finished test: " + name.getMethodName() + " ******************");    
    }
    /**
     * Returns the container database.
     * @return
     * @throws Exception
     */
    public IOrcaContainerDatabase getContainerDatabase() throws Exception {
        return Globals.getContainer().getDatabase();
    }

    /**
     * Makes an actor clock.
     * @return
     */
    public ActorClock getActorClock() {
        return Globals.getContainer().getActorClock();
    }
}
