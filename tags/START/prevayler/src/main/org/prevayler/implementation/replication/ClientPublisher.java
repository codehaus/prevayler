// Prevayler(TM) - The Open-Source Prevalence Layer.
// Copyright (C) 2001-2003 Klaus Wuestefeld.
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License version 2.1 as published by the Free Software Foundation. This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.

package org.prevayler.implementation.replication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;

import org.prevayler.Clock;
import org.prevayler.Transaction;
import org.prevayler.implementation.clock.BrokenClock;
import org.prevayler.implementation.publishing.TransactionPublisher;
import org.prevayler.implementation.publishing.TransactionSubscriber;


/** Reserved for future implementation.
 */
public class ClientPublisher implements TransactionPublisher {

	private final BrokenClock _clock = new BrokenClock();

	private TransactionSubscriber _subscriber;
	private final Object _upToDateMonitor = new Object();

	private Transaction _myTransaction;
	private final Object _myTransactionMonitor = new Object();
	private RuntimeException _myTransactionRuntimeException;
	private Error _myTransactionError;

	private final ObjectOutputStream _toServer;
	private final ObjectInputStream _fromServer;


	public ClientPublisher(String serverIpAddress, int serverPort) throws IOException, ClassNotFoundException {
		System.out.println("The replication logic is still under development.");
		Socket socket = new Socket(serverIpAddress, serverPort);
		_toServer = new ObjectOutputStream(socket.getOutputStream());   // Get the OUTPUT stream first. JDK 1.3.1_01 for Windows will lock up if you get the INPUT stream first.
		_fromServer = new ObjectInputStream(socket.getInputStream());
		startListening();
	}


	private void startListening() {
		Thread listener = new Thread() {
			public void run() {
				try {
					while (true) receiveTransactionFromServer();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		listener.setDaemon(true);
		listener.start();
	}


	public synchronized void addSubscriber(TransactionSubscriber subscriber, long initialTransaction) throws IOException, ClassNotFoundException {
		if (_subscriber != null) throw new UnsupportedOperationException("The current implementation of RemoteTransactionPublisher can only support one subscriber. Future implementations will support more.");
		_subscriber = subscriber;
		synchronized (_upToDateMonitor) {
			_toServer.writeObject(new Long(initialTransaction));
			wait(_upToDateMonitor);
		}
	}


	public synchronized void publish(Transaction transaction) {
		if (_subscriber == null) throw new IllegalStateException("To publish a transaction, the RemoteTransactionPublisher needs a registered subscriber.");
		synchronized (_myTransactionMonitor) {
			_myTransaction = transaction;
			
			try {
				_toServer.writeObject(transaction);
			} catch (IOException iox) {
				iox.printStackTrace();
				while (true) Thread.yield();
			}
			wait(_myTransactionMonitor);
			
			throwEventualErrors();
		}
	}


	private void throwEventualErrors() throws RuntimeException, Error {
		try {
			if (_myTransactionRuntimeException != null) throw _myTransactionRuntimeException;
			if (_myTransactionError != null) throw _myTransactionError;
		} finally {
			_myTransactionRuntimeException = null;
			_myTransactionError = null;
		}
	}


	private void receiveTransactionFromServer() throws IOException, ClassNotFoundException {
		Object transactionCandidate = _fromServer.readObject();
		
		if (transactionCandidate.equals(ServerConnection.SUBSCRIBER_UP_TO_DATE)) {
			synchronized (_upToDateMonitor) { _upToDateMonitor.notify(); }
			return;
		}

		if (transactionCandidate instanceof RuntimeException) {
			_myTransactionRuntimeException = (RuntimeException)transactionCandidate;
			notifyMyTransactionMonitor();
			return;
		}
		if (transactionCandidate instanceof Error) {
			_myTransactionError = (Error)transactionCandidate;
			notifyMyTransactionMonitor();
			return;
		}

		Date timestamp = (Date)_fromServer.readObject();
		_clock.advanceTo(timestamp);

		if (transactionCandidate.equals(ServerConnection.CLOCK_TICK)) return;

		if (transactionCandidate.equals(ServerConnection.REMOTE_TRANSACTION)) {
			_subscriber.receive(_myTransaction, timestamp);
			notifyMyTransactionMonitor();
			return;
		}

		_subscriber.receive((Transaction)transactionCandidate, timestamp);
	}


	private static void wait(Object monitor) {
		try {
			monitor.wait();
		} catch (InterruptedException ix) {
			throw new RuntimeException("Unexpected InterruptedException.");
		}
	}


	private void notifyMyTransactionMonitor() {
		synchronized (_myTransactionMonitor) {
			_myTransactionMonitor.notify();
		}
	}


	public Clock clock() {
		return _clock;
	}


}