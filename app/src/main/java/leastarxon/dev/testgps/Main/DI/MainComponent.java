package leastarxon.dev.testgps.Main.DI;

import javax.inject.Singleton;
;
import dagger.Component;
import leastarxon.dev.testgps.Main.MainActivity;

@Singleton
@Component(modules = {MainModule.class})
public interface MainComponent {
    void injectMainActivity(MainActivity activity);
}
