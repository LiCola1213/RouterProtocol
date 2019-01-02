package com.licola.route.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 路由请求体的封装
 *
 * @author LiCola
 * @date 2018/7/23
 */
public class RouteRequest {

  public static final int STANDARD_REQUEST_CODE = Activity.RESULT_OK;

  private int requestCode;
  @Nullable
  private String originalPath;
  @Nullable
  private String redirectPath;
  @Nullable
  private Intent intent;
  @Nullable
  private Bundle bundle;

  public static RouteRequest create(int requestCode,
      String path) {
    return new RouteRequest(requestCode, path);
  }

  private RouteRequest(int requestCode,
      @Nullable String originalPath) {
    this.requestCode = requestCode;
    this.originalPath = originalPath;
  }

  /**
   * 返回Intent实例 使用该方法得到的Intent，加入一些参数 1:添加extra附加参数 2:设置flag标识
   */
  public Intent putArgs() {
    if (this.intent == null) {
      this.intent = new Intent();
    }
    return intent;
  }

  public Intent putArgs(Intent intent) {
    this.intent = intent;
    return intent;
  }

  /**
   * 更新Intent 建议使用该方法返回的Intent
   */
  public Intent notifyIntent() {
    this.intent = new Intent();
    return intent;
  }

  public Bundle putBundle() {
    if (this.bundle == null) {
      this.bundle = new Bundle();
    }
    return bundle;
  }

  public Bundle putBundle(Bundle bundle) {
    this.bundle = bundle;
    return bundle;
  }

  public Bundle notifyBundle() {
    this.bundle = new Bundle();
    return bundle;
  }


  /**
   * 更新显式路径
   *
   * @return true:成功更新路径
   */
  public boolean notifyPath(String newPath) {

    //非空检查
    if (Utils.isEmpty(newPath)) {
      return false;
    }

    //原始路径为空 直接更新
    if (Utils.isEmpty(originalPath)) {
      this.originalPath = newPath;
      return true;
    }

    //重定向路径空 直接更新
    if (Utils.isEmpty(redirectPath)) {
      this.redirectPath = newPath;
      return true;
    }

    //在原始路径和重定向路径非空 情况下 尝试更新重定向路径
    if (!redirectPath.equals(newPath)) {
      this.redirectPath = newPath;
      return true;
    }

    return false;
  }


  @Nullable
  public String getOriginalPath() {
    return originalPath;
  }

  @Nullable
  public String getRedirectPath() {
    return redirectPath;
  }

  @Nullable
  Intent getIntent() {
    return intent;
  }

  @Nullable
  public Bundle getBundle() {
    return bundle;
  }

  void setIntent(@NonNull Intent intent) {
    this.intent = intent;
  }

  public int getRequestCode() {
    return requestCode;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RouteRequest{");
    sb.append("requestCode=").append(requestCode);
    sb.append(", originalPath='").append(originalPath).append('\'');
    sb.append(", redirectPath='").append(redirectPath).append('\'');
    sb.append(", intent=").append(intent);
    sb.append('}');
    return sb.toString();
  }
}
