package com.licola.model.routerprotocol;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.licola.llogger.LLogger;
import com.licola.route.RouteApp;
import com.licola.route.RouteModule;
import com.licola.route.RouteProtocolApp;
import com.licola.route.RouteProtocolModule;
import com.licola.route.annotation.Route;
import com.licola.route.api.Interceptor;
import com.licola.route.api.RouteApi;
import com.licola.route.api.RouteCode;
import com.licola.route.api.RouteResponse;

@Route
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }


  public void onNavigationSimpleClick(View view) {
    RouteApi routeApi = RouteApi.build(getApplication(), new RouteProtocolApp());
    RouteApp routeApp = routeApi.create(RouteApp.class);

    routeApp.navigation(RouteProtocolApp.SecondActivity);
  }

  public void onNavigationInterceptorClick(View view) {
    RouteApi routeApi = RouteApi.build(getApplication(), new RouteProtocolApp(), new Interceptor() {
      @Override
      public RouteResponse intercept(RouteApi route, RouteResponse response) {
        LLogger.d("外部注入的拦截器 可以修改目标 重定向到其他页面");

        return RouteResponse.notifyTarget(response, RouteProtocolApp.RedirectActivity);
      }
    });

    RouteApp routeApp = routeApi.create(RouteApp.class);
    @RouteCode.Code int navigation = routeApp.navigation(RouteProtocolApp.SecondActivity);
    switch (navigation) {
      case RouteCode.CODE_FAILED:
        LLogger.e("跳转界面失败");
        break;
      case RouteCode.CODE_PROCESS:
        LLogger.d("跳转处理中");
        break;
      case RouteCode.CODE_REDIRECT:
        LLogger.d("重定向跳转");
        break;
      case RouteCode.CODE_SUCCESS:
        LLogger.d("成功跳转");
        break;
    }
  }

  public void onNavigationInterceptorArgClick(View view) {
    RouteApi routeApi = RouteApi.build(getApplication(), new RouteProtocolApp(), new Interceptor() {
      @Override
      public RouteResponse intercept(RouteApi route, RouteResponse response) {
        LLogger.d("外部注入的拦截器 优先级比较低");
        return response;
      }
    });
    RouteApp routeApp = routeApi.create(RouteApp.class);
    routeApp.navigation(RouteProtocolApp.SecondActivity, new Interceptor() {
      @Override
      public RouteResponse intercept(RouteApi route, RouteResponse response) {
        LLogger.d("随参数注入的拦截器 优先级最高 可以修改目标 重定向到其他页面");
        return RouteResponse.notifyTarget(response, RouteProtocolApp.RedirectActivity);
      }
    });

  }

  public void onNavigationModuleClick(View view) {
    RouteApi routeApi = RouteApi.build(getApplication(), new RouteProtocolModule());
    RouteModule routeModule = routeApi.create(RouteModule.class);
    routeModule.navigation(RouteProtocolModule.ModuleActivity);
  }
}
