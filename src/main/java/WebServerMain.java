
import config.ApplicationContext;

public class WebServerMain {

    public static void main(String[] args) {

        try (ApplicationContext context = new ApplicationContext()) {

            context.startServer();
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Critical startup error:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}