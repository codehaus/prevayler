//Copyright (C) 2004 Klaus Wuestefeld
//This is free software. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license distributed along with this file for more details.
//Contributions: Alexandre Nodari.

package org.prevayler.foundation.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.prevayler.foundation.Cool;


public class NetworkMock extends BaseNetworkMock 
                         implements OldNetwork {

	
	public synchronized ObjectSocket openSocket(String serverIpAddress, int serverPort) throws IOException {
	    crashIfNotLocal(serverIpAddress);
        return startClient(serverPort);
		
	}

	public synchronized ObjectServerSocket openObjectServerSocket(int serverPort) throws IOException {
	    return startServer(serverPort);
	}
}
