//Prevayler(TM) - The Free-Software Prevalence Layer.
//Copyright (C) 2001-2003 Klaus Wuestefeld
//This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

package org.prevayler.implementation.publishing;

import java.io.IOException;
import java.util.Date;

import org.prevayler.Clock;
import org.prevayler.Transaction;
import org.prevayler.foundation.Cool;
import org.prevayler.foundation.Turn;
import org.prevayler.implementation.clock.PausableClock;
import org.prevayler.implementation.logging.TransactionLogger;
import org.prevayler.implementation.publishing.censorship.*;

public class CentralPublisher extends AbstractPublisher {

	private final PausableClock _pausableClock;
	private final TransactionCensor _censor;
	private final TransactionLogger _logger;

	private final Object _pendingSubscriptionMonitor = new Object();
	private volatile int _pendingPublications = 0;
	private final Object _pendingPublicationsMonitor = new Object();

	private Turn _nextTurn = Turn.first();
	private final Object _nextTurnMonitor = new Object();

	private boolean _foodTasterIsDead = false;
	private int _pipelinedTransactions = 0;
	private final Object _pipelinedTransactionsMonitor = new Object();


	public CentralPublisher(Clock clock, TransactionCensor censor, TransactionLogger logger) {
		super(new PausableClock(clock));
		_pausableClock = (PausableClock)_clock; //This is just to avoid casting the inherited _clock every time.

		_censor = censor;
		_logger = logger;
	}


	public void publish(Transaction transaction) {
		synchronized (_pendingSubscriptionMonitor) {  //Blocks all new publications until the subscription is over.
			synchronized (_pendingPublicationsMonitor) {
				if (_pendingPublications == 0) _pausableClock.pause();
				_pendingPublications++;
			}
		}

		try {
			publishWithoutWorryingAboutNewSubscriptions(transaction);  // Suggestions for a better method name are welcome.  :)
		} finally {
			synchronized (_pendingPublicationsMonitor) {
				_pendingPublications--;
				if (_pendingPublications == 0) _pausableClock.resume();
			}
		}
	}


	private void publishWithoutWorryingAboutNewSubscriptions(Transaction transaction) {
		Turn myTurn = nextTurn();

		Date executionTime = realTime(myTurn);  //TODO realTime() and approve in the same turn.
		approve(transaction, executionTime, myTurn);
		_logger.log(transaction, executionTime, myTurn);
		notifySubscribers(transaction, executionTime, myTurn);
	}


	private Turn nextTurn() {
		synchronized (_nextTurnMonitor) {
			Turn result = _nextTurn;
			_nextTurn = _nextTurn.next();
			return result;
		}
	}


	private Date realTime(Turn myTurn) {
		try {
			myTurn.start();
			return _pausableClock.realTime();
		} finally {	myTurn.end(); }
	}


	private void approve(Transaction transaction, Date executionTime, Turn myTurn) throws RuntimeException, Error {
		try {
			myTurn.start();

			if (_foodTasterIsDead) {
				synchronized (_pipelinedTransactionsMonitor) {
					while (_pipelinedTransactions > 0) {
						Cool.wait(_pipelinedTransactionsMonitor);
					}
				}
			}

			_censor.approve(transaction, executionTime);

			_foodTasterIsDead = false;

			synchronized (_pipelinedTransactionsMonitor) {
				_pipelinedTransactions++;
			}

			myTurn.end();
		} catch (RuntimeException r) { dealWithError(myTurn); throw r;
		} catch (Error e) {	dealWithError(myTurn); throw e; }
	}

	
	private void dealWithError(Turn myTurn) {
		_foodTasterIsDead = true;
		myTurn.alwaysSkip();
	}


	private void notifySubscribers(Transaction transaction, Date executionTime, Turn myTurn) {
		try {
			myTurn.start();
			_pausableClock.advanceTo(executionTime);
			notifySubscribers(transaction, executionTime);

			synchronized (_pipelinedTransactionsMonitor) {
				_pipelinedTransactions--;
				if (_pipelinedTransactions == 0) {
					_pipelinedTransactionsMonitor.notifyAll();
				}
			}
		} finally {	myTurn.end(); }
	}


	public void addSubscriber(TransactionSubscriber subscriber, long initialTransaction) throws IOException, ClassNotFoundException {
		synchronized (_pendingSubscriptionMonitor) {
			while (_pendingPublications != 0) Thread.yield();
			
			_logger.update(subscriber, initialTransaction);
			super.addSubscriber(subscriber);
		}
	}


	public void close() throws IOException { _logger.close(); }

}