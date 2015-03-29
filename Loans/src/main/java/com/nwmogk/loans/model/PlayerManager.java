package com.nwmogk.loans.model;

import java.util.List;
import java.util.UUID;

import javax.jdo.Query;
import javax.jdo.Transaction;

import com.nwmogk.loans.api.EntityType;
import com.nwmogk.loans.jdo.FinancialEntity;

public class PlayerManager {

	private Economy	eco;

	public PlayerManager( Economy economy ) {

		this.eco = economy;
	}

	/**
	 * Attempts to add a Player corresponding to the given UUID to the
	 * FinancialEntities table. The method reports that the entity exists.
	 * 
	 * @param playerID
	 *            UUID of the player to be added.
	 * @return true if the entity is in the FinancialEntities table by the end
	 *         of the method, false otherwise.
	 */
	public boolean addPlayer( UUID playerId, String name ) {

		// Check to see if the entity is already in the table
		if ( inFinancialEntitiesTable( playerId ) ) { return true;

		}

		Transaction tx = eco.pm.currentTransaction();

		tx.begin();

		FinancialEntity newPlayer = new FinancialEntity( playerId, name,
				EntityType.PLAYER );

		eco.pm.makePersistent( newPlayer );

		tx.commit();

		return !tx.isActive();

	}

	/**
	 * Checks if the given UUID represents an entry in the FinancialEntities
	 * table.
	 * 
	 * @param entityID
	 *            UUID to check
	 * @return true if the entry was found, false otherwise.
	 */
	public boolean inFinancialEntitiesTable( UUID entityId ) {

		Query q = eco.pm.newQuery( FinancialEntity.class );
		q.setFilter( "this.entityId.equals(otherId)" );
		q.declareParameters( "UUID otherId" );

		@SuppressWarnings( "rawtypes" )
		List results = (List) q.execute( entityId );

		return !results.isEmpty();

	}

}
