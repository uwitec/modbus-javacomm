/*
 * Copyright (c) 2011, Kustaa Nyholm / SpareTimeLabs
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list 
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this 
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *  
 * Neither the name of the Kustaa Nyholm or SpareTimeLabs nor the names of its 
 * contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package com.sparetimelabs.serial;

/**
 * The type Parallel port.
 */
public class ParallelPort {

    /**
     * The constant LPT_MODE_ANY.
     */
    public static final int LPT_MODE_ANY = 0;
    /**
     * The constant LPT_MODE_SPP.
     */
    public static final int LPT_MODE_SPP = 1;
    /**
     * The constant LPT_MODE_PS2.
     */
    public static final int LPT_MODE_PS2 = 2;
    /**
     * The constant LPT_MODE_EPP.
     */
    public static final int LPT_MODE_EPP = 3;
    /**
     * The constant LPT_MODE_ECP.
     */
    public static final int LPT_MODE_ECP = 4;
    /**
     * The constant LPT_MODE_NIBBLE.
     */
    public static final int LPT_MODE_NIBBLE = 5;

    /**
     * Add event listener.
     *
     * @param lsnr the lsnr
     */
    void addEventListener(ParallelPortEventListener lsnr) {

    }

    /**
     * Gets mode.
     *
     * @return the mode
     */
    int getMode() {
        return LPT_MODE_ANY;
    }

    /**
     * Gets output buffer free.
     *
     * @return the output buffer free
     */
    int getOutputBufferFree() {
        return 0;
    }

    /**
     * Is paper out boolean.
     *
     * @return the boolean
     */
    boolean isPaperOut() {
        return false;
    }

    /**
     * Is printer busy boolean.
     *
     * @return the boolean
     */
    boolean isPrinterBusy() {
        return false;
    }

    /**
     * Is printer error boolean.
     *
     * @return the boolean
     */
    boolean isPrinterError() {
        return false;
    }

    /**
     * Is printer selected boolean.
     *
     * @return the boolean
     */
    boolean isPrinterSelected() {
        return false;
    }

    /**
     * Is printer timed out boolean.
     *
     * @return the boolean
     */
    boolean isPrinterTimedOut() {
        return false;
    }

    /**
     * Notify on buffer.
     *
     * @param notify the notify
     */
    void notifyOnBuffer(boolean notify) {

    }

    /**
     * Notify on error.
     *
     * @param notify the notify
     */
    void notifyOnError(boolean notify) {

    }

    /**
     * Remove event listener.
     */
    void removeEventListener() {

    }

    /**
     * Restart.
     */
    void restart() {

    }

    /**
     * Sets mode.
     *
     * @param mode the mode
     * @return the mode
     * @throws UnsupportedCommOperationException the unsupported comm operation exception
     */
    int setMode(int mode) throws UnsupportedCommOperationException {
        return getMode();
    }

    /**
     * Suspend.
     */
    void suspend() {

    }
}
