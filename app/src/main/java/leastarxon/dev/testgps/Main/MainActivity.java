package leastarxon.dev.testgps.Main;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.inject.Inject;

import leastarxon.dev.testgps.Main.DI.DaggerMainComponent;
import leastarxon.dev.testgps.Main.DI.MainComponent;
import leastarxon.dev.testgps.Main.DI.MainModule;
import leastarxon.dev.testgps.R;
import leastarxon.dev.testgps.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    @Inject
    MainVM viewModel;
    public static MainComponent mainComponent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainComponent = DaggerMainComponent
                .builder()
                .mainModule(new MainModule())
                .build();
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainComponent.injectMainActivity(this);
        viewModel.setContext(this);
        viewModel.init();
        binding.setMainVM(viewModel);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.onDestroy();
    }
}
