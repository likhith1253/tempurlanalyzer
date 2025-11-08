import com.sla.app.MainController;
import com.sla.util.AnsiColors;


 
public class SecurityLogAnalyzer {
    
    public static void main(String[] args) {
        System.out.println(AnsiColors.GRAY + "--> Working directory: " + 
            System.getProperty("user.dir") + AnsiColors.RESET);
        
        MainController controller = new MainController();
        controller.run();
    }
}
