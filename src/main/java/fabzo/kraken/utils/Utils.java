package fabzo.kraken.utils;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import lombok.val;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;

public class Utils {
    private static final String RUNNING_IN_IDE = "RUNNING_IN_IDE";
    private static final Object ASSIGNED_PORTS_MONITOR = new Object();
    private static Set<Integer> assignedPorts = HashSet.empty();

    public static boolean isRunningInIDE() {
        final boolean foundIdeaRtJar = System.getProperty("java.class.path").contains("idea_rt.jar");

        String runningInIDE = System.getenv(RUNNING_IN_IDE);
        if (runningInIDE == null) {
            runningInIDE = System.getProperty(RUNNING_IN_IDE);
        }

        return foundIdeaRtJar || "true".equalsIgnoreCase(runningInIDE);
    }

    public static Try<Integer> getAvailablePort() {
        synchronized (ASSIGNED_PORTS_MONITOR) {
            return Try.of(() -> {
                int hostPort;
                do {
                    final ServerSocket serverSocket = new ServerSocket(0);
                    hostPort = serverSocket.getLocalPort();
                    serverSocket.close();
                } while (assignedPorts.contains(hostPort));

                assignedPorts = assignedPorts.add(hostPort);

                return hostPort;
            });
        }
    }

    public static Try<String> getPublicFacingIP() {
        return Try.of(() -> {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint()) {
                    continue;
                }

                final Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress addr = addresses.nextElement();

                    final String ip = addr.getHostAddress();
                    if (Inet4Address.class == addr.getClass()) {
                        return ip;
                    }
                }
            }

            throw new Exception("Could not find public facing IP");
        });
    }

    public static String nameOf(final Object obj) {
        val aClass = obj.getClass();
        if ("".equals(aClass.getSimpleName())) {
            return aClass.getName();
        }
        return aClass.getSimpleName();
    }
}
