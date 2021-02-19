import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MP2 {
    static final int PORT = 53;
    static final int BUFFER_SIZE = 65530;
    static final String SPACER = "    ";
    static final String MARKER = "*** ";

    public void runServer() {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

            while (true) {
                System.out.println(MARKER + "Now listening for packets...");
                socket.receive(packet);
                System.out.println(MARKER + "Packet received from " + packet.getAddress().getHostAddress());
                printDNSPacket(packet);
                answerDNSQuery(packet, socket, packet.getAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printDNSPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        System.out.println(SPACER + "Transaction ID: " + toHex(data, 0, 2));
        System.out.println(SPACER + "Flags: " + toHex(data, 2, 4));
        System.out.println(SPACER + "Questions: " + toInt(data, 4, 6));
        System.out.println(SPACER + "Answer RRs: " + toInt(data, 6, 8));
        System.out.println(SPACER + "Authority RRs: " + toInt(data, 8, 10));
        System.out.println(SPACER + "Additional RRs: " + toInt(data, 10, 12));
    }

    public void answerDNSQuery(DatagramPacket packet, DatagramSocket socket, InetAddress address) {

    }

    private String toHex(byte[] arr, int start, int end) {
        StringBuilder hex = new StringBuilder();
        for (int i = start; i < end; i++) {
            hex.append(String.format("%02x", arr[i]));
        }
        return "0x" + hex.toString();
    }

    private int toInt(byte[] arr, int start, int end) {
        int num = 0;
        int shift = (end - start - 1) * 8;
        for (int i = start; i < end; i++) {
            num += (arr[i] << shift);
            shift -= 8;
        }
        return num;
    }

    private int getBit(byte b, int pos) {
        return (b >> pos) & 1;
    }

    public static void main(String[] args) {
        MP2 mp2 = new MP2();
        mp2.runServer();
    }
}
