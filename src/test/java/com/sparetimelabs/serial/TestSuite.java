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

import static junit.framework.Assert.fail;

/**
 * The type Test suite.
 */
public class TestSuite {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        // Native.setProtected(false);
        if(args.length>0) {

            //jtermios.JTermios.JTermiosLogging.setLogMask(255);
            // System.setProperty("purejavacomm.usepoll", "true");
            // System.setProperty("purejavacomm.rawreadmode", "true");
            try {
                System.out.println("PureJavaComm Test Suite");
                System.out.println("Using port: " + TestBase.getPortName());
                TestFreeFormPortIdentifiers.testMissingPortInCommPortIdentifier();
                TestFreeFormPortIdentifiers.testDevicePathInCommPortIdentifier();
                TestFreeFormPortIdentifiers.testDevicePathToInvalidTTYInCommPortIdentifier();
                Test1.run();
                Test2.run(19200);
                Test3.run();
                Test4.run();
                Test5.run();
                Test6.run();
                Test7.run();
                Test8.run(!(args.length > 1 && args[1].equals("limited")));
                Test9.run();
                Test10.run();
                Test11.run();
                Test12.run();
                Test13.run();
                Test15.run();
                Test16.run();
                System.out.println("All tests passed OK.");
            } catch (TestBase.TestFailedException e) {
                fail("Test failure");
            }
        }else{
            System.out.println("not runnable without args");
        }
    }
}
