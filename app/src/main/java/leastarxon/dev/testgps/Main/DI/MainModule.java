package leastarxon.dev.testgps.Main.DI;

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import leastarxon.dev.testgps.Main.MainVM;


@Module
public class MainModule {
    @Provides
    @Singleton
    MainVM provideMainVM() {
        return new MainVM();
    }
}
