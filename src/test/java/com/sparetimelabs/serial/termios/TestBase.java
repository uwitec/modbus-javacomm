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
package com.sparetimelabs.serial.termios;

import static com.sparetimelabs.serial.termios.JTermios.errno;

/**
 * The type Test base.
 */
public class TestBase {

    /**
     * The constant m_TestPortName.
     */
    protected static volatile String m_TestPortName;
    /**
     * The constant m_Tab.
     */
    protected static int m_Tab;
    /**
     * The constant m_Progress.
     */
    protected static int m_Progress;
    private static volatile long m_T0;

    /**
     * S.
     *
     * @param result the result
     * @throws TestFailedException the test failed exception
     */
    public static void S(int result) throws TestFailedException {
        S(result, null);
    }

    /**
     * S.
     *
     * @param result the result
     * @param msg    the msg
     * @throws TestFailedException the test failed exception
     */
    public static void S(int result, String msg) throws TestFailedException {
        if (result == -1) {
            fail(msg == null ? "Operation failed with errno %d"
                    : msg + " (errno %d)", errno());
        }
    }

    /**
     * Begin.
     *
     * @param name the name
     */
    static void begin(String name) {
        System.out.printf("%-46s", name);
        m_Tab = 46;
        m_T0 = System.currentTimeMillis();
        m_Progress = 0;
    }

    /**
     * Sleep.
     *
     * @param t the t
     * @throws InterruptedException the interrupted exception
     */
    static protected void sleep(int t) throws InterruptedException {
        int m = 1000;
        while (t > 0) {
            Thread.sleep(t > m ? m : t);
            t -= m;
            while ((System.currentTimeMillis() - m_T0) / m > m_Progress) {
                System.out.print(".");
                m_Tab--;
                m_Progress++;
            }
        }
    }

    /**
     * Fail.
     *
     * @param format the format
     * @param args   the args
     * @throws TestFailedException the test failed exception
     */
    static void fail(String format, Object... args) throws TestFailedException {
        System.out.println(" FAILED");
        System.out.println("------------------------------------------------------------");
        System.out.printf(format, args);
        System.out.println();
        System.out.println("------------------------------------------------------------");
        throw new TestFailedException();
    }

    /**
     * Finished ok.
     */
    static void finishedOK() {
        finishedOK("");
    }

    /**
     * Finished ok.
     *
     * @param format the format
     * @param args   the args
     */
    static void finishedOK(String format, Object... args) {
        for (int i = 0; i < m_Tab; i++) {
            System.out.print(".");
        }
        System.out.printf(" OK " + format, args);
        System.out.println();
    }

    /**
     * Init.
     *
     * @param args the args
     */
    static public void init(String[] args) {
        m_TestPortName = "cu.usbserial-FTOXM3NX";
        if (args.length > 0) {
            m_TestPortName = args[0];
        } else {
            for (String port : JTermios.getPortList()) {
                m_TestPortName = port;
            }
        }
    }

    /**
     * Gets port name.
     *
     * @return the port name
     */
    static public String getPortName() {
        return m_TestPortName;
    }
}
