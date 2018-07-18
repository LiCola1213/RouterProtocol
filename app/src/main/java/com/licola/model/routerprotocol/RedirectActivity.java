package com.licola.model.routerprotocol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.licola.route.annotation.Route;

@Route(name = "user/login")
public class RedirectActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_redirect);
  }
}
