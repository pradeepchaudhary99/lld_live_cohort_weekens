

package week_2;
//UI System.. DarkTheme, LightTheme
interface IButton{
    void renderButton();
}

interface IToolBar{
    void renderToolBar();
}

interface IBackground{
    void renderBackground();
}

//LightTheme
class LightThemeButton implements Button{
    @Override
    public void renderButton() {
        System.out.println("LightTheme Button");
    }
}

class LightThemeToolBar implements ToolBar{
    @Override
    public void renderToolBar() {
        System.out.println("LightTheme ToolBar");
    }
}

class LightThemeBackground implements Background{

    @Override
    public void renderBackground() {
        System.out.println("LightTheme Background");
    }
}



//Dark Theme
//LightTheme
class DarkThemeButton implements Button{

    @Override
    public void renderButton() {
        System.out.println("DarkTheme Button");
    }

}

class DarkThemeToolBar implements ToolBar{

    @Override
    public void renderToolBar() {
        System.out.println("DarkTheme ToolBar");
    }

}

class DarkThemeBackground implements Background{

    @Override
    public void renderBackground() {
        System.out.println("DarkTheme Background");
    }

}

interface ITheme{
    Button renderButton();
    ToolBar renderToolBar();
    Background renderBackground();
}

class LightThemeFactory implements Theme{
    @Override
    public Button renderButton() {
        return new LightThemeButton();
    }

    @Override
    public ToolBar renderToolBar() {
        return new LightThemeToolBar();
    }

    @Override
    public Background renderBackground() {
        return new LightThemeBackground();
    }
}

class DarkThemeFactory implements Theme{
    @Override
    public Button renderButton() {
        return new DarkThemeButton();
    }

    @Override
    public ToolBar renderToolBar() {
        return new DarkThemeToolBar();
    }

    @Override
    public Background renderBackground() {
        return new DarkThemeBackground();
    }
}

class Display{
    Theme theme;
    public Display(Theme theme){
        this.theme = theme;
    }

    public void Button(){
        theme.renderButton().renderButton();;
    }

    public void ToolBar(){
        theme.renderToolBar().renderToolBar();;
    }

    public void Background(){
        theme.renderBackground().renderBackground();;
    }

    void changeTheme(Theme theme){
        this.theme = theme;
    }

}


public class AbstractFactoryDesignPattern {
    public static void main(String[] args) {
        Display display = new Display(new LightThemeFactory());
        display.Button();
        display.Background();
        display.ToolBar();

        display.changeTheme(new DarkThemeFactory());

        display.Button();
        display.Background();
        display.ToolBar();
    }
}
