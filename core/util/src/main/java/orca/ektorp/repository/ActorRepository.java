package orca.ektorp.repository;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import orca.ektorp.actor;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;


/*
 * ActorRepository for reducing repetitive code and facilitate management of design documents.
 * It maintains views, automatic updates, query methods, etc.
 * @author Claris Castillo
 */
public class ActorRepository extends CouchDbRepositorySupport<actor>{

	public ActorRepository(CouchDbConnector db) {
		super(actor.class, db);
		initStandardDesignDocument();
		
	}
	
	public void update(actor entity){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		entity.setAlive(dateFormat.format(date));
		this.db.update(entity);
	}
}
