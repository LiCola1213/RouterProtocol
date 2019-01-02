package com.licola.route.api;

import android.content.Intent;
import android.os.Bundle;
import com.licola.route.annotation.RouteMeta;
import com.licola.route.api.exceptions.RouteBadChainException;
import com.licola.route.api.exceptions.RouteBadRequestException;
import com.licola.route.api.exceptions.RouteConfigError;
import com.licola.route.api.source.Source;
import java.util.Map;

/**
 * Created by LiCola on 2018/7/5. 控制实际的跳转的 跳转拦截器
 */
public class JumpInterceptor implements Interceptor {

  @Override
  public void intercept(Chain chain) {
    if (!(chain instanceof RealChain)) {
      chain.onBreak(new RouteBadChainException("错误的Chain链对象调用"));
      return;
    }

    RealChain realChain = (RealChain) chain;

    Map<String, RouteMeta> routeMap = realChain.getRouteMap();

    Source source = realChain.getSource();

    RouteRequest request = realChain.getRequest();

    if (realChain.getResponse() != null) {
      chain.onProcess();
      return;
    }

    Intent intent = request.getIntent();
    int requestCode = request.getRequestCode();
    Bundle bundle = request.getBundle();
    RouteResponse response;

    String requestPath = request.getOriginalPath();
    String redirectPath = request.getRedirectPath();

    if (source.isResolveNotDeclareIntent(intent)) {
      boolean isRedirect = !Utils.isEmpty(redirectPath);
      response = RouteResponse.createNotDeclare(intent, requestCode, isRedirect);
    } else {
      if (Utils.isEmpty(routeMap)) {
        chain.onBreak(new RouteConfigError("路由配置错误 路由表为空"));
        return;
      }

      //优先使用 重定向路径
      String path = !Utils.isEmpty(redirectPath) ? redirectPath : requestPath;

      if (Utils.isEmpty(path)) {
        chain.onBreak(new RouteBadRequestException("路由查表方式跳转 但是path为空 无法查表"));
        return;
      }

      RouteMeta meta = routeMap.get(path);
      if (meta == null || meta.getTarget() == null) {
        chain.onBreak(new RouteBadRequestException("路由表中没有查询到请求目标"));
        return;
      }

      Class<?> target = meta.getTarget();
      if (intent == null) {
        intent = new Intent();
        request.setIntent(intent);
      }
      intent.setClass(source.getContext(), target);

      response = RouteResponse
          .createDeclare(intent, requestCode, path, meta, !Utils.isEmpty(redirectPath));
    }

    source.startActivity(intent, requestCode,bundle);

    chain.onProcess(response);
  }


}
