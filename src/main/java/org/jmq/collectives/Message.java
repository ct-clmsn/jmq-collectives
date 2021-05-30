//  Copyright (c) 2021 Christopher Taylor
//
//  Distributed under the Boost Software License, Version 1.0. (See accompanying
//  file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.jmq.collectives;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

public class Message implements Serializable {
    private boolean is_heartbeat;
    private long rank;
    private byte [] bytes;

    public Message(final byte [] buf, final boolean is_heartbeat, final long rank) {
        this.is_heartbeat = is_heartbeat;
        this.rank = rank;
        this.bytes = buf;
    }

    public boolean isHeartbeat() { return this.is_heartbeat; }
    public long getRank() { return this.rank; }
    public byte [] bytes() { return this.bytes; }

    private void writeObject(ObjectOutputStream oos) 
      throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(this.is_heartbeat);
        oos.writeObject(this.rank);
        oos.writeObject(this.bytes);
    }

    private void readObject(ObjectInputStream ois) 
      throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.is_heartbeat = (boolean) ois.readObject();
        this.rank = (long) ois.readObject();
        this.bytes = (byte []) ois.readObject();
    }

}
