public class Main {

    public static void main(String[] args) {
        try {
            SimpleHttpServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Hello World!");
    }
}
