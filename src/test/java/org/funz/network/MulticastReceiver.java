package org.funz.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MulticastReceiver {

    public static void main(String[] args) {
        String IP=args[0];
        InetAddress ia = null;
        byte[] buffer = new byte[65535];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

        try {
            ia = InetAddress.getByName(IP);
        } catch (UnknownHostException e) {
            System.err.println(e);
        }

        try {
            MulticastSocket ms = new MulticastSocket(MulticastSender.PORT);
            ms.joinGroup(ia);
            while (true) {
                ms.receive(dp);
                String s = new String(dp.getData(), 0, dp.getLength());
                System.out.println(s);
            }
        } catch (SocketException se) {
            System.err.println(se);
        } catch (IOException ie) {
            System.err.println(ie);
        }
    }
}