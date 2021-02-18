import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MP2 {
    static final int PORT = 53;
    static final int BUFFER_SIZE = 65530;

    public void runServer() {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

            while (true) {
                System.out.println("### Now listening for packets...");
                socket.receive(packet);
                System.out.println("### Packet received from " + packet.getAddress().getHostAddress());
                printDNSPacket(packet);
                answerDNSQuery(packet, socket, packet.getAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printDNSPacket(DatagramPacket packet) {

    }

    public void answerDNSQuery(DatagramPacket packet, DatagramSocket socket, InetAddress address) {

    }

    public static void main(String[] args) {
        MP2 mp2 = new MP2();
        mp2.runServer();
    }
}
