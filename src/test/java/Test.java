import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Test {
    public static void main(String[] args) {
        try {

            System.out.println(InetAddress.getLocalHost().getHostAddress());
            System.out.println(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),8343));
        } catch (Exception e) {

        }
    }
}
