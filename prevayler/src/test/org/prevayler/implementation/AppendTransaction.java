package org.prevayler.implementation;

import org.prevayler.Transaction;

import java.util.Date;

class AppendTransaction implements Transaction {

	String toAdd;

	private AppendTransaction() {
		// Skaringa requires a default constructor, but XStream does not.
	}

	public AppendTransaction(String toAdd) {
		this.toAdd = toAdd;
	}

	public void executeOn(Object prevalentSystem, Date executionTime) {
		StringBuffer system = (StringBuffer) prevalentSystem;
		system.append(toAdd);
	}

}
