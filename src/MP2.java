import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MP2 {
    static final int PORT = 53; // This is a DNS server - we only care about what comes in on port 53
    static final int BUFFER_SIZE = 64;
    static final String SPACER = "    ";
    static final String MARKER = "*** ";
    static final String UNKNOWN = "unknown";
    static final int NAME_START = 12;

    public void runServer() {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);

            while (true) {
                System.out.println(MARKER + "Now listening for packets...");
                socket.receive(packet);
                System.out.println(MARKER + "Packet received from " + packet.getAddress().getHostAddress());
                printDNSPacket(packet);
                System.out.println(MARKER + "Answering query...");
                answerDNSQuery(packet, socket);
                System.out.println(MARKER + "Answer sent.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printDNSPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        System.out.println(SPACER + "Transaction ID: " + toHex(data, 0, 2));
        System.out.println(SPACER + "Flags: " + toHex(data, 2, 4));
        printDNSFlags(data);
        System.out.println(SPACER + "Questions: " + toInt(data, 4, 6));
        System.out.println(SPACER + "Answer RRs: " + toInt(data, 6, 8));
        System.out.println(SPACER + "Authority RRs: " + toInt(data, 8, 10));
        System.out.println(SPACER + "Additional RRs: " + toInt(data, 10, 12));
        System.out.println(SPACER + "Queries");
        printDNSQueries(data);
    }

    private void printDNSFlags(byte[] data) {
        int response = getBit(data[2], 7);
        System.out.println(SPACER + SPACER + response + "... .... .... .... = Response: Message is a " + (response == 0 ? "query" : "response"));

        int opcode0 = getBit(data[2], 6);
        int opcode1 = getBit(data[2], 5);
        int opcode2 = getBit(data[2], 4);
        int opcode3 = getBit(data[2], 3);
        System.out.println(SPACER + SPACER + "." + opcode0 + opcode1 + opcode2 + " " + opcode3 + "... .... .... = Opcode: Standard query (0)");

        int authority = getBit(data[2], 2);
        System.out.println(SPACER + SPACER + ".... ." + authority + ".. .... .... = Authority: Server is " + (authority == 0 ? "not " : "") + "an authority for domain");

        int truncated = getBit(data[2], 1);
        System.out.println(SPACER + SPACER + ".... .." + truncated + ". .... .... = Truncated: Message is " + (truncated == 0 ? "not " : "") + "truncated");

        int recursive = getBit(data[2], 0);
        System.out.println(SPACER + SPACER + ".... ..." + recursive + " .... .... = Recursion desired: Do " + (recursive == 0 ? "not do " : "") + "query recursively");

        int reserved = getBit(data[3], 6);
        System.out.println(SPACER + SPACER + ".... .... ." + reserved + ".. .... = Z: reserved (0)");

        int unauthenticated = getBit(data[3], 4);
        System.out.println(SPACER + SPACER + ".... .... ..." + unauthenticated + " .... = Non-authenticated data: " + (unauthenticated == 0 ? "Unacceptable" : "Acceptable"));
    }

    private void printDNSQueries(byte[] data) {
        String name = getDNSName(data, NAME_START);
        int nameEnd = NAME_START + name.length() + 2;
        String type = switch (toInt(data, nameEnd, nameEnd + 2)) {
            case 1 -> "A";
            case 2 -> "NS";
            default -> UNKNOWN;
        };
        String qClass = UNKNOWN;
        if (toInt(data, nameEnd + 2, nameEnd + 4) == 1) {
            qClass = "IN";
        }
        System.out.println(SPACER + SPACER + name + ": type " + type + ", class " + qClass);
    }

    public void answerDNSQuery(DatagramPacket packet, DatagramSocket socket) throws IOException {
        byte[] queryData = packet.getData();
        byte[] responseData = new byte[BUFFER_SIZE];
        String dnsName = getDNSName(queryData, NAME_START);

        System.arraycopy(queryData, 0, responseData, 0, BUFFER_SIZE);
        // Set flags
        responseData[2] = (byte) 0x81;
        responseData[3] = (byte) 0x80;
        // Set # of answer RRs
        responseData[7] = (byte) 0x01;
        // Copy queries
        System.arraycopy(queryData, NAME_START, responseData, NAME_START, dnsName.length() + 1);
        // Set answer params
        int answerStart = NAME_START + dnsName.length() + 6;
        responseData[answerStart] = (byte) 0xc0;
        responseData[answerStart + 1] = (byte) 0x0c;
        responseData[answerStart + 2] = (byte) 0x00;
        responseData[answerStart + 3] = (byte) 0x01;
        responseData[answerStart + 4] = (byte) 0x00;
        responseData[answerStart + 5] = (byte) 0x01;
        responseData[answerStart + 7] = (byte) 0x00;
        responseData[answerStart + 8] = (byte) 0x00;
        responseData[answerStart + 9] = (byte) 0xb4;
        responseData[answerStart + 10] = (byte) 0x00;
        responseData[answerStart + 11] = (byte) 0x04;
        // Set answer IP address
        InetAddress resolution = InetAddress.getByName(dnsName);
        System.arraycopy(resolution.getAddress(), 0, responseData, answerStart + 12, 4);
        // Send packet
        socket.send(new DatagramPacket(responseData, BUFFER_SIZE, packet.getAddress(), packet.getPort()));
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

    public static String getDNSName(byte bytes[], int start) {
        int pos = start;
        StringBuilder name = new StringBuilder();
        while (bytes[pos] != 0) {
            if (pos != start) name.append(".");
            int length = bytes[pos];

            // POINTER!  We recursively print from a different place in the packet
            if (length == -64) {
                int pos2 = bytes[pos + 1] & 0xFF;
                name.append(getDNSName(bytes, pos2));
                break;

                // Otherwise the "length" is the number of characters in this part of
                // name.
            } else {
                for (int i = 1; i <= length; i++) {
                    name.append((char) bytes[pos + i]);
                }
                pos += length + 1;
            }
        }
        return name.toString();
    }

    public static void main(String[] args) {
        MP2 mp2 = new MP2();
        mp2.runServer();
    }
}
