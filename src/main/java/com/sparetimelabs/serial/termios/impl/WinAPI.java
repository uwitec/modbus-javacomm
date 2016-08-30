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
package com.sparetimelabs.serial.termios.impl;

import com.sun.jna.FromNativeContext;
import com.sun.jna.IntegerType;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.List;

import static com.sparetimelabs.serial.termios.JTermios.JTermiosLogging.log;
import static com.sparetimelabs.serial.termios.JTermios.JTermiosLogging.ref;

/**
 * This WinAPI class implements a simple wrapper API to access the Windows COM
 * ports from Java.
 * <p>
 * The purpose is to follow reasonably closely the WIN32 API so that COM port
 * related C-code can be ported to Java almost as-is with little changes when
 * this class is statically imported.
 * <p>
 * This is a pure lightweight wrapper around WIN32 API calls with no added
 * syntactic sugar, functionality or niceties.
 * <p>
 * Here is a rude example:
 * <p>
 * <pre>
 * <code>
 * import static jtermios.windows.WinAPI.*;
 * ...
 *    byte[] buffer = "Hello World".getBytes();
 *    HANDLE hcomm = CreateFileA( "COM5:", GENERIC_READ |GENERIC_WRITE, 0, null, 0, 0, null );
 *    int[] wrtn = {0};
 *    WriteFile(hcomm, buffer, buffer.length, wrtn);
 *    CloseHandle(hcomm);
 * </code>
 * </pre>
 * <p>
 * Can't get much closer to C-code, what!
 * <p>
 * In addition to the basic open/close/read/write and setup operations this
 * class also makes available enough of the WIN32 Event API to make it possible
 * to use overlapped (asynchronous) I/O on COM ports.
 * <p>
 * <p>
 * Note that overlapped IO API is full of fine print. Especially worth
 * mentioning is that the OVERLAPPED structure cannot use autosync as it is
 * modified (by Windows) outside the function calls that use it. OVERLAPPED
 * takes care of not autosyncing but it is best to us the writeField() methods
 * to set fields of OVERLAPPED.
 * <p>
 * <pre>
 * <code>
 *    OVERLAPPED ovl = new OVERLAPPED();
 *    ovl.writeField("hEvent",CreateEvent(null, true, false, null));
 *   ...
 *    WriteFile(hComm, txm, txb.length, txn, ovl);
 *   ...
 *    GetOverlappedResult(hComm, ovl, txn, true);
 * </code>
 * </pre>
 *
 * @author Kustaa Nyholm
 */
class WinAPI {

    private static Windows_kernel32_lib m_K32lib;
    private static Windows_kernel32_lib_Direct m_K32libDM;
    private static WaitMultiple m_K32libWM;

    static {
        // Moved to static per JNA recommendations
        Native.setPreserveLastError(true); // For older JNA to hopefully preserve last error although we don't use it with Windows
        // This had to be separated out for Direct Mapping (no non-primative arrays)
        m_K32libWM = (WaitMultiple) Native.loadLibrary("kernel32", WaitMultiple.class, com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS);
        // Added com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS so we don't mix/match WString and String
        Native.register(Windows_kernel32_lib_Direct.class, NativeLibrary.getInstance("kernel32", com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS));
        m_K32libDM = new Windows_kernel32_lib_Direct();
        m_K32lib = m_K32libDM;
        // m_K32lib = (Windows_kernel32_lib) Native.loadLibrary("kernel32", Windows_kernel32_lib.class, com.sun.jna.win32.W32APIOptions.ASCII_OPTIONS);
    }

    // The following is to fix JNA's non-thread-local getLastError implementation
    private static final ThreadLocal<int[]> LastError = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[1];  // Arrays are always initialized to zero values
        }
    };

    /**
     * The type Handle.
     */
    public static class HANDLE extends PointerType {

        private boolean immutable;

        /**
         * Instantiates a new Handle.
         */
        public HANDLE() {
        }

        /**
         * Instantiates a new Handle.
         *
         * @param p the p
         */
        public HANDLE(Pointer p) {
            setPointer(p);
            immutable = true;
        }

        public Object fromNative(Object nativeValue, FromNativeContext context) {
            Object o = super.fromNative(nativeValue, context);
            if (NULL.equals(o)) {
                return NULL;
            }
            if (INVALID_HANDLE_VALUE.equals(o)) {
                return INVALID_HANDLE_VALUE;
            }
            return o;
        }

        public void setPointer(Pointer p) {
            if (immutable) {
                throw new UnsupportedOperationException("immutable");
            }

            super.setPointer(p);
        }
    }

    /**
     * The constant INVALID_HANDLE_VALUE.
     */
    public static HANDLE INVALID_HANDLE_VALUE = new HANDLE(Pointer.createConstant(Pointer.SIZE == 8 ? -1 : 0xFFFFFFFFL));
    /**
     * The constant NULL.
     */
    public static HANDLE NULL = new HANDLE(Pointer.createConstant(0));

    /**
     * The type Windows kernel 32 lib direct.
     */
    public static class Windows_kernel32_lib_Direct implements Windows_kernel32_lib {

        native public HANDLE CreateFile(String name, int access, int mode, SECURITY_ATTRIBUTES security, int create, int atteribs, Pointer template) throws LastErrorException;

        native public boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn, Pointer lpOverlapped) throws LastErrorException;

        native public boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED lpOverlapped) throws LastErrorException;

        native public boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd, Pointer lpOverlapped) throws LastErrorException;

        native public boolean ReadFile(HANDLE hFile, Pointer lpBuffer, int rdn, int[] nrd, OVERLAPPED lpOverlapped) throws LastErrorException;

        native public boolean FlushFileBuffers(HANDLE hFile) throws LastErrorException;

        native public boolean PurgeComm(HANDLE hFile, int qmask) throws LastErrorException;

        native public boolean CancelIo(HANDLE hFile) throws LastErrorException;

        native public boolean CloseHandle(HANDLE hFile) throws LastErrorException;

        native public boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s) throws LastErrorException;

        native public boolean SetCommMask(HANDLE hFile, int dwEvtMask) throws LastErrorException;

        native public boolean GetCommMask(HANDLE hFile, int[] dwEvtMask) throws LastErrorException;

        native public boolean GetCommState(HANDLE hFile, DCB dcb) throws LastErrorException;

        native public boolean SetCommState(HANDLE hFile, DCB dcb) throws LastErrorException;

        native public boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS tout) throws LastErrorException;

        native public boolean SetupComm(HANDLE hFile, int dwInQueue, int dwOutQueue) throws LastErrorException;

        native public boolean SetCommBreak(HANDLE hFile) throws LastErrorException;

        native public boolean ClearCommBreak(HANDLE hFile) throws LastErrorException;

        native public boolean GetCommModemStatus(HANDLE hFile, int[] stat) throws LastErrorException;

        native public boolean EscapeCommFunction(HANDLE hFile, int func) throws LastErrorException;

        native public HANDLE CreateEvent(SECURITY_ATTRIBUTES lpEventAttributes, boolean bManualReset, boolean bInitialState, String lpName) throws LastErrorException;

        native public boolean ResetEvent(HANDLE hEvent) throws LastErrorException;

        native public boolean SetEvent(HANDLE hEvent) throws LastErrorException;

        native public boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED lpOverlapped) throws LastErrorException;

        native public int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);

        native public boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED lpOverlapped, int[] lpNumberOfBytesTransferred, boolean bWait) throws LastErrorException;

        native public int FormatMessageW(int flags, Pointer src, int msgId, int langId, Pointer dst, int sze, Pointer va_list);

        native public int QueryDosDevice(String name, byte[] buffer, int bsize) throws LastErrorException;

    }

    /**
     * The interface Wait multiple.
     */
    public interface WaitMultiple extends StdCallLibrary {

        /**
         * Wait for multiple objects int.
         *
         * @param nCount         the n count
         * @param lpHandles      the lp handles
         * @param bWaitAll       the b wait all
         * @param dwMilliseconds the dw milliseconds
         * @return the int
         */
        public int WaitForMultipleObjects(int nCount, HANDLE[] lpHandles, boolean bWaitAll, int dwMilliseconds);
    }

    /**
     * The interface Windows kernel 32 lib.
     */
    public interface Windows_kernel32_lib extends StdCallLibrary {

        /**
         * Create file handle.
         *
         * @param name     the name
         * @param access   the access
         * @param mode     the mode
         * @param security the security
         * @param create   the create
         * @param atteribs the atteribs
         * @param template the template
         * @return the handle
         */
        public HANDLE CreateFile(String name, int access, int mode, SECURITY_ATTRIBUTES security, int create, int atteribs, Pointer template);

        /**
         * Write file boolean.
         *
         * @param hFile        the h file
         * @param buf          the buf
         * @param wrn          the wrn
         * @param nwrtn        the nwrtn
         * @param lpOverlapped the lp overlapped
         * @return the boolean
         */
        public boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn, Pointer lpOverlapped);

        /**
         * Write file boolean.
         *
         * @param hFile        the h file
         * @param buf          the buf
         * @param wrn          the wrn
         * @param nwrtn        the nwrtn
         * @param lpOverlapped the lp overlapped
         * @return the boolean
         */
        public boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED lpOverlapped);

        /**
         * Read file boolean.
         *
         * @param hFile        the h file
         * @param buf          the buf
         * @param rdn          the rdn
         * @param nrd          the nrd
         * @param lpOverlapped the lp overlapped
         * @return the boolean
         */
        public boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd, Pointer lpOverlapped);

        /**
         * Read file boolean.
         *
         * @param hFile        the h file
         * @param lpBuffer     the lp buffer
         * @param rdn          the rdn
         * @param nrd          the nrd
         * @param lpOverlapped the lp overlapped
         * @return the boolean
         */
        public boolean ReadFile(HANDLE hFile, Pointer lpBuffer, int rdn, int[] nrd, OVERLAPPED lpOverlapped);

        /**
         * Flush file buffers boolean.
         *
         * @param hFile the h file
         * @return the boolean
         */
        public boolean FlushFileBuffers(HANDLE hFile);

        /**
         * Purge comm boolean.
         *
         * @param hFile the h file
         * @param qmask the qmask
         * @return the boolean
         */
        public boolean PurgeComm(HANDLE hFile, int qmask);

        /**
         * Cancel io boolean.
         *
         * @param hFile the h file
         * @return the boolean
         */
        public boolean CancelIo(HANDLE hFile);

        /**
         * Close handle boolean.
         *
         * @param hFile the h file
         * @return the boolean
         */
        public boolean CloseHandle(HANDLE hFile);

        /**
         * Clear comm error boolean.
         *
         * @param hFile the h file
         * @param n     the n
         * @param s     the s
         * @return the boolean
         */
        public boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s);

        /**
         * Set comm mask boolean.
         *
         * @param hFile     the h file
         * @param dwEvtMask the dw evt mask
         * @return the boolean
         */
        public boolean SetCommMask(HANDLE hFile, int dwEvtMask);

        /**
         * Get comm mask boolean.
         *
         * @param hFile     the h file
         * @param dwEvtMask the dw evt mask
         * @return the boolean
         */
        public boolean GetCommMask(HANDLE hFile, int[] dwEvtMask);

        /**
         * Get comm state boolean.
         *
         * @param hFile the h file
         * @param dcb   the dcb
         * @return the boolean
         */
        public boolean GetCommState(HANDLE hFile, DCB dcb);

        /**
         * Set comm state boolean.
         *
         * @param hFile the h file
         * @param dcb   the dcb
         * @return the boolean
         */
        public boolean SetCommState(HANDLE hFile, DCB dcb);

        /**
         * Set comm timeouts boolean.
         *
         * @param hFile the h file
         * @param tout  the tout
         * @return the boolean
         */
        public boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS tout);

        /**
         * Setup comm boolean.
         *
         * @param hFile      the h file
         * @param dwInQueue  the dw in queue
         * @param dwOutQueue the dw out queue
         * @return the boolean
         */
        public boolean SetupComm(HANDLE hFile, int dwInQueue, int dwOutQueue);

        /**
         * Set comm break boolean.
         *
         * @param hFile the h file
         * @return the boolean
         */
        public boolean SetCommBreak(HANDLE hFile);

        /**
         * Clear comm break boolean.
         *
         * @param hFile the h file
         * @return the boolean
         */
        public boolean ClearCommBreak(HANDLE hFile);

        /**
         * Get comm modem status boolean.
         *
         * @param hFile the h file
         * @param stat  the stat
         * @return the boolean
         */
        public boolean GetCommModemStatus(HANDLE hFile, int[] stat);

        /**
         * Escape comm function boolean.
         *
         * @param hFile the h file
         * @param func  the func
         * @return the boolean
         */
        public boolean EscapeCommFunction(HANDLE hFile, int func);

        /**
         * Create event handle.
         *
         * @param lpEventAttributes the lp event attributes
         * @param bManualReset      the b manual reset
         * @param bInitialState     the b initial state
         * @param lpName            the lp name
         * @return the handle
         */
        public HANDLE CreateEvent(SECURITY_ATTRIBUTES lpEventAttributes, boolean bManualReset, boolean bInitialState, String lpName);

        /**
         * Reset event boolean.
         *
         * @param hEvent the h event
         * @return the boolean
         */
        public boolean ResetEvent(HANDLE hEvent);

        /**
         * Set event boolean.
         *
         * @param hEvent the h event
         * @return the boolean
         */
        public boolean SetEvent(HANDLE hEvent);

        /**
         * Wait comm event boolean.
         *
         * @param hFile        the h file
         * @param lpEvtMask    the lp evt mask
         * @param lpOverlapped the lp overlapped
         * @return the boolean
         */
        public boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED lpOverlapped);

        /**
         * Wait for single object int.
         *
         * @param hHandle        the h handle
         * @param dwMilliseconds the dw milliseconds
         * @return the int
         */
        public int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);

        /**
         * Get overlapped result boolean.
         *
         * @param hFile                      the h file
         * @param lpOverlapped               the lp overlapped
         * @param lpNumberOfBytesTransferred the lp number of bytes transferred
         * @param bWait                      the b wait
         * @return the boolean
         */
        public boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED lpOverlapped, int[] lpNumberOfBytesTransferred, boolean bWait);

        /**
         * Format message w int.
         *
         * @param flags   the flags
         * @param src     the src
         * @param msgId   the msg id
         * @param langId  the lang id
         * @param dst     the dst
         * @param sze     the sze
         * @param va_list the va list
         * @return the int
         */
        public int FormatMessageW(int flags, Pointer src, int msgId, int langId, Pointer dst, int sze, Pointer va_list);

        /**
         * Query dos device int.
         *
         * @param name   the name
         * @param buffer the buffer
         * @param bsize  the bsize
         * @return the int
         */
        public int QueryDosDevice(String name, byte[] buffer, int bsize);

    }

    /**
     * The constant ERROR_INSUFFICIENT_BUFFER.
     */
    // There seems to be very little rhyme or reason from which header file
    // these come from in C, so I did not bother to keep track of the
    // origin of these constants
    public static final int ERROR_INSUFFICIENT_BUFFER = 122;
    /**
     * The constant MAXDWORD.
     */
    public static final int MAXDWORD = 0xFFFFFFFF;
    /**
     * The constant STATUS_WAIT_0.
     */
    public static final int STATUS_WAIT_0 = 0x00000000;
    /**
     * The constant STATUS_ABANDONED_WAIT_0.
     */
    public static final int STATUS_ABANDONED_WAIT_0 = 0x00000080;
    /**
     * The constant WAIT_ABANDONED.
     */
    public static final int WAIT_ABANDONED = (STATUS_ABANDONED_WAIT_0) + 0;
    /**
     * The constant WAIT_ABANDONED_0.
     */
    public static final int WAIT_ABANDONED_0 = (STATUS_ABANDONED_WAIT_0) + 0;
    /**
     * The constant WAIT_OBJECT_0.
     */
    public static final int WAIT_OBJECT_0 = ((STATUS_WAIT_0) + 0);
    /**
     * The constant WAIT_FAILED.
     */
    public static final int WAIT_FAILED = 0xFFFFFFFF;
    /**
     * The constant INFINITE.
     */
    public static final int INFINITE = 0xFFFFFFFF;
    /**
     * The constant WAIT_TIMEOUT.
     */
    public static final int WAIT_TIMEOUT = 258; //
    /**
     * The constant GENERIC_READ.
     */
    public static final int GENERIC_READ = 0x80000000;
    /**
     * The constant GENERIC_WRITE.
     */
    public static final int GENERIC_WRITE = 0x40000000;
    /**
     * The constant GENERIC_EXECUTE.
     */
    public static final int GENERIC_EXECUTE = 0x20000000;
    /**
     * The constant GENERIC_ALL.
     */
    public static final int GENERIC_ALL = 0x10000000;
    /**
     * The constant CREATE_NEW.
     */
    public static final int CREATE_NEW = 1;
    /**
     * The constant CREATE_ALWAYS.
     */
    public static final int CREATE_ALWAYS = 2;
    /**
     * The constant OPEN_EXISTING.
     */
    public static final int OPEN_EXISTING = 3;
    /**
     * The constant OPEN_ALWAYS.
     */
    public static final int OPEN_ALWAYS = 4;
    /**
     * The constant TRUNCATE_EXISTING.
     */
    public static final int TRUNCATE_EXISTING = 5;
    /**
     * The constant PURGE_TXABORT.
     */
    public static final int PURGE_TXABORT = 0x0001;
    /**
     * The constant PURGE_RXABORT.
     */
    public static final int PURGE_RXABORT = 0x0002;
    /**
     * The constant PURGE_TXCLEAR.
     */
    public static final int PURGE_TXCLEAR = 0x0004;
    /**
     * The constant PURGE_RXCLEAR.
     */
    public static final int PURGE_RXCLEAR = 0x0008;
    /**
     * The constant MS_CTS_ON.
     */
    public static final int MS_CTS_ON = 0x0010;
    /**
     * The constant MS_DSR_ON.
     */
    public static final int MS_DSR_ON = 0x0020;
    /**
     * The constant MS_RING_ON.
     */
    public static final int MS_RING_ON = 0x0040;
    /**
     * The constant MS_RLSD_ON.
     */
    public static final int MS_RLSD_ON = 0x0080;
    /**
     * The constant SETXOFF.
     */
    public static final int SETXOFF = 1;
    /**
     * The constant SETXON.
     */
    public static final int SETXON = 2;
    /**
     * The constant SETRTS.
     */
    public static final int SETRTS = 3;
    /**
     * The constant CLRRTS.
     */
    public static final int CLRRTS = 4;
    /**
     * The constant SETDTR.
     */
    public static final int SETDTR = 5;
    /**
     * The constant CLRDTR.
     */
    public static final int CLRDTR = 6;
    /**
     * The constant RESETDEV.
     */
    public static final int RESETDEV = 7;
    /**
     * The constant SETBREAK.
     */
    public static final int SETBREAK = 8;
    /**
     * The constant CLRBREAK.
     */
    public static final int CLRBREAK = 9;

    /**
     * The constant FILE_FLAG_WRITE_THROUGH.
     */
    public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;
    /**
     * The constant FILE_FLAG_OVERLAPPED.
     */
    public static final int FILE_FLAG_OVERLAPPED = 0x40000000;
    /**
     * The constant FILE_FLAG_NO_BUFFERING.
     */
    public static final int FILE_FLAG_NO_BUFFERING = 0x20000000;
    /**
     * The constant FILE_FLAG_RANDOM_ACCESS.
     */
    public static final int FILE_FLAG_RANDOM_ACCESS = 0x10000000;
    /**
     * The constant FILE_FLAG_SEQUENTIAL_SCAN.
     */
    public static final int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000;
    /**
     * The constant FILE_FLAG_DELETE_ON_CLOSE.
     */
    public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;
    /**
     * The constant FILE_FLAG_BACKUP_SEMANTICS.
     */
    public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;
    /**
     * The constant FILE_FLAG_POSIX_SEMANTICS.
     */
    public static final int FILE_FLAG_POSIX_SEMANTICS = 0x01000000;
    /**
     * The constant FILE_FLAG_OPEN_REPARSE_POINT.
     */
    public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;
    /**
     * The constant FILE_FLAG_OPEN_NO_RECALL.
     */
    public static final int FILE_FLAG_OPEN_NO_RECALL = 0x00100000;
    /**
     * The constant FILE_FLAG_FIRST_PIPE_INSTANCE.
     */
    public static final int FILE_FLAG_FIRST_PIPE_INSTANCE = 0x00080000;
    /**
     * The constant ERROR_OPERATION_ABORTED.
     */
    public static final int ERROR_OPERATION_ABORTED = 995;
    /**
     * The constant ERROR_IO_INCOMPLETE.
     */
    public static final int ERROR_IO_INCOMPLETE = 996;
    /**
     * The constant ERROR_IO_PENDING.
     */
    public static final int ERROR_IO_PENDING = 997;
    /**
     * The constant ERROR_BROKEN_PIPE.
     */
    public static final int ERROR_BROKEN_PIPE = 109;
    /**
     * The constant ERROR_MORE_DATA.
     */
    public static final int ERROR_MORE_DATA = 234;
    /**
     * The constant ERROR_FILE_NOT_FOUND.
     */
    public static final int ERROR_FILE_NOT_FOUND = 2;
    /**
     * The constant NOPARITY.
     */
    public static final byte NOPARITY = 0;
    /**
     * The constant ODDPARITY.
     */
    public static final byte ODDPARITY = 1;
    /**
     * The constant EVENPARITY.
     */
    public static final byte EVENPARITY = 2;
    /**
     * The constant MARKPARITY.
     */
    public static final byte MARKPARITY = 3;
    /**
     * The constant SPACEPARITY.
     */
    public static final byte SPACEPARITY = 4;
    /**
     * The constant ONESTOPBIT.
     */
    public static final byte ONESTOPBIT = 0;
    /**
     * The constant ONE5STOPBITS.
     */
    public static final byte ONE5STOPBITS = 1;
    /**
     * The constant TWOSTOPBITS.
     */
    public static final byte TWOSTOPBITS = 2;
    /**
     * The constant CBR_110.
     */
    public static final int CBR_110 = 110;
    /**
     * The constant CBR_300.
     */
    public static final int CBR_300 = 300;
    /**
     * The constant CBR_600.
     */
    public static final int CBR_600 = 600;
    /**
     * The constant CBR_1200.
     */
    public static final int CBR_1200 = 1200;
    /**
     * The constant CBR_2400.
     */
    public static final int CBR_2400 = 2400;
    /**
     * The constant CBR_4800.
     */
    public static final int CBR_4800 = 4800;
    /**
     * The constant CBR_9600.
     */
    public static final int CBR_9600 = 9600;
    /**
     * The constant CBR_14400.
     */
    public static final int CBR_14400 = 14400;
    /**
     * The constant CBR_19200.
     */
    public static final int CBR_19200 = 19200;
    /**
     * The constant CBR_38400.
     */
    public static final int CBR_38400 = 38400;
    /**
     * The constant CBR_56000.
     */
    public static final int CBR_56000 = 56000;
    /**
     * The constant CBR_57600.
     */
    public static final int CBR_57600 = 57600;
    /**
     * The constant CBR_115200.
     */
    public static final int CBR_115200 = 115200;
    /**
     * The constant CBR_128000.
     */
    public static final int CBR_128000 = 128000;
    /**
     * The constant CBR_256000.
     */
    public static final int CBR_256000 = 256000;
    /**
     * The constant CE_RXOVER.
     */
    public static final int CE_RXOVER = 0x0001;
    /**
     * The constant CE_OVERRUN.
     */
    public static final int CE_OVERRUN = 0x0002;
    /**
     * The constant CE_RXPARITY.
     */
    public static final int CE_RXPARITY = 0x0004;
    /**
     * The constant CE_FRAME.
     */
    public static final int CE_FRAME = 0x0008;
    /**
     * The constant CE_BREAK.
     */
    public static final int CE_BREAK = 0x0010;
    /**
     * The constant CE_TXFULL.
     */
    public static final int CE_TXFULL = 0x0100;
    /**
     * The constant CE_PTO.
     */
    public static final int CE_PTO = 0x0200;
    /**
     * The constant CE_IOE.
     */
    public static final int CE_IOE = 0x0400;
    /**
     * The constant CE_DNS.
     */
    public static final int CE_DNS = 0x0800;
    /**
     * The constant CE_OOP.
     */
    public static final int CE_OOP = 0x1000;
    /**
     * The constant CE_MODE.
     */
    public static final int CE_MODE = 0x8000;
    /**
     * The constant IE_BADID.
     */
    public static final int IE_BADID = -1;
    /**
     * The constant IE_OPEN.
     */
    public static final int IE_OPEN = -2;
    /**
     * The constant IE_NOPEN.
     */
    public static final int IE_NOPEN = -3;
    /**
     * The constant IE_MEMORY.
     */
    public static final int IE_MEMORY = -4;
    /**
     * The constant IE_DEFAULT.
     */
    public static final int IE_DEFAULT = -5;
    /**
     * The constant IE_HARDWARE.
     */
    public static final int IE_HARDWARE = -10;
    /**
     * The constant IE_BYTESIZE.
     */
    public static final int IE_BYTESIZE = -11;
    /**
     * The constant IE_BAUDRATE.
     */
    public static final int IE_BAUDRATE = -12;
    /**
     * The constant EV_RXCHAR.
     */
    public static final int EV_RXCHAR = 0x0001;
    /**
     * The constant EV_RXFLAG.
     */
    public static final int EV_RXFLAG = 0x0002;
    /**
     * The constant EV_TXEMPTY.
     */
    public static final int EV_TXEMPTY = 0x0004;
    /**
     * The constant EV_CTS.
     */
    public static final int EV_CTS = 0x0008;
    /**
     * The constant EV_DSR.
     */
    public static final int EV_DSR = 0x0010;
    /**
     * The constant EV_RLSD.
     */
    public static final int EV_RLSD = 0x0020;
    /**
     * The constant EV_BREAK.
     */
    public static final int EV_BREAK = 0x0040;
    /**
     * The constant EV_ERR.
     */
    public static final int EV_ERR = 0x0080;
    /**
     * The constant EV_RING.
     */
    public static final int EV_RING = 0x0100;
    /**
     * The constant EV_PERR.
     */
    public static final int EV_PERR = 0x0200;
    /**
     * The constant EV_RX80FULL.
     */
    public static final int EV_RX80FULL = 0x0400;
    /**
     * The constant EV_EVENT1.
     */
    public static final int EV_EVENT1 = 0x0800;
    /**
     * The constant EV_EVENT2.
     */
    public static final int EV_EVENT2 = 0x1000;

    /**
     * The constant FORMAT_MESSAGE_ALLOCATE_BUFFER.
     */
    public static final int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x00000100;
    /**
     * The constant FORMAT_MESSAGE_IGNORE_INSERTS.
     */
    public static final int FORMAT_MESSAGE_IGNORE_INSERTS = 0x00000200;
    /**
     * The constant FORMAT_MESSAGE_FROM_STRING.
     */
    public static final int FORMAT_MESSAGE_FROM_STRING = 0x00000400;
    /**
     * The constant FORMAT_MESSAGE_FROM_HMODULE.
     */
    public static final int FORMAT_MESSAGE_FROM_HMODULE = 0x00000800;
    /**
     * The constant FORMAT_MESSAGE_FROM_SYSTEM.
     */
    public static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000;
    /**
     * The constant FORMAT_MESSAGE_ARGUMENT_ARRAY.
     */
    public static final int FORMAT_MESSAGE_ARGUMENT_ARRAY = 0x00002000;
    /**
     * The constant FORMAT_MESSAGE_MAX_WIDTH_MASK.
     */
    public static final int FORMAT_MESSAGE_MAX_WIDTH_MASK = 0x000000FF;

    /**
     * The constant LANG_NEUTRAL.
     */
    public static final int LANG_NEUTRAL = 0x00;
    /**
     * The constant SUBLANG_DEFAULT.
     */
    public static final int SUBLANG_DEFAULT = 0x01;

    /**
     * Makelangid int.
     *
     * @param p the p
     * @param s the s
     * @return the int
     */
    public static int MAKELANGID(int p, int s) {
        return (s << 10) | p;
    }

    /**
     * The type Ulong ptr.
     */
    public static class ULONG_PTR extends IntegerType {

        /**
         * Instantiates a new Ulong ptr.
         */
        public ULONG_PTR() {
            this(0);
        }

        /**
         * Instantiates a new Ulong ptr.
         *
         * @param value the value
         */
        public ULONG_PTR(long value) {
            super(Pointer.SIZE, value);
        }
    }

    /**
     * Represent the Windows API struct OVERLAPPED. The constructor of this
     * class does 'this.setAutoSynch(false)' because instances of this class
     * should not be auto synchronized nor written as a whole, because Windows
     * stores pointers to the actual memory representing this this struct and
     * modifies it outside the function calls and copying (writing) the Java
     * class fields to the actual memory will destroy those structures.
     * <p>
     * <p>
     * To set the fields it recommend to use the 'writeField(String,Object)'. It
     * is ok to read those fields of OVERLAPPED using Java dot-notatio. that
     * have been written by Java code, but those field that Windows modifies
     * should be accessed using 'readField(String)' or by invoking 'read()' on
     * the object before accessing the fields with the java dot-notation.
     * <p>
     * For example this is acceptable usage for doing overlapped I/O (except
     * this code does no error checking!):
     * <p>
     * <pre>
     * <code>
     *  OVERLAPPED ovl = new OVERLAPPED();
     *  ovl.writeField("hEvent", CreateEvent(null, true, false, null));
     *  ResetEvent(osReader.hEvent);
     *  ReadFile(hComm, buffer, reqN, recN, ovl);
     * </code>
     * </pre>
     *
     * @author nyholku
     */
    public static class OVERLAPPED extends Structure {

        /**
         * The Internal.
         */
        public ULONG_PTR Internal;
        /**
         * The Internal high.
         */
        public ULONG_PTR InternalHigh;
        /**
         * The Offset.
         */
        public int Offset;
        /**
         * The Offset high.
         */
        public int OffsetHigh;
        /**
         * The H event.
         */
        public HANDLE hEvent;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("Internal",//
                    "InternalHigh",//
                    "Offset",//
                    "OffsetHigh",//
                    "hEvent"//
            );
        }

        /**
         * Instantiates a new Overlapped.
         */
        public OVERLAPPED() {
            setAutoSynch(false);
        }

        public String toString() {
            return String.format(//
                    "[Offset %d OffsetHigh %d hEvent %s]",//
                    Offset, OffsetHigh, hEvent.toString());
        }
    }

    /**
     * The type Security attributes.
     */
    public static class SECURITY_ATTRIBUTES extends Structure {

        /**
         * The N length.
         */
        public int nLength;
        /**
         * The Lp security descriptor.
         */
        public Pointer lpSecurityDescriptor;
        /**
         * The B inherit handle.
         */
        public boolean bInheritHandle;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("nLength",//
                    "lpSecurityDescriptor",//
                    "bInheritHandle"//
            );
        }
    }

    /**
     * The type Dcb.
     */
    public static class DCB extends Structure {

        /**
         * The Dc blength.
         */
        public int DCBlength;
        /**
         * The Baud rate.
         */
        public int BaudRate;
        /**
         * The F flags.
         */
        public int fFlags; // No bit field mapping in JNA so define a flags field and masks for fFlags
        /**
         * The constant fBinary.
         */
        public static final int fBinary = 0x00000001;
        /**
         * The constant fParity.
         */
        public static final int fParity = 0x00000002;
        /**
         * The constant fOutxCtsFlow.
         */
        public static final int fOutxCtsFlow = 0x00000004;
        /**
         * The constant fOutxDsrFlow.
         */
        public static final int fOutxDsrFlow = 0x00000008;
        /**
         * The constant fDtrControl.
         */
        public static final int fDtrControl = 0x00000030;
        /**
         * The constant fDsrSensitivity.
         */
        public static final int fDsrSensitivity = 0x00000040;
        /**
         * The constant fTXContinueOnXoff.
         */
        public static final int fTXContinueOnXoff = 0x00000080;
        /**
         * The constant fOutX.
         */
        public static final int fOutX = 0x00000100;
        /**
         * The constant fInX.
         */
        public static final int fInX = 0x00000200;
        /**
         * The constant fErrorChar.
         */
        public static final int fErrorChar = 0x00000400;
        /**
         * The constant fNull.
         */
        public static final int fNull = 0x00000800;
        /**
         * The constant fRtsControl.
         */
        public static final int fRtsControl = 0x00003000;
        /**
         * The constant fAbortOnError.
         */
        public static final int fAbortOnError = 0x00004000;
        /**
         * The constant fDummy2.
         */
        public static final int fDummy2 = 0xFFFF8000;
        /**
         * The W reserved.
         */
        public short wReserved;
        /**
         * The Xon lim.
         */
        public short XonLim;
        /**
         * The Xoff lim.
         */
        public short XoffLim;
        /**
         * The Byte size.
         */
        public byte ByteSize;
        /**
         * The Parity.
         */
        public byte Parity;
        /**
         * The Stop bits.
         */
        public byte StopBits;
        /**
         * The Xon char.
         */
        public byte XonChar;
        /**
         * The Xoff char.
         */
        public byte XoffChar;
        /**
         * The Error char.
         */
        public byte ErrorChar;
        /**
         * The Eof char.
         */
        public byte EofChar;
        /**
         * The Evt char.
         */
        public byte EvtChar;
        /**
         * The W reserved 1.
         */
        public short wReserved1;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("DCBlength",//
                    "BaudRate",//
                    "fFlags",//
                    "wReserved",//
                    "XonLim",//
                    "XoffLim",//
                    "ByteSize",//
                    "Parity",//
                    "StopBits",//
                    "XonChar",//
                    "XoffChar",//
                    "ErrorChar",//
                    "EofChar",//
                    "EvtChar",//
                    "wReserved1"//
            );
        }

        public String toString() {
            return String.format(//
                    "[BaudRate %d fFlags %04X wReserved %d XonLim %d XoffLim %d ByteSize %d Parity %d StopBits %d XonChar %02X XoffChar %02X ErrorChar %02X EofChar %02X EvtChar %02X wReserved1 %d]", //
                    BaudRate, fFlags, wReserved, XonLim, XoffLim, ByteSize, Parity, StopBits, XonChar, XoffChar, ErrorChar, EofChar, EvtChar, wReserved1);
        }

    };

    /**
     * The type Commtimeouts.
     */
    public static class COMMTIMEOUTS extends Structure {

        /**
         * The Read interval timeout.
         */
        public int ReadIntervalTimeout;
        /**
         * The Read total timeout multiplier.
         */
        public int ReadTotalTimeoutMultiplier;
        /**
         * The Read total timeout constant.
         */
        public int ReadTotalTimeoutConstant;
        /**
         * The Write total timeout multiplier.
         */
        public int WriteTotalTimeoutMultiplier;
        /**
         * The Write total timeout constant.
         */
        public int WriteTotalTimeoutConstant;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("ReadIntervalTimeout",//
                    "ReadTotalTimeoutMultiplier",//
                    "ReadTotalTimeoutConstant",//
                    "WriteTotalTimeoutMultiplier",//
                    "WriteTotalTimeoutConstant"//
            );
        }

        public String toString() {
            return String.format(//
                    "[ReadIntervalTimeout %d ReadTotalTimeoutMultiplier %d ReadTotalTimeoutConstant %d WriteTotalTimeoutMultiplier %d WriteTotalTimeoutConstant %d]", //
                    ReadIntervalTimeout, ReadTotalTimeoutMultiplier, ReadTotalTimeoutConstant, WriteTotalTimeoutMultiplier, WriteTotalTimeoutConstant);
        }

    };

    /**
     * The type Comstat.
     */
    public static class COMSTAT extends Structure {

        /**
         * The F flags.
         */
        public int fFlags;
        /**
         * The constant fCtsHold.
         */
        public static final int fCtsHold = 0x00000001;
        /**
         * The constant fDsrHold.
         */
        public static final int fDsrHold = 0x00000002;
        /**
         * The constant fRlsdHold.
         */
        public static final int fRlsdHold = 0x00000004;
        /**
         * The constant fXoffHold.
         */
        public static final int fXoffHold = 0x00000008;
        /**
         * The constant fXoffSent.
         */
        public static final int fXoffSent = 0x00000010;
        /**
         * The constant fEof.
         */
        public static final int fEof = 0x00000020;
        /**
         * The constant fTxim.
         */
        public static final int fTxim = 0x00000040;
        /**
         * The constant fReserved.
         */
        public static final int fReserved = 0xFFFFFF80;
        /**
         * The Cb in que.
         */
        public int cbInQue;
        /**
         * The Cb out que.
         */
        public int cbOutQue;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("fFlags",//
                    "cbInQue",//
                    "cbOutQue"//
            );
        }

        public String toString() {
            return String.format("[fFlags %04X cbInQue %d cbInQue %d]", fFlags, cbInQue, cbOutQue);
        }
    };

    /**
     * Create file handle.
     *
     * @param name     the name
     * @param access   the access
     * @param sharing  the sharing
     * @param security the security
     * @param create   the create
     * @param attribs  the attribs
     * @param template the template
     * @return the handle
     */
    static public HANDLE CreateFile(String name, int access, int sharing, SECURITY_ATTRIBUTES security, int create, int attribs, Pointer template) {
        log = log && log(5, "> CreateFileA(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s)\n", name, access, sharing, security, create, attribs, template);
        HANDLE h;
        try {
            h = m_K32lib.CreateFile(name, access, sharing, security, create, attribs, template);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            h = INVALID_HANDLE_VALUE;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< CreateFileA(%s, 0x%08X, 0x%08X, %s, 0x%08X, 0x%08X,%s) => %s\n", name, access, sharing, security, create, attribs, template, h);
        return h;
    }

    /**
     * Write file boolean.
     *
     * @param hFile the h file
     * @param buf   the buf
     * @param wrn   the wrn
     * @param nwrtn the nwrtn
     * @return the boolean
     */
    // This is for synchronous writes only
    static public boolean WriteFile(HANDLE hFile, byte[] buf, int wrn, int[] nwrtn) {
        log = log && log(5, "> WriteFile(%s, %s, %d, [%d])\n", hFile, log(buf, wrn), wrn, nwrtn[0]);
        boolean res;
        try {
            res = m_K32lib.WriteFile(hFile, buf, wrn, nwrtn, null);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< WriteFile(%s, %s, %d, [%d]) => %s\n", hFile, log(buf, wrn), wrn, nwrtn[0], res);
        return res;
    }

    /**
     * Write file boolean.
     *
     * @param hFile the h file
     * @param buf   the buf
     * @param wrn   the wrn
     * @param nwrtn the nwrtn
     * @param ovrlp the ovrlp
     * @return the boolean
     */
    // This can be used with synchronous as well as overlapped writes
    static public boolean WriteFile(HANDLE hFile, Pointer buf, int wrn, int[] nwrtn, OVERLAPPED ovrlp) {
        log = log && log(5, "> WriteFile(%s, %s, %d, [%d], %s)\n", hFile, log(buf.getByteArray(0, wrn), 5), wrn, nwrtn[0], ref(ovrlp));
        boolean res;
        try {
            res = m_K32lib.WriteFile(hFile, buf, wrn, nwrtn, ovrlp);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< WriteFile(%s, %s, %d, [%d], %s) => %s\n", hFile, log(buf.getByteArray(0, wrn), 5), wrn, nwrtn[0], ref(ovrlp), res);
        return res;
    }

    /**
     * Read file boolean.
     *
     * @param hFile the h file
     * @param buf   the buf
     * @param rdn   the rdn
     * @param nrd   the nrd
     * @return the boolean
     */
    // This is for synchronous reads only
    static public boolean ReadFile(HANDLE hFile, byte[] buf, int rdn, int[] nrd) {
        log = log && log(5, "> ReadFile(%s, %s, %d, [%d])\n", hFile, log(buf, rdn), rdn, nrd[0]);
        boolean res;
        try {
            res = m_K32lib.ReadFile(hFile, buf, rdn, nrd, null);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< ReadFile(%s, %s, %d, [%d]) => %s\n", hFile, log(buf, rdn), rdn, nrd[0], res);
        return res;
    }

    /**
     * Read file boolean.
     *
     * @param hFile the h file
     * @param buf   the buf
     * @param rdn   the rdn
     * @param nrd   the nrd
     * @param ovrlp the ovrlp
     * @return the boolean
     */
    // This can be used with synchronous as well as overlapped reads
    static public boolean ReadFile(HANDLE hFile, Pointer buf, int rdn, int[] nrd, OVERLAPPED ovrlp) {
        log = log && log(5, "> ReadFile(%s, %s, %d, [%d], %s)\n", hFile, log(buf.getByteArray(0, rdn), 5), rdn, nrd[0], ref(ovrlp));
        boolean res;
        try {
            res = m_K32lib.ReadFile(hFile, buf, rdn, nrd, ovrlp);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< ReadFile(%s, %s, %d, [%d], %s) => %s\n", hFile, log(buf.getByteArray(0, rdn), 5), rdn, nrd[0], ref(ovrlp), res);
        return res;
    }

    /**
     * Flush file buffers boolean.
     *
     * @param hFile the h file
     * @return the boolean
     */
    static public boolean FlushFileBuffers(HANDLE hFile) {
        log = log && log(5, "> FlushFileBuffers(%s)\n", hFile);
        boolean res;
        try {
            res = m_K32lib.FlushFileBuffers(hFile);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< FlushFileBuffers(%s) => %s\n", hFile, res);
        return res;
    }

    /**
     * Purge comm boolean.
     *
     * @param hFile the h file
     * @param qmask the qmask
     * @return the boolean
     */
    static public boolean PurgeComm(HANDLE hFile, int qmask) {
        log = log && log(5, "> PurgeComm(%s,0x%08X)\n", hFile, qmask);
        boolean res;
        try {
            res = m_K32lib.PurgeComm(hFile, qmask);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< PurgeComm(%s,0x%08X) => %s\n", hFile, qmask, res);
        return res;
    }

    /**
     * Cancel io boolean.
     *
     * @param hFile the h file
     * @return the boolean
     */
    static public boolean CancelIo(HANDLE hFile) {
        log = log && log(5, "> CancelIo(%s)\n", hFile);
        boolean res;
        try {
            res = m_K32lib.CancelIo(hFile);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< CancelIo(%s) => %s\n", hFile, res);
        return res;
    }

    /**
     * Close handle boolean.
     *
     * @param hFile the h file
     * @return the boolean
     */
    static public boolean CloseHandle(HANDLE hFile) {
        log = log && log(5, "> CloseHandle(%s)\n", hFile);
        boolean res;
        try {
            res = m_K32lib.CloseHandle(hFile);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< CloseHandle(%s) => %s\n", hFile, res);
        return res;
    }

    /**
     * Clear comm error boolean.
     *
     * @param hFile the h file
     * @param n     the n
     * @param s     the s
     * @return the boolean
     */
    static public boolean ClearCommError(HANDLE hFile, int[] n, COMSTAT s) {
        log = log && log(5, "> ClearCommError(%s, [%d], %s)\n", hFile, n[0], s);
        boolean res;
        try {
            res = m_K32lib.ClearCommError(hFile, n, s);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< ClearCommError(%s, [%d], %s) => %s\n", hFile, n[0], s, res);
        return res;
    }

    /**
     * Set comm mask boolean.
     *
     * @param hFile the h file
     * @param mask  the mask
     * @return the boolean
     */
    static public boolean SetCommMask(HANDLE hFile, int mask) {
        log = log && log(5, "> SetCommMask(%s, 0x%08X)\n", hFile, mask);
        boolean res;
        try {
            res = m_K32lib.SetCommMask(hFile, mask);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetCommMask(%s, 0x%08X) => %s\n", hFile, mask, res);
        return res;
    }

    /**
     * Get comm mask boolean.
     *
     * @param hFile the h file
     * @param mask  the mask
     * @return the boolean
     */
    static public boolean GetCommMask(HANDLE hFile, int[] mask) {
        log = log && log(5, "> GetCommMask(%s, [0x%08X])\n", hFile, mask[0]);
        boolean res;
        try {
            res = m_K32lib.GetCommMask(hFile, mask);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< GetCommMask(%s, [0x%08X]) => %s\n", hFile, mask[0], res);
        return res;
    }

    /**
     * Get comm state boolean.
     *
     * @param hFile the h file
     * @param dcb   the dcb
     * @return the boolean
     */
    static public boolean GetCommState(HANDLE hFile, DCB dcb) {
        log = log && log(5, "> GetCommState(%s, %s)\n", hFile, dcb);
        boolean res;
        try {
            res = m_K32lib.GetCommState(hFile, dcb);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< GetCommState(%s, %s) => %s\n", hFile, dcb, res);
        return res;
    }

    /**
     * Set comm state boolean.
     *
     * @param hFile the h file
     * @param dcb   the dcb
     * @return the boolean
     */
    static public boolean SetCommState(HANDLE hFile, DCB dcb) {
        log = log && log(5, "> SetCommState(%s, %s)\n", hFile, dcb);
        boolean res;
        try {
            res = m_K32lib.SetCommState(hFile, dcb);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetCommState(%s, %s) => %s\n", hFile, dcb, res);
        return res;
    }

    /**
     * Set comm timeouts boolean.
     *
     * @param hFile the h file
     * @param touts the touts
     * @return the boolean
     */
    static public boolean SetCommTimeouts(HANDLE hFile, COMMTIMEOUTS touts) {
        log = log && log(5, "> SetCommTimeouts(%s, %s)\n", hFile, touts);
        boolean res;
        try {
            res = m_K32lib.SetCommTimeouts(hFile, touts);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetCommTimeouts(%s, %s) => %s\n", hFile, touts, res);
        return res;
    }

    /**
     * Setup comm boolean.
     *
     * @param hFile      the h file
     * @param inQueueSz  the in queue sz
     * @param outQueueSz the out queue sz
     * @return the boolean
     */
    static public boolean SetupComm(HANDLE hFile, int inQueueSz, int outQueueSz) {
        log = log && log(5, "> SetCommTimeouts(%s, %d, %d)\n", hFile, inQueueSz, outQueueSz);
        boolean res;
        try {
            res = m_K32lib.SetupComm(hFile, inQueueSz, outQueueSz);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetCommTimeouts(%s, %d, %d) => %s\n", hFile, inQueueSz, outQueueSz, res);
        return res;
    }

    /**
     * Set comm break boolean.
     *
     * @param hFile the h file
     * @return the boolean
     */
    static public boolean SetCommBreak(HANDLE hFile) {
        log = log && log(5, "> SetCommBreak(%s)\n", hFile);
        boolean res;
        try {
            res = m_K32lib.SetCommBreak(hFile);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetCommBreak(%s) => %s\n", hFile, res);
        return res;
    }

    /**
     * Clear comm break boolean.
     *
     * @param hFile the h file
     * @return the boolean
     */
    static public boolean ClearCommBreak(HANDLE hFile) {
        log = log && log(5, "> ClearCommBreak(%s)\n", hFile);
        boolean res;
        try {
            res = m_K32lib.ClearCommBreak(hFile);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< ClearCommBreak(%s) => %s\n", hFile, res);
        return res;
    }

    /**
     * Get comm modem status boolean.
     *
     * @param hFile the h file
     * @param stat  the stat
     * @return the boolean
     */
    static public boolean GetCommModemStatus(HANDLE hFile, int[] stat) {
        log = log && log(5, "> GetCommModemStatus(%s,0x%08X)\n", hFile, stat[0]);
        boolean res;
        try {
            res = m_K32lib.GetCommModemStatus(hFile, stat);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< GetCommModemStatus(%s,0x%08X) => %s\n", hFile, stat[0], res);
        return res;
    }

    /**
     * Escape comm function boolean.
     *
     * @param hFile the h file
     * @param func  the func
     * @return the boolean
     */
    static public boolean EscapeCommFunction(HANDLE hFile, int func) {
        log = log && log(5, "> EscapeCommFunction(%s,0x%08X)\n", hFile, func);
        boolean res;
        try {
            res = m_K32lib.EscapeCommFunction(hFile, func);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< EscapeCommFunction(%s,0x%08X) => %s\n", hFile, func, res);
        return res;
    }

    /**
     * Create event handle.
     *
     * @param security the security
     * @param manual   the manual
     * @param initial  the initial
     * @param name     the name
     * @return the handle
     */
    static public HANDLE CreateEvent(SECURITY_ATTRIBUTES security, boolean manual, boolean initial, String name) {
        log = log && log(5, "> CreateEventA(%s, %s, %s, %s)\n", ref(security), manual, initial, name);
        HANDLE h;
        try {
            h = m_K32lib.CreateEvent(security, manual, initial, name);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            h = INVALID_HANDLE_VALUE;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< CreateEventA(%s, %s, %s, %s) => %s\n", ref(security), manual, initial, name, h);
        return h;
    }

    /**
     * Set event boolean.
     *
     * @param hEvent the h event
     * @return the boolean
     */
    static public boolean SetEvent(HANDLE hEvent) {
        log = log && log(5, "> SetEvent(%s)\n", hEvent);
        boolean res;
        try {
            res = m_K32lib.SetEvent(hEvent);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< SetEvent(%s) => %s\n", hEvent, res);
        return res;
    }

    /**
     * Reset event boolean.
     *
     * @param hEvent the h event
     * @return the boolean
     */
    static public boolean ResetEvent(HANDLE hEvent) {
        log = log && log(5, "> ResetEvent(%s)\n", hEvent);
        boolean res;
        try {
            res = m_K32lib.ResetEvent(hEvent);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< ResetEvent(%s) => %s\n", hEvent, res);
        return res;
    }

    /**
     * Wait comm event boolean.
     *
     * @param hFile     the h file
     * @param lpEvtMask the lp evt mask
     * @param ovl       the ovl
     * @return the boolean
     */
    static public boolean WaitCommEvent(HANDLE hFile, IntByReference lpEvtMask, OVERLAPPED ovl) {
        log = log && log(5, "> WaitCommEvent(%s, [%d], %s)\n", hFile, lpEvtMask.getValue(), ref(ovl));
        boolean res;
        try {
            res = m_K32lib.WaitCommEvent(hFile, lpEvtMask, ovl);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask.getValue(), ref(ovl), res);
        return res;
    }

    /**
     * Wait comm event boolean.
     *
     * @param hFile     the h file
     * @param lpEvtMask the lp evt mask
     * @return the boolean
     */
    static public boolean WaitCommEvent(HANDLE hFile, int[] lpEvtMask) {
        log = log && log(5, "> WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask[0], null);
        IntByReference brlpEvtMask = new IntByReference(lpEvtMask[0]);
        boolean res;
        try {
            res = m_K32lib.WaitCommEvent(hFile, brlpEvtMask, null);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        lpEvtMask[0] = brlpEvtMask.getValue();
        log = log && log(4, "< WaitCommEvent(%s, [%d], %s) => %s\n", hFile, lpEvtMask[0], null, res);
        return res;
    }

    /**
     * Wait for single object int.
     *
     * @param hHandle        the h handle
     * @param dwMilliseconds the dw milliseconds
     * @return the int
     */
    static public int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds) {
        log = log && log(5, "> WaitForSingleObject(%s, %d)\n", hHandle, dwMilliseconds);
        int res = m_K32lib.WaitForSingleObject(hHandle, dwMilliseconds);
        log = log && log(4, "< WaitForSingleObject(%s, %d) => %s\n", hHandle, dwMilliseconds, res);
        return res;
    }

    /**
     * Wait for multiple objects int.
     *
     * @param nCount         the n count
     * @param lpHandles      the lp handles
     * @param bWaitAll       the b wait all
     * @param dwMilliseconds the dw milliseconds
     * @return the int
     */
    static public int WaitForMultipleObjects(int nCount, HANDLE[] lpHandles, boolean bWaitAll, int dwMilliseconds) {
        log = log && log(5, "> WaitForMultipleObjects(%d, %s, %s, %d)\n", nCount, log(lpHandles, 3), bWaitAll, dwMilliseconds);
        int res = m_K32libWM.WaitForMultipleObjects(nCount, lpHandles, bWaitAll, dwMilliseconds);
        log = log && log(4, "< WaitForMultipleObjects(%d, %s, %s, %d) => %s\n", nCount, log(lpHandles, 3), bWaitAll, dwMilliseconds, res);
        return res;
    }

    /**
     * Get overlapped result boolean.
     *
     * @param hFile the h file
     * @param ovl   the ovl
     * @param ntfrd the ntfrd
     * @param wait  the wait
     * @return the boolean
     */
    static public boolean GetOverlappedResult(HANDLE hFile, OVERLAPPED ovl, int[] ntfrd, boolean wait) {
        log = log && log(5, "> GetOverlappedResult(%s, %s, [%d], %s)\n", hFile, ref(ovl), ntfrd[0], wait);
        boolean res;
        try {
            res = m_K32lib.GetOverlappedResult(hFile, ovl, ntfrd, wait);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = false;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< GetOverlappedResult(%s, %s, [%d], %s) => %s\n", hFile, ref(ovl), ntfrd[0], wait, res);
        return res;
    }

    /**
     * Get last error int.
     *
     * @return the int
     */
    static public int GetLastError() {
        log = log && log(5, "> GetLastError()\n");
        int res = LastError.get()[0]; // This prevents multiple retrying to create this function per JNA rules
        log = log && log(4, "< GetLastError() => %d\n", res);
        return res;
    }

    /**
     * Format message w int.
     *
     * @param flags   the flags
     * @param src     the src
     * @param msgId   the msg id
     * @param langId  the lang id
     * @param dst     the dst
     * @param sze     the sze
     * @param va_list the va list
     * @return the int
     */
    static public int FormatMessageW(int flags, Pointer src, int msgId, int langId, Pointer dst, int sze, Pointer va_list) {
        log = log && log(5, "> FormatMessageW(%08x, %08x, %d, %d, %s, %d, %s)\n", flags, src, msgId, langId, dst, sze, va_list);
        int res = m_K32lib.FormatMessageW(flags, src, msgId, langId, dst, sze, va_list);
        log = log && log(4, "< FormatMessageW(%08x, %08x, %d, %d, %s, %d, %s) => %d\n", flags, src, msgId, langId, dst, sze, va_list, res);
        return res;
    }

    /**
     * Query dos device int.
     *
     * @param name   the name
     * @param buffer the buffer
     * @param bsize  the bsize
     * @return the int
     */
    static public int QueryDosDevice(String name, byte[] buffer, int bsize) {
        log = log && log(5, "> QueryDosDeviceA(%s, %s, %d)\n", name, buffer, bsize);
        int res;
        try {
            res = m_K32lib.QueryDosDevice(name, buffer, bsize);
            LastError.get()[0] = 0;
        } catch (LastErrorException le) {
            res = 0;
            LastError.get()[0] = le.getErrorCode();
        }
        log = log && log(4, "< QueryDosDeviceA(%s, %s, %d) => %d\n", name, buffer, bsize, res);
        return res;
    }

}
