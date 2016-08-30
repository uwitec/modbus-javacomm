/***
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package net.wimpi.modbus.io;

import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.msg.ModbusMessage;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;

import java.io.IOException;

/**
 * Interface defining the I/O mechanisms for
 * <tt>ModbusMessage</tt> instances.
 *
 * @author Dieter Wimberger
 * @version 1.2
 */
public interface ModbusTransport {

    /**
     * Closes the raw input and output streams of
     * this <tt>ModbusTransport</tt>.
     *
     * @throws IOException if a stream         cannot be closed properly.
     */
    public void close() throws IOException;

    /**
     * Writes a <tt>ModbusMessage</tt> to the
     * output stream of this <tt>ModbusTransport</tt>.
     *
     * @param msg a ModbusMessage.
     * @throws ModbusIOException data cannot be written properly to the raw output stream of this ModbusTransport.
     */
    public void writeMessage(ModbusMessage msg) throws ModbusIOException;

    /**
     * Reads a <tt>ModbusRequest</tt> from the
     * input stream of this <tt>ModbusTransport</tt>.
     *
     * @return req the ModbusRequest read from the underlying stream.
     * @throws ModbusIOException data cannot be read properly from the raw input stream of this ModbusTransport.
     */
    public ModbusRequest readRequest() throws ModbusIOException;

    /**
     * Reads a ModbusResponse from the
     * input stream of this ModbusTransport.
     *
     * @return res the ModbusResponse read from the underlying stream.
     * @throws ModbusIOException data cannot be read properly from the raw input stream of this ModbusTransport.
     */
    public ModbusResponse readResponse() throws ModbusIOException;

}//class ModbusTransport
