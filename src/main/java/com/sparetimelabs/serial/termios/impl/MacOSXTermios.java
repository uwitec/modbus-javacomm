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

import com.sun.jna.*;
import java.io.File;

import java.util.*;
import java.util.regex.Pattern;

import com.sparetimelabs.serial.termios.JTermios;
import com.sparetimelabs.serial.termios.Termios;
import com.sparetimelabs.serial.termios.TimeVal;
import static com.sparetimelabs.serial.termios.JTermios.*;
import static com.sparetimelabs.serial.termios.JTermios.JTermiosLogging.log;

/**
 * The type Mac osx termios.
 */
public class MacOSXTermios implements JTermiosInterface {

    private static int IOSSIOSPEED = 0x80045402;
    private static String DEVICE_DIR_PATH = "/dev/";
    /**
     * The M clib dm.
     */
    static C_lib_DirectMapping m_ClibDM;
    /**
     * The M clib.
     */
    static C_lib m_Clib;
//	static NonDirectCLib m_ClibND;

    static {
        Native.setPreserveLastError(true);
//		m_ClibND = (NonDirectCLib) Native.loadLibrary(Platform.C_LIBRARY_NAME, NonDirectCLib.class);
        Native.register(C_lib_DirectMapping.class, NativeLibrary.getInstance(Platform.C_LIBRARY_NAME));
        m_ClibDM = new C_lib_DirectMapping();
        m_Clib = m_ClibDM;
    }

    /**
     * The type C lib direct mapping.
     */
    public static class C_lib_DirectMapping implements C_lib {

        native public int pipe(int[] fds);

        native public int tcdrain(int fd);

        native public void cfmakeraw(termios termios);

        native public int fcntl(int fd, int cmd, int arg);

        /**
         * Ioctl int.
         *
         * @param fd  the fd
         * @param cmd the cmd
         * @return the int
         */
        native public int ioctl(int fd, NativeLong cmd);

        /**
         * Ioctl int.
         *
         * @param fd  the fd
         * @param cmd the cmd
         * @param arg the arg
         * @return the int
         */
        native public int ioctl(int fd, NativeLong cmd, int arg);

        native public int ioctl(int fd, NativeLong cmd, int[] arg);

        native public int open(String path, int flags);

        native public int close(int fd);

        native public int tcgetattr(int fd, termios termios);

        native public int tcsetattr(int fd, int cmd, termios termios);

        native public int cfsetispeed(termios termios, NativeLong i);

        native public int cfsetospeed(termios termios, NativeLong i);

        native public NativeLong cfgetispeed(termios termios);

        native public NativeLong cfgetospeed(termios termios);

        native public NativeSize write(int fd, byte[] buffer, NativeSize count);

        native public NativeSize read(int fd, byte[] buffer, NativeSize count);

        native public int tcflush(int fd, int qs);

        native public void perror(String msg);

        native public int tcsendbreak(int fd, int duration);

        native public int select(int n, fd_set read, fd_set write, fd_set error, timeval timeout);

    }

    /**
     * The interface C lib.
     */
    public interface C_lib extends Library {

        /**
         * Pipe int.
         *
         * @param fds the fds
         * @return the int
         */
        public int pipe(int[] fds);

        /**
         * Tcdrain int.
         *
         * @param fd the fd
         * @return the int
         */
        public int tcdrain(int fd);

        /**
         * Cfmakeraw.
         *
         * @param termios the termios
         */
        public void cfmakeraw(termios termios);

        /**
         * Fcntl int.
         *
         * @param fd  the fd
         * @param cmd the cmd
         * @param arg the arg
         * @return the int
         */
        public int fcntl(int fd, int cmd, int arg);

        /**
         * Ioctl int.
         *
         * @param fd  the fd
         * @param cmd the cmd
         * @param arg the arg
         * @return the int
         */
        public int ioctl(int fd, NativeLong cmd, int[] arg);

        /**
         * Open int.
         *
         * @param path  the path
         * @param flags the flags
         * @return the int
         */
        public int open(String path, int flags);

        /**
         * Close int.
         *
         * @param fd the fd
         * @return the int
         */
        public int close(int fd);

        /**
         * Tcgetattr int.
         *
         * @param fd      the fd
         * @param termios the termios
         * @return the int
         */
        public int tcgetattr(int fd, termios termios);

        /**
         * Tcsetattr int.
         *
         * @param fd      the fd
         * @param cmd     the cmd
         * @param termios the termios
         * @return the int
         */
        public int tcsetattr(int fd, int cmd, termios termios);

        /**
         * Cfsetispeed int.
         *
         * @param termios the termios
         * @param i       the
         * @return the int
         */
        public int cfsetispeed(termios termios, NativeLong i);

        /**
         * Cfsetospeed int.
         *
         * @param termios the termios
         * @param i       the
         * @return the int
         */
        public int cfsetospeed(termios termios, NativeLong i);

        /**
         * Cfgetispeed native long.
         *
         * @param termios the termios
         * @return the native long
         */
        public NativeLong cfgetispeed(termios termios);

        /**
         * Cfgetospeed native long.
         *
         * @param termios the termios
         * @return the native long
         */
        public NativeLong cfgetospeed(termios termios);

        /**
         * Write native size.
         *
         * @param fd     the fd
         * @param buffer the buffer
         * @param count  the count
         * @return the native size
         */
        public NativeSize write(int fd, byte[] buffer, NativeSize count);

        /**
         * Read native size.
         *
         * @param fd     the fd
         * @param buffer the buffer
         * @param count  the count
         * @return the native size
         */
        public NativeSize read(int fd, byte[] buffer, NativeSize count);

        /**
         * Tcflush int.
         *
         * @param fd the fd
         * @param qs the qs
         * @return the int
         */
        public int tcflush(int fd, int qs);

        /**
         * Perror.
         *
         * @param msg the msg
         */
        public void perror(String msg);

        /**
         * Tcsendbreak int.
         *
         * @param fd       the fd
         * @param duration the duration
         * @return the int
         */
        public int tcsendbreak(int fd, int duration);

        /**
         * Select int.
         *
         * @param n       the n
         * @param read    the read
         * @param write   the write
         * @param error   the error
         * @param timeout the timeout
         * @return the int
         */
        public int select(int n, fd_set read, fd_set write, fd_set error, timeval timeout);

    }

    /**
     * The type Timeval.
     */
    //	public interface NonDirectCLib extends com.sun.jna.Library {
//
//		public int ioctl(int fd, NativeLong cmd, NativeLong[] arg);
//
//		//       public int poll(pollfd.ByReference pfds, int nfds, int timeout);
//	}
    static public class timeval extends Structure {

        /**
         * The Tv sec.
         */
        public NativeLong tv_sec;
        /**
         * The Tv usec.
         */
        public NativeLong tv_usec;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(//
                    "tv_sec",//
                    "tv_usec"//
            );
        }

        /**
         * Instantiates a new Timeval.
         *
         * @param timeout the timeout
         */
        public timeval(com.sparetimelabs.serial.termios.TimeVal timeout) {
            tv_sec = new NativeLong(timeout.tv_sec);
            tv_usec = new NativeLong(timeout.tv_usec);
        }
    }

    /**
     * The type Pollfd.
     */
    static public class pollfd extends Structure {

        /**
         * The type By reference.
         */
        public static class ByReference extends pollfd implements Structure.ByReference {
        }

        /**
         * The Fd.
         */
        public int fd;
        /**
         * The Events.
         */
        public short events;
        /**
         * The Revents.
         */
        public short revents;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(//
                    "fd",//
                    "events",//
                    "revents"//
            );
        }

        /**
         * Instantiates a new Pollfd.
         */
        public pollfd() {
        }

        /**
         * Instantiates a new Pollfd.
         *
         * @param pfd the pfd
         */
        public pollfd(Pollfd pfd) {
            fd = pfd.fd;
            events = pfd.events;
            revents = pfd.revents;
        }
    }

    /**
     * The type Fd set.
     */
    static public class fd_set extends Structure implements FDSet {

        private final static int NFBBITS = 32;
        private final static int fd_count = 1024;
        /**
         * The Fd array.
         */
        public int[] fd_array = new int[(fd_count + NFBBITS - 1) / NFBBITS];

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(//
                    "fd_array"//
            );
        }

        public void FD_SET(int fd) {
            fd_array[fd / NFBBITS] |= (1 << (fd % NFBBITS));
        }

        public boolean FD_ISSET(int fd) {
            return (fd_array[fd / NFBBITS] & (1 << (fd % NFBBITS))) != 0;
        }

        public void FD_ZERO() {
            Arrays.fill(fd_array, 0);
        }

        public void FD_CLR(int fd) {
            fd_array[fd / NFBBITS] &= ~(1 << (fd % NFBBITS));
        }

    }

    /**
     * The type Termios.
     */
    static public class termios extends Structure {

        /**
         * The C iflag.
         */
        public NativeLong c_iflag;
        /**
         * The C oflag.
         */
        public NativeLong c_oflag;
        /**
         * The C cflag.
         */
        public NativeLong c_cflag;
        /**
         * The C lflag.
         */
        public NativeLong c_lflag;
        /**
         * The C cc.
         */
        public byte[] c_cc = new byte[20];
        /**
         * The C ispeed.
         */
        public NativeLong c_ispeed;
        /**
         * The C ospeed.
         */
        public NativeLong c_ospeed;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(//
                    "c_iflag",//
                    "c_oflag",//
                    "c_cflag",//
                    "c_lflag",//
                    "c_cc",//
                    "c_ispeed",//
                    "c_ospeed"//
            );
        }

        /**
         * Instantiates a new Termios.
         */
        public termios() {
        }

        /**
         * Instantiates a new Termios.
         *
         * @param t the t
         */
        public termios(com.sparetimelabs.serial.termios.Termios t) {
            c_iflag.setValue(t.c_iflag);
            c_oflag.setValue(t.c_oflag);
            c_cflag.setValue(t.c_cflag);
            c_lflag.setValue(t.c_lflag);
            System.arraycopy(t.c_cc, 0, c_cc, 0, Math.min(t.c_cc.length, c_cc.length));
            c_ispeed.setValue(t.c_ispeed);
            c_ospeed.setValue(t.c_ospeed);
        }

        /**
         * Update.
         *
         * @param t the t
         */
        public void update(com.sparetimelabs.serial.termios.Termios t) {
            t.c_iflag = c_iflag.intValue();
            t.c_oflag = c_oflag.intValue();
            t.c_cflag = c_cflag.intValue();
            t.c_lflag = c_lflag.intValue();
            System.arraycopy(c_cc, 0, t.c_cc, 0, Math.min(t.c_cc.length, c_cc.length));
            t.c_ispeed = c_ispeed.intValue();
            t.c_ospeed = c_ospeed.intValue();
        }
    }

    /**
     * Instantiates a new Mac osx termios.
     */
    public MacOSXTermios() {
        log = log && log(1, "instantiating %s\n", getClass().getCanonicalName());
    }

    public int errno() {
        return Native.getLastError();
    }

    public void cfmakeraw(Termios termios) {
        termios t = new termios(termios);
        m_Clib.cfmakeraw(t);
        t.update(termios);
    }

    public int fcntl(int fd, int cmd, int arg) {
        return m_Clib.fcntl(fd, cmd, arg);
    }

    public int tcdrain(int fd) {
        return m_Clib.tcdrain(fd);
    }

    public int cfgetispeed(Termios termios) {
        return m_Clib.cfgetispeed(new termios(termios)).intValue();
    }

    public int cfgetospeed(Termios termios) {
        return m_Clib.cfgetospeed(new termios(termios)).intValue();
    }

    public int cfsetispeed(Termios termios, int speed) {
        termios t = new termios(termios);
        int ret = m_Clib.cfsetispeed(t, new NativeLong(speed));
        t.update(termios);
        return ret;
    }

    public int cfsetospeed(Termios termios, int speed) {
        termios t = new termios(termios);
        int ret = m_Clib.cfsetospeed(t, new NativeLong(speed));
        t.update(termios);
        return ret;
    }

    public int open(String s, int t) {
        if (s != null && !s.startsWith("/")) {
            s = DEVICE_DIR_PATH + s;
        }
        return m_Clib.open(s, t);
    }

    public int read(int fd, byte[] buffer, int len) {
        return m_Clib.read(fd, buffer, new NativeSize(len)).intValue();
    }

    public int write(int fd, byte[] buffer, int len) {
        return m_Clib.write(fd, buffer, new NativeSize(len)).intValue();
    }

    public int close(int fd) {
        return m_Clib.close(fd);
    }

    public int tcflush(int fd, int b) {
        return m_Clib.tcflush(fd, b);
    }

    public int tcgetattr(int fd, Termios termios) {
        termios t = new termios();
        int ret = m_Clib.tcgetattr(fd, t);
        t.update(termios);
        return ret;
    }

    public void perror(String msg) {
        m_Clib.perror(msg);
    }

    public int tcsendbreak(int fd, int duration) {
        // If duration is not zero, it sends zero-valued bits for duration*N seconds,
        // where N is at least 0.25, and not more than 0.5.
        return m_Clib.tcsendbreak(fd, duration / 250);
    }

    public int tcsetattr(int fd, int cmd, Termios termios) {
        return m_Clib.tcsetattr(fd, cmd, new termios(termios));
    }

    public int select(int nfds, FDSet rfds, FDSet wfds, FDSet efds, TimeVal timeout) {
        timeval tout = null;
        if (timeout != null) {
            tout = new timeval(timeout);
        }

        return m_Clib.select(nfds, (fd_set) rfds, (fd_set) wfds, (fd_set) efds, tout);
    }

    public int poll(Pollfd fds[], int nfds, int timeout) {
        throw new UnsupportedOperationException("Poll not supported");
    }

    public boolean canPoll() {
        return false;
    }

    public FDSet newFDSet() {
        return new fd_set();
    }

    public int ioctl(int fd, int cmd, int... data) {
        // At this time, all ioctl commands we have defined are either no parameter or 4 byte parameter.
        return m_Clib.ioctl(fd, new NativeLong(0xFFFFFFFFL & cmd), data);
    }

    public List<String> getPortList() {
        File dir = new File(DEVICE_DIR_PATH);
        if (!dir.isDirectory()) {
            log = log && log(1, "device directory %s does not exist\n", DEVICE_DIR_PATH);
            return null;
        }
        String[] devs = dir.list();
        LinkedList<String> list = new LinkedList<String>();

        Pattern p = JTermios.getPortNamePattern(this);
        if (devs != null) {
            for (int i = 0; i < devs.length; i++) {
                String s = devs[i];
                if (p.matcher(s).matches()) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    public String getPortNamePattern() {
        return "^(tty\\.|cu\\.).*";
    }

    public void shutDown() {

    }

    public int setspeed(int fd, Termios termios, int speed) {
        int r;
        r = cfsetispeed(termios, speed);
        if (r == 0) {
            r = cfsetospeed(termios, speed);
        }
        if (r == 0) {
            r = tcsetattr(fd, TCSANOW, termios);
        }
        if (r != 0) {
            // Darell Tan had patched RXTX with this sequence, so lets try this
            if (cfsetispeed(termios, B9600) == 0 && cfsetospeed(termios, B9600) == 0 && tcsetattr(fd, TCSANOW, termios) == 0) {
                r = ioctl(fd, IOSSIOSPEED, speed);
            }
        }
        return r;
    }

    public int pipe(int[] fds) {
        return m_Clib.pipe(fds);
    }
}
