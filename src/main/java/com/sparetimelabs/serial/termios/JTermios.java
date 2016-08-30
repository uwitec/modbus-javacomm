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

import com.sparetimelabs.serial.termios.JTermios.JTermiosInterface.Pollfd;
import static com.sparetimelabs.serial.termios.JTermios.JTermiosLogging.*;
import com.sparetimelabs.serial.termios.impl.*;

import java.util.List;
import java.util.regex.Pattern;

import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import java.util.*;

/**
 * JTermios provides a limited cross platform unix termios type interface to
 * serial ports.
 *
 * @author nyholku
 */
public class JTermios {

    /**
     * The constant FIONREAD.
     */
    // Note About the read/write methods and the buffers
    //
    // This library provides read/write(byte[] buffer,int length) without an offset
    // to the buffer. This is because it appears that there is a bug in JNA's use of
    // ByteBuffer.wrap(byte[] buffer,int offset,int length) in that the offset gets
    // ignored. So this needs to be handled in the JTermiosImpl classes
    // or somewhere else. Handling the offset requires a buffer to hold 
    // temporarily the bytes. I deemed that it is better to pass the buck ie burden 
    // to the clients of JTermios as they know better what size of buffer (if any) 
    // is best and because then the implementation of that buffer is in one place, 
    // not in each of the JTermiosImpl classes. In this way Mac OS X (and presumably
    // Linux/Unix) does need not a buffer at all in JTermiosImpl. Windows needs a
    // JNA Memory buffer anyway because of the limitations inherent in using 
    // Overlapped I/O with JNA.
    // The 'constants' here, which are equivalent to the corresponding #defines in C
    // come from Mac OS X 10.6.6 / x86_64 architecture
    // Every implementing class for each architecture needs to initialize them in 
    // their JTermiosImpl constructor. For Windows the termios functionality is
    // totally emulated so jtermios.windows.JTermiosImpl can just use these default values as
    // can obviously jtermios.macosx.JTermiosImpl (at least for x86_64 architecture).
    // Much as we liked these cannot be defined 'final' but should be treated immutable all the same.
    // sys/filio.h stuff
    public static int FIONREAD = 0x4004667F;
    /**
     * The constant O_RDWR.
     */
    // fcntl.h stuff
    public static int O_RDWR = 0x00000002;
    /**
     * The constant O_NONBLOCK.
     */
    public static int O_NONBLOCK = 0x00000004;
    /**
     * The constant O_NOCTTY.
     */
    public static int O_NOCTTY = 0x00020000;
    /**
     * The constant O_NDELAY.
     */
    public static int O_NDELAY = 0x00000004;
    /**
     * The constant O_CREAT.
     */
    public static int O_CREAT = 0x00000200;
    /**
     * The constant F_GETFL.
     */
    public static int F_GETFL = 0x00000003;
    /**
     * The constant F_SETFL.
     */
    public static int F_SETFL = 0x00000004;
    /**
     * The constant EAGAIN.
     */
    // errno.h stuff
    public static int EAGAIN = 35;
    /**
     * The constant EBADF.
     */
    public static int EBADF = 9;
    /**
     * The constant EACCES.
     */
    public static int EACCES = 22;
    /**
     * The constant EEXIST.
     */
    public static int EEXIST = 17;
    /**
     * The constant EINTR.
     */
    public static int EINTR = 4;
    /**
     * The constant EINVAL.
     */
    public static int EINVAL = 22;
    /**
     * The constant EIO.
     */
    public static int EIO = 5;
    /**
     * The constant EISDIR.
     */
    public static int EISDIR = 21;
    /**
     * The constant ELOOP.
     */
    public static int ELOOP = 62;
    /**
     * The constant EMFILE.
     */
    public static int EMFILE = 24;
    /**
     * The constant ENAMETOOLONG.
     */
    public static int ENAMETOOLONG = 63;
    /**
     * The constant ENFILE.
     */
    public static int ENFILE = 23;
    /**
     * The constant ENOENT.
     */
    public static int ENOENT = 2;
    /**
     * The constant ENOSR.
     */
    public static int ENOSR = 98;
    /**
     * The constant ENOSPC.
     */
    public static int ENOSPC = 28;
    /**
     * The constant ENOTDIR.
     */
    public static int ENOTDIR = 20;
    /**
     * The constant ENXIO.
     */
    public static int ENXIO = 6;
    /**
     * The constant EOVERFLOW.
     */
    public static int EOVERFLOW = 84;
    /**
     * The constant EROFS.
     */
    public static int EROFS = 30;
    /**
     * The constant ENOTSUP.
     */
    public static int ENOTSUP = 45;
    /**
     * The constant EBUSY.
     */
    public static int EBUSY = 16;
    /**
     * The constant ENOTTY.
     */
    public static int ENOTTY = 25;
    /**
     * The constant TIOCM_RNG.
     */
    // termios.h stuff
    public static int TIOCM_RNG = 0x00000080;
    /**
     * The constant TIOCM_CAR.
     */
    public static int TIOCM_CAR = 0x00000040;
    /**
     * The constant IGNBRK.
     */
    public static int IGNBRK = 0x00000001;
    /**
     * The constant BRKINT.
     */
    public static int BRKINT = 0x00000002;
    /**
     * The constant IGNPAR.
     */
    public static int IGNPAR = 0x00000004;
    /**
     * The constant PARMRK.
     */
    public static int PARMRK = 0x00000008;
    /**
     * The constant INLCR.
     */
    public static int INLCR = 0x00000040;
    /**
     * The constant IGNCR.
     */
    public static int IGNCR = 0x00000080;
    /**
     * The constant ICRNL.
     */
    public static int ICRNL = 0x00000100;
    /**
     * The constant ECHONL.
     */
    public static int ECHONL = 0x00000010;
    /**
     * The constant IEXTEN.
     */
    public static int IEXTEN = 0x00000400;
    /**
     * The constant CLOCAL.
     */
    public static int CLOCAL = 0x00008000;
    /**
     * The constant OPOST.
     */
    public static int OPOST = 0x00000001;
    /**
     * The constant VSTART.
     */
    public static int VSTART = 0x0000000C;
    /**
     * The constant TCSANOW.
     */
    public static int TCSANOW = 0x00000000;
    /**
     * The constant VSTOP.
     */
    public static int VSTOP = 0x0000000D;
    /**
     * The constant VMIN.
     */
    public static int VMIN = 0x00000010;
    /**
     * The constant VTIME.
     */
    public static int VTIME = 0x00000011;
    /**
     * The constant VEOF.
     */
    public static int VEOF = 0x00000000;
    /**
     * The constant TIOCMGET.
     */
    public static int TIOCMGET = 0x4004746A;
    /**
     * The constant TIOCM_CTS.
     */
    public static int TIOCM_CTS = 0x00000020;
    /**
     * The constant TIOCM_DSR.
     */
    public static int TIOCM_DSR = 0x00000100;
    /**
     * The constant TIOCM_RI.
     */
    public static int TIOCM_RI = 0x00000080;
    /**
     * The constant TIOCM_CD.
     */
    public static int TIOCM_CD = 0x00000040;
    /**
     * The constant TIOCM_DTR.
     */
    public static int TIOCM_DTR = 0x00000002;
    /**
     * The constant TIOCM_RTS.
     */
    public static int TIOCM_RTS = 0x00000004;
    /**
     * The constant ICANON.
     */
    public static int ICANON = 0x00000100;
    /**
     * The constant ECHO.
     */
    public static int ECHO = 0x00000008;
    /**
     * The constant ECHOE.
     */
    public static int ECHOE = 0x00000002;
    /**
     * The constant ISIG.
     */
    public static int ISIG = 0x00000080;
    /**
     * The constant TIOCMSET.
     */
    public static int TIOCMSET = 0x8004746D;
    /**
     * The constant IXON.
     */
    public static int IXON = 0x00000200;
    /**
     * The constant IXOFF.
     */
    public static int IXOFF = 0x00000400;
    /**
     * The constant IXANY.
     */
    public static int IXANY = 0x00000800;
    /**
     * The constant CRTSCTS.
     */
    public static int CRTSCTS = 0x00030000;
    /**
     * The constant TCSADRAIN.
     */
    public static int TCSADRAIN = 0x00000001;
    /**
     * The constant INPCK.
     */
    public static int INPCK = 0x00000010;
    /**
     * The constant ISTRIP.
     */
    public static int ISTRIP = 0x00000020;
    /**
     * The constant CSIZE.
     */
    public static int CSIZE = 0x00000300;
    /**
     * The constant TCIFLUSH.
     */
    public static int TCIFLUSH = 0x00000001;
    /**
     * The constant TCOFLUSH.
     */
    public static int TCOFLUSH = 0x00000002;
    /**
     * The constant TCIOFLUSH.
     */
    public static int TCIOFLUSH = 0x00000003;
    /**
     * The constant CS5.
     */
    public static int CS5 = 0x00000000;
    /**
     * The constant CS6.
     */
    public static int CS6 = 0x00000100;
    /**
     * The constant CS7.
     */
    public static int CS7 = 0x00000200;
    /**
     * The constant CS8.
     */
    public static int CS8 = 0x00000300;
    /**
     * The constant CSTOPB.
     */
    public static int CSTOPB = 0x00000400;
    /**
     * The constant CREAD.
     */
    public static int CREAD = 0x00000800;
    /**
     * The constant PARENB.
     */
    public static int PARENB = 0x00001000;
    /**
     * The constant PARODD.
     */
    public static int PARODD = 0x00002000;
    /**
     * The constant CMSPAR.
     */
    public static int CMSPAR = 010000000000; // Is this standard ? Not available on Mac OS X
    /**
     * The constant B0.
     */
    //public static int CCTS_OFLOW = 0x00010000; // Not linux
    //public static int CRTS_IFLOW = 0x00020000; // Not linux
    //public static int CDTR_IFLOW = 0x00040000; // Not linux
    //public static int CDSR_OFLOW = 0x00080000; // Not linux
    //public static int CCAR_OFLOW = 0x00100000; // Not linux
    public static int B0 = 0;
    /**
     * The constant B50.
     */
    public static int B50 = 50;
    /**
     * The constant B75.
     */
    public static int B75 = 75;
    /**
     * The constant B110.
     */
    public static int B110 = 110;
    /**
     * The constant B134.
     */
    public static int B134 = 134;
    /**
     * The constant B150.
     */
    public static int B150 = 150;
    /**
     * The constant B200.
     */
    public static int B200 = 200;
    /**
     * The constant B300.
     */
    public static int B300 = 300;
    /**
     * The constant B600.
     */
    public static int B600 = 600;
    /**
     * The constant B1200.
     */
    public static int B1200 = 1200;
    /**
     * The constant B1800.
     */
    public static int B1800 = 1800;
    /**
     * The constant B2400.
     */
    public static int B2400 = 2400;
    /**
     * The constant B4800.
     */
    public static int B4800 = 4800;
    /**
     * The constant B9600.
     */
    public static int B9600 = 9600;
    /**
     * The constant B19200.
     */
    public static int B19200 = 19200;
    /**
     * The constant B38400.
     */
    public static int B38400 = 38400;
    /**
     * The constant B7200.
     */
    public static int B7200 = 7200; // Not Linux
    /**
     * The constant B14400.
     */
    public static int B14400 = 14400;// Not Linux
    /**
     * The constant B28800.
     */
    public static int B28800 = 28800;// Not Linux
    /**
     * The constant B57600.
     */
    public static int B57600 = 57600;
    /**
     * The constant B76800.
     */
    public static int B76800 = 76800; // Not Linux
    /**
     * The constant B115200.
     */
    public static int B115200 = 115200;
    /**
     * The constant B230400.
     */
    public static int B230400 = 230400;
    /**
     * The constant POLLIN.
     */
    // poll.h stuff
    public static short POLLIN = 0x0001;
    /**
     * The constant POLLPRI.
     */
    //public static short POLLRDNORM = 0x0040; // Not Linux
    //public static short POLLRDBAND = 0x0080; // Not Linux
    public static short POLLPRI = 0x0002;
    /**
     * The constant POLLOUT.
     */
    public static short POLLOUT = 0x0004;
    /**
     * The constant POLLERR.
     */
    //public static short POLLWRNORM = 0x0004; // Not Linux
    //public static short POLLWRBAND = 0x0100; // Not Linux
    public static short POLLERR = 0x0008;
    /**
     * The constant POLLERR_OUT.
     */
    public static short POLLERR_OUT = 0x0008;
    /**
     * The constant POLLNVAL.
     */
    public static short POLLNVAL = 0x0020;
    /**
     * The constant POLLNVAL_OUT.
     */
    public static int POLLNVAL_OUT = 0x0020;

    /**
     * The constant DC1.
     */
    // misc stuff
    public static int DC1 = 0x11; // Ctrl-Q;
    /**
     * The constant DC3.
     */
    public static int DC3 = 0x13; // Ctrl-S;

    // reference to single arc/os specific implementation
    private static JTermiosInterface m_Termios;

    /**
     * The interface Fd set.
     */
    public interface FDSet {

        /**
         * Fd set.
         *
         * @param fd the fd
         */
        public void FD_SET(int fd);

        /**
         * Fd clr.
         *
         * @param fd the fd
         */
        public void FD_CLR(int fd);

        /**
         * Fd isset boolean.
         *
         * @param fd the fd
         * @return the boolean
         */
        public boolean FD_ISSET(int fd);

        /**
         * Fd zero.
         */
        public void FD_ZERO();
    }

    /**
     * The interface J termios interface.
     */
    public interface JTermiosInterface {

        /**
         * The type Pollfd.
         */
        class Pollfd {

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
        }

        /**
         * The type Native size.
         */
        public static class NativeSize extends IntegerType {

            /**
             *
             */
            private static final long serialVersionUID = 2398288011955445078L;
            /**
             * Size of a size_t integer, in bytes.
             */
            public static int SIZE = Native.SIZE_T_SIZE;//Platform.is64Bit() ? 8 : 4;

            /**
             * Create a zero-valued Size.
             */
            public NativeSize() {
                this(0);
            }

            /**
             * Create a Size with the given value.
             *
             * @param value the value
             */
            public NativeSize(long value) {
                super(SIZE, value);
            }
        }

        /**
         * New fd set fd set.
         *
         * @return the fd set
         */
        public FDSet newFDSet();

        /**
         * Pipe int.
         *
         * @param fds the fds
         * @return the int
         */
        int pipe(int[] fds);

        /**
         * Shut down.
         */
        void shutDown();

        /**
         * Errno int.
         *
         * @return the int
         */
        int errno();

        /**
         * Fcntl int.
         *
         * @param fd  the fd
         * @param cmd the cmd
         * @param arg the arg
         * @return the int
         */
        int fcntl(int fd, int cmd, int arg);

        /**
         * Sets .
         *
         * @param fd      the fd
         * @param termios the termios
         * @param speed   the speed
         * @return the
         */
        int setspeed(int fd, Termios termios, int speed);

        /**
         * Cfgetispeed int.
         *
         * @param termios the termios
         * @return the int
         */
        int cfgetispeed(Termios termios);

        /**
         * Cfgetospeed int.
         *
         * @param termios the termios
         * @return the int
         */
        int cfgetospeed(Termios termios);

        /**
         * Cfsetispeed int.
         *
         * @param termios the termios
         * @param speed   the speed
         * @return the int
         */
        int cfsetispeed(Termios termios, int speed);

        /**
         * Cfsetospeed int.
         *
         * @param termios the termios
         * @param speed   the speed
         * @return the int
         */
        int cfsetospeed(Termios termios, int speed);

        /**
         * Tcflush int.
         *
         * @param fd the fd
         * @param b  the b
         * @return the int
         */
        int tcflush(int fd, int b);

        /**
         * Tcdrain int.
         *
         * @param fd the fd
         * @return the int
         */
        int tcdrain(int fd);

        /**
         * Cfmakeraw.
         *
         * @param termios the termios
         */
        void cfmakeraw(Termios termios);

        /**
         * Tcgetattr int.
         *
         * @param fd      the fd
         * @param termios the termios
         * @return the int
         */
        int tcgetattr(int fd, Termios termios);

        /**
         * Tcsetattr int.
         *
         * @param fd      the fd
         * @param cmd     the cmd
         * @param termios the termios
         * @return the int
         */
        int tcsetattr(int fd, int cmd, Termios termios);

        /**
         * Tcsendbreak int.
         *
         * @param fd       the fd
         * @param duration the duration
         * @return the int
         */
        int tcsendbreak(int fd, int duration);

        /**
         * Open int.
         *
         * @param s the s
         * @param t the t
         * @return the int
         */
        int open(String s, int t);

        /**
         * Close int.
         *
         * @param fd the fd
         * @return the int
         */
        int close(int fd);

        /**
         * Write int.
         *
         * @param fd     the fd
         * @param buffer the buffer
         * @param len    the len
         * @return the int
         */
        int write(int fd, byte[] buffer, int len);

        /**
         * Read int.
         *
         * @param fd     the fd
         * @param buffer the buffer
         * @param len    the len
         * @return the int
         */
        int read(int fd, byte[] buffer, int len);

        /**
         * Ioctl int.
         *
         * @param fd   the fd
         * @param cmd  the cmd
         * @param data the data
         * @return the int
         */
        int ioctl(int fd, int cmd, int... data);

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
        int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout);

        /**
         * Poll int.
         *
         * @param fds     the fds
         * @param nfds    the nfds
         * @param timeout the timeout
         * @return the int
         */
        int poll(Pollfd[] fds, int nfds, int timeout);

        /**
         * poll() on Windows has not been implemented and while implemented on
         * Mac OS X, does not work for devices.
         *
         * @return the boolean
         */
        boolean canPoll();

        /**
         * Perror.
         *
         * @param msg the msg
         */
        void perror(String msg);

        /**
         * Gets port list.
         *
         * @return the port list
         */
        List<String> getPortList();

        /**
         * Gets port name pattern.
         *
         * @return the port name pattern
         */
        public String getPortNamePattern();

    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        if (m_Termios != null) {
            m_Termios.shutDown();
        }
    }

    static { // INSTANTIATION 
        if (Platform.isMac()) {
            m_Termios = new MacOSXTermios();
        } else if (Platform.isWindows()) {
            m_Termios = new WindowsTermios();
        } else if (Platform.isLinux()) {
            m_Termios = new LinuxTermios();
        } else if (Platform.isSolaris()) {
            m_Termios = new SolarisTermios();
        } else if (Platform.isFreeBSD()) {
            m_Termios = new FreeBSDTermios();
        } else {
            log(0, "JTermios has no support for OS %s\n", System.getProperty("os.name"));
        }
    }

    /**
     * Errno int.
     *
     * @return the int
     */
    static public int errno() {
        log = log && log(5, "> errno()\n");
        int ret = m_Termios.errno();
        log = log && log(3, "< errno() => %d\n", ret);
        return ret;
    }

    /**
     * Fcntl int.
     *
     * @param fd  the fd
     * @param cmd the cmd
     * @param arg the arg
     * @return the int
     */
    static public int fcntl(int fd, int cmd, int arg) {
        log = log && log(5, "> fcntl(%d, %d, %d)\n", fd, cmd, arg);
        int ret = m_Termios.fcntl(fd, cmd, arg);
        log = log && log(3, "< fcntl(%d, %d, %d) => %d\n", fd, cmd, arg, ret);
        return ret;
    }

    /**
     * Cfgetispeed int.
     *
     * @param termios the termios
     * @return the int
     */
    static public int cfgetispeed(Termios termios) {
        log = log && log(5, "> cfgetispeed(%s)\n", termios);
        int ret = m_Termios.cfgetispeed(termios);
        log = log && log(3, "< cfgetispeed(%s) => %d\n", termios, ret);
        return ret;
    }

    /**
     * Cfgetospeed int.
     *
     * @param termios the termios
     * @return the int
     */
    static public int cfgetospeed(Termios termios) {
        log = log && log(5, "> cfgetospeed(%s)\n", termios);
        int ret = m_Termios.cfgetospeed(termios);
        log = log && log(3, "< cfgetospeed(%s) => %d\n", termios, ret);
        return ret;
    }

    /**
     * Cfsetispeed int.
     *
     * @param termios the termios
     * @param speed   the speed
     * @return the int
     */
    static public int cfsetispeed(Termios termios, int speed) {
        log = log && log(5, "> cfgetospeed(%s,%d)\n", termios, speed);
        int ret = m_Termios.cfsetispeed(termios, speed);
        log = log && log(3, "< cfgetospeed(%s,%d) => %d\n", termios, speed, ret);
        return ret;
    }

    /**
     * Cfsetospeed int.
     *
     * @param termios the termios
     * @param speed   the speed
     * @return the int
     */
    static public int cfsetospeed(Termios termios, int speed) {
        log = log && log(5, "> cfgetospeed(%s,%d)\n", termios, speed);
        int ret = m_Termios.cfsetospeed(termios, speed);
        log = log && log(3, "< cfgetospeed(%s,%d) => %d\n", termios, speed, ret);
        return ret;
    }

    /**
     * Sets .
     *
     * @param fd      the fd
     * @param termios the termios
     * @param speed   the speed
     * @return the
     */
    static public int setspeed(int fd, Termios termios, int speed) {
        log = log && log(5, "> setspeed(%d,%s,%d)\n", fd, termios, speed);
        int ret = m_Termios.setspeed(fd, termios, speed);
        log = log && log(3, "< setspeed(%d,%s,%d) => %d\n", fd, termios, speed, ret);
        return ret;
    }

    /**
     * Tcflush int.
     *
     * @param a the a
     * @param b the b
     * @return the int
     */
    static public int tcflush(int a, int b) {
        log = log && log(5, "> tcflush(%d,%d)\n", a, b);
        int ret = m_Termios.tcflush(a, b);
        log = log && log(3, "< tcflush(%d,%d) => %d\n", a, b, ret);
        return ret;
    }

    /**
     * Tcdrain int.
     *
     * @param fd the fd
     * @return the int
     */
    static public int tcdrain(int fd) {
        log = log && log(5, "> tcdrain(%d)\n", fd);
        int ret = m_Termios.tcdrain(fd);
        log = log && log(3, "< tcdrain(%d) => %d\n", fd, ret);
        return ret;
    }

    /**
     * Cfmakeraw.
     *
     * @param fd      the fd
     * @param termios the termios
     */
    static public void cfmakeraw(int fd, Termios termios) {
        log = log && log(5, "> cfmakeraw(%d,%s)\n", fd, termios);
        m_Termios.cfmakeraw(termios);
        log = log && log(3, "< cfmakeraw(%d,%s)\n", fd, termios);
    }

    /**
     * Tcgetattr int.
     *
     * @param fd      the fd
     * @param termios the termios
     * @return the int
     */
    static public int tcgetattr(int fd, Termios termios) {
        log = log && log(5, "> tcgetattr(%d,%s)\n", fd, termios);
        int ret = m_Termios.tcgetattr(fd, termios);
        log = log && log(3, "< tcgetattr(%d,%s) => %d\n", fd, termios, ret);
        return ret;
    }

    /**
     * Tcsetattr int.
     *
     * @param fd      the fd
     * @param cmd     the cmd
     * @param termios the termios
     * @return the int
     */
    static public int tcsetattr(int fd, int cmd, Termios termios) {
        log = log && log(5, "> tcsetattr(%d,%d,%s)\n", fd, cmd, termios);
        int ret = m_Termios.tcsetattr(fd, cmd, termios);
        log = log && log(3, "< tcsetattr(%d,%d,%s) => %d\n", fd, cmd, termios, ret);
        return ret;
    }

    /**
     * Tcsendbreak int.
     *
     * @param fd       the fd
     * @param duration the duration
     * @return the int
     */
    static public int tcsendbreak(int fd, int duration) {
        log = log && log(5, "> tcsendbreak(%d,%d,%s)\n", fd, duration);
        int ret = m_Termios.tcsendbreak(fd, duration);
        log = log && log(3, "< tcsendbreak(%d,%d,%s) => %d\n", fd, duration, ret);
        return ret;
    }

    /**
     * Open int.
     *
     * @param s the s
     * @param t the t
     * @return the int
     */
    static public int open(String s, int t) {
        log = log && log(5, "> open('%s',%08X)\n", s, t);
        int ret = m_Termios.open(s, t);
        log = log && log(3, "< open('%s',%08X) => %d\n", s, t, ret);
        return ret;
    }

    /**
     * Close int.
     *
     * @param fd the fd
     * @return the int
     */
    static public int close(int fd) {
        log = log && log(5, "> close(%d)\n", fd);
        int ret = m_Termios.close(fd);
        log = log && log(3, "< close(%d) => %d\n", fd, ret);
        return ret;
    }

    /**
     * Write int.
     *
     * @param fd     the fd
     * @param buffer the buffer
     * @param len    the len
     * @return the int
     */
    static public int write(int fd, byte[] buffer, int len) {
        log = log && log(5, "> write(%d,%s,%d)\n", fd, log(buffer, 8), len);
        int ret = m_Termios.write(fd, buffer, len);
        log = log && log(3, "< write(%d,%s,%d) => %d\n", fd, log(buffer, 8), len, ret);
        return ret;
    }

    /**
     * Read int.
     *
     * @param fd     the fd
     * @param buffer the buffer
     * @param len    the len
     * @return the int
     */
    static public int read(int fd, byte[] buffer, int len) {
        log = log && log(5, "> read(%d,%s,%d)\n", fd, log(buffer, 8), len);
        int ret = m_Termios.read(fd, buffer, len);
        log = log && log(3, "< read(%d,%s,%d) => %d\n", fd, log(buffer, 8), len, ret);
        return ret;
    }

    /**
     * Ioctl int.
     *
     * @param fd   the fd
     * @param cmd  the cmd
     * @param data the data
     * @return the int
     */
    static public int ioctl(int fd, int cmd, int... data) {
        log = log && log(5, "> ioctl(%d,%d,[%s])\n", fd, cmd, Arrays.toString(data));
        int ret = m_Termios.ioctl(fd, cmd, data);
        log = log && log(3, "< ioctl(%d,%d,[%s]) => %d\n", fd, cmd, Arrays.toString(data), ret);
        return ret;
    }

    private static String toString(int n, FDSet fdset) {
        StringBuffer s = new StringBuffer("[");
        for (int fd = 0; fd < n; fd++) {
            if (fd > 0) {
                s.append(",");
            }
            if (FD_ISSET(fd, fdset)) {
                s.append(Integer.toString(fd));
            }
        }
        s.append("]");
        return s.toString();
    }

    /**
     * Unlike Linux select this does not modify 'timeout' so it can be re-used.
     *
     * @param n       the n
     * @param read    the read
     * @param write   the write
     * @param error   the error
     * @param timeout the timeout
     * @return the int
     */
    static public int select(int n, FDSet read, FDSet write, FDSet error, TimeVal timeout) {
        log = log && log(5, "> select(%d,%s,%s,%s,%s)\n", n, toString(n, read), toString(n, write), toString(n, error), timeout);
        int ret = m_Termios.select(n, read, write, error, timeout);
        log = log && log(3, "< select(%d,%s,%s,%s,%s) => %d\n", n, toString(n, read), toString(n, write), toString(n, error), timeout, ret);
        return ret;
    }

    /**
     * Poll int.
     *
     * @param fds     the fds
     * @param nfds    the nfds
     * @param timeout the timeout
     * @return the int
     */
    static public int poll(Pollfd[] fds, int nfds, int timeout) {
        log = log && log(5, "> poll(%s,%d,%d)\n", log(fds, 8), nfds, timeout);
        int ret = m_Termios.poll(fds, nfds, timeout);
        log = log && log(3, "< poll(%s,%d,%d) => %d\n", log(fds, 8), nfds, timeout, ret);
        return ret;
    }

    /**
     * Can poll boolean.
     *
     * @return the boolean
     */
    static public boolean canPoll() {
        return m_Termios.canPoll();
    }

    /**
     * Pipe int.
     *
     * @param fds the fds
     * @return the int
     */
    static public int pipe(int[] fds) {
        log = log && log(5, "> pipe([%d,%d,%d])\n", fds.length, fds[0], fds[1]);
        int ret = m_Termios.pipe(fds);
        log = log && log(3, "< pipe([%d,%d,%d]) => %d\n", fds.length, fds[0], fds[1], ret);
        return ret;
    }

    /**
     * Perror.
     *
     * @param msg the msg
     */
    static public void perror(String msg) {
        m_Termios.perror(msg);
    }

    /**
     * New fd set fd set.
     *
     * @return the fd set
     */
    static public FDSet newFDSet() {
        return m_Termios.newFDSet();
    }

    /**
     * Fd set.
     *
     * @param fd  the fd
     * @param set the set
     */
    static public void FD_SET(int fd, FDSet set) {
        if (set != null) {
            set.FD_SET(fd);
        }
    }

    /**
     * Fd clr.
     *
     * @param fd  the fd
     * @param set the set
     */
    static public void FD_CLR(int fd, FDSet set) {
        if (set != null) {
            set.FD_CLR(fd);
        }
    }

    /**
     * Fd isset boolean.
     *
     * @param fd  the fd
     * @param set the set
     * @return the boolean
     */
    static public boolean FD_ISSET(int fd, FDSet set) {
        if (set == null) {
            return false;
        }
        return set.FD_ISSET(fd);
    }

    /**
     * Fd zero.
     *
     * @param set the set
     */
    static public void FD_ZERO(FDSet set) {
        if (set != null) {
            set.FD_ZERO();
        }
    }

    /**
     * Gets port list.
     *
     * @return the port list
     */
    static public List<String> getPortList() {
        return m_Termios.getPortList();

    }

    /**
     * Gets port name pattern.
     *
     * @param jtermios the jtermios
     * @return the port name pattern
     */
    static public Pattern getPortNamePattern(JTermiosInterface jtermios) {
        String ps = System.getProperty("purejavacomm.portnamepattern." + jtermios.getClass().getName());
        if (ps == null) {
            ps = System.getProperty("purejavacomm.portnamepattern");
        }
        if (ps == null) {
            ps = jtermios.getPortNamePattern();
        }
        return Pattern.compile(ps);
    }

    /**
     * The type J termios logging.
     */
    public static class JTermiosLogging {

        private static int LOG_MASK = 1;
        /**
         * The constant log.
         */
        public static boolean log = false;

        static { // initialization 
            String loglevel = System.getProperty("purejavacomm.loglevel");
            if (loglevel != null) {
                setLogLevel(Integer.parseInt(loglevel));
            }
        }

        /**
         * Lineno string.
         *
         * @return the string
         */
        public static String lineno() {
            return lineno(0);
        }

        /**
         * Lineno string.
         *
         * @param n the n
         * @return the string
         */
        public static String lineno(int n) {
            StackTraceElement e = Thread.currentThread().getStackTrace()[2 + n];
            return String.format("class %s line% d", e.getClassName(), e.getLineNumber());
        }

        /**
         * Ref string.
         *
         * @param struct the struct
         * @return the string
         */
        public static String ref(Structure struct) {
            if (struct == null) {
                return "null";
            } else {
                return struct.getPointer().toString();
            }
        }

        /**
         * Log string.
         *
         * @param bts the bts
         * @param n   the n
         * @return the string
         */
        public static String log(byte[] bts, int n) {
            StringBuffer b = new StringBuffer();
            if (n < 0 || n > bts.length) {
                n = bts.length;
            }
            b.append(String.format("[%d", bts.length));
            for (int i = 0; i < n; i++) {
                b.append(String.format(",0x%02X", bts[i]));
            }
            if (n < bts.length) {
                b.append("...");
            }
            b.append("]");
            return b.toString();
        }

        /**
         * Log string.
         *
         * @param ints the ints
         * @param n    the n
         * @return the string
         */
        public static String log(int[] ints, int n) {
            StringBuilder b = new StringBuilder();
            if (n < 0 || n > ints.length) {
                n = ints.length;
            }
            b.append(String.format("[%d", ints.length));
            for (int i = 0; i < n; i++) {
                b.append(String.format(",0x%08X", ints[i]));
            }
            if (n < ints.length) {
                b.append("...");
            }
            b.append("]");
            return b.toString();
        }

        /**
         * Log string.
         *
         * @param bts the bts
         * @param n   the n
         * @return the string
         */
        public static String log(char[] bts, int n) {
            StringBuilder b = new StringBuilder();
            if (n < 0 || n > bts.length) {
                n = bts.length;
            }
            b.append(String.format("[%d", bts.length));
            for (int i = 0; i < n; i++) {
                b.append(String.format(",%c", bts[i]));
            }
            if (n < bts.length) {
                b.append("...");
            }
            b.append("]");
            return b.toString();
        }

        /**
         * Log string.
         *
         * @param bts the bts
         * @param n   the n
         * @return the string
         */
        public static String log(Object[] bts, int n) {
            StringBuilder b = new StringBuilder();
            if (n < 0 || n > bts.length) {
                n = bts.length;
            }
            b.append(String.format("[%d", bts.length));
            for (int i = 0; i < n; i++) {
                b.append(",");
                b.append(bts[i] != null ? bts[i].toString() : "null");
            }
            if (n < bts.length) {
                b.append("...");
            }
            b.append("]");
            return b.toString();
        }

        /**
         * Log boolean.
         *
         * @param l      the l
         * @param format the format
         * @param args   the args
         * @return the boolean
         */
        static public boolean log(int l, String format, Object... args) {
            final StringBuffer buffer = new StringBuffer();
            if (l == 0 || LOG_MASK != 0) {
                buffer.setLength(0);
                if ((LOG_MASK & (1 << (5))) != 0) {
                    buffer.append(String.format("%06d,", System.currentTimeMillis() % 1000000));
                }
                if ((LOG_MASK & (1 << (6))) != 0) {
                    buffer.append(lineno(2));
                    buffer.append(", ");
                }
                if ((LOG_MASK & (1 << (7))) != 0) {
                    buffer.append("thread id ");
                    buffer.append(Thread.currentThread().getId());
                    buffer.append(", ");
                    buffer.append(Thread.currentThread().getName());
                    buffer.append(", ");
                }
                if (l == 0 || (LOG_MASK & (1 << (l - 1))) != 0) {
                    buffer.append(String.format(format, args));
                }
                if (buffer.length() > 0) {
                    System.err.printf("log: " + buffer.toString());
                }
            }
            return true;
        }

        /**
         * Sets log level.
         *
         * @param l the l
         */
        public static void setLogLevel(int l) {
            LOG_MASK = 0;
            for (int i = 0; i < l; i++) {
                LOG_MASK = (LOG_MASK << 1) + 1;
            }
            log = LOG_MASK != 0;
        }

        /**
         * Sets log mask.
         *
         * @param mask the mask
         */
        public static void setLogMask(int mask) {
            LOG_MASK = mask;
            log = LOG_MASK != 0;
        }
    }

}
