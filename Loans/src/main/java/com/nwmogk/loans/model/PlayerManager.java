package com.nwmogk.loans.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jdo.Query;
import javax.jdo.Transaction;

import com.nwmogk.loans.api.EntityType;
import com.nwmogk.loans.jdo.CashTransaction;
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
	public boolean addPlayer( UUID playerId ) {

		// Check to see if the entity is already in the table
		if ( inFinancialEntitiesTable( playerId ) )
			return true;

		if ( !eco.plugin.isPlayerOnline( playerId ) )
			return false;

		String name = eco.plugin.getPlayerName( playerId );

		Transaction tx = eco.pm.currentTransaction();
		
		BigDecimal startingCash = new BigDecimal(eco.cfg.getSettings().getProperty( "economy.initial-money" ));
		FinancialEntity centralBank = getFinancialEntity("CentralBank", EntityType.BANK);

		try {
			tx.begin();

			FinancialEntity newPlayer = new FinancialEntity( playerId, name,
					EntityType.PLAYER );
			
			newPlayer.addCash( startingCash );
			
			CashTransaction initialAmount = new CashTransaction(centralBank, newPlayer, new Timestamp(new Date().getTime()), startingCash);

			eco.pm.makePersistent( newPlayer );
			eco.pm.makePersistent( initialAmount );

			tx.commit();
		} finally {
			if ( tx.isActive() )
				tx.rollback(); // Error occurred so rollback the PM transaction
		}

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
	
	public FinancialEntity getFinancialEntity(String name, EntityType type) {
		Query q = eco.pm.newQuery( FinancialEntity.class );
		q.setFilter( "this.name.equals(search) && this.type.equals(t)" );
		q.declareParameters( "String search" );
		q.declareParameters( "EntityType t" );

		@SuppressWarnings( "unchecked" )
		List<FinancialEntity> results = (List<FinancialEntity>) q.execute( name, type );
		
		return results.get( 0 );
	}

}
