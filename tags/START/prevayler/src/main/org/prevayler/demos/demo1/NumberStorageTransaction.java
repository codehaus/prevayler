// Prevayler(TM) - The Open-Source Prevalence Layer.
// Copyright (C) 2001-2003 Klaus Wuestefeld, Paul Hammant
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License version 2.1 as published by the Free Software Foundation. This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.

package org.prevayler.demos.demo1;

import java.util.Date;

import org.prevayler.Transaction;


/**
 * To change the state of the business objects, the client code must use a Transaction like this one.
 */
class NumberStorageTransaction implements Transaction {

	private final int _numberToKeep;


	NumberStorageTransaction(int numberToKeep) {
		_numberToKeep = numberToKeep;
	}

	public void executeOn(Object prevalentSystem, Date ignored) {
		((NumberKeeper)prevalentSystem).keep(_numberToKeep);
	}
}