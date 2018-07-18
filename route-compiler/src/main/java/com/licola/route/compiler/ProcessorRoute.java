package com.licola.route.compiler;

import static com.licola.route.compiler.Constants.PACKAGE_API;
import static com.licola.route.compiler.Constants.PATH_SEPARATOR;
import static com.licola.route.compiler.Constants.ROUTE_ANNOTATION_PROTOCOL;
import static com.licola.route.compiler.Constants.ROUTE_CLASS_INNER_API;
import static com.licola.route.compiler.Constants.ROUTE_CLASS_INNER_ROUTE;
import static com.licola.route.compiler.Constants.ROUTE_CLASS_INTERCEPTOR;
import static com.licola.route.compiler.Constants.ROUTE_CLASS_ROUTE_API;
import static com.licola.route.compiler.Constants.ROUTE_CLASS_ROUTE_ROOT;
import static com.licola.route.compiler.Constants.ROUTE_FIELD_ROUTE_MODULE_NAME;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_LOAD;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_LOAD_PARAMETER;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_NAVIGATION;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_NAVIGATION_PARAMETER_ACTIVITY;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_NAVIGATION_PARAMETER_INTERCEPTOR;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_NAVIGATION_PARAMETER_REQUEST_CODE;
import static com.licola.route.compiler.Constants.ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET;

import com.google.auto.common.MoreElements;
import com.licola.route.annotation.Route;
import com.licola.route.annotation.RouteMeta;
import com.licola.route.annotation.RoutePath;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 * Created by LiCola on 2017/6/21.
 */

public class ProcessorRoute {

  private Set<? extends Element> elements;
  private String packName;
  private String className;
  private String moduleName;

  public static ProcessorRoute build(Set<? extends Element> elements,
      String packName, String className, String moduleName) {
    return new ProcessorRoute(elements, packName, className, moduleName);
  }

  public ProcessorRoute(Set<? extends Element> elements, String packName,
      String className, String moduleName) {
    this.elements = elements;
    this.packName = packName;
    this.className = className;
    this.moduleName = moduleName;
  }

  TypeSpec process() {
    if (CheckUtils.isEmpty(elements)) {
      return null;
    }

    if (CheckUtils.isEmpty(className)) {
      return null;
    }

    //定义类
    TypeSpec.Builder classSpecBuild = TypeSpec.classBuilder(className)
        .addField(FieldSpec.builder(String.class, ROUTE_FIELD_ROUTE_MODULE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", moduleName)
            .build());

    //定义内部Root类
    TypeSpec.Builder classInnerRoute = TypeSpec.classBuilder(ROUTE_CLASS_INNER_ROUTE)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addSuperinterface(ClassName.get(PACKAGE_API, ROUTE_CLASS_ROUTE_ROOT));

    //定义内部Api类
    TypeSpec.Builder classInnerApi = TypeSpec.classBuilder(ROUTE_CLASS_INNER_API)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

    //定义Def注释
    AnnotationSpec.Builder annotationBuilder =
        AnnotationSpec.builder(ClassName.get("android.support.annotation", "StringDef"));

    //定义load方法
    MethodSpec.Builder methodBuild = MethodSpec.methodBuilder(ROUTE_METHOD_LOAD)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ParameterizedTypeName.get(Map.class, String.class, RouteMeta.class),
            ROUTE_METHOD_LOAD_PARAMETER)
        .returns(void.class);

    for (Element element : elements) {
      addClassAndAnnotationField(moduleName, ROUTE_FIELD_ROUTE_MODULE_NAME, element, classSpecBuild,
          annotationBuilder,
          methodBuild);
    }

    classInnerRoute
        .addMethod(methodBuild.build());

    //定义并构建 protocol 注解
    TypeSpec.Builder annotationBuild = TypeSpec.annotationBuilder(ROUTE_ANNOTATION_PROTOCOL)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(annotationBuilder.build())//添加Def注释
        .addAnnotation(AnnotationSpec.builder(Retention.class)
            .addMember("value", "$L",
                ClassName.get("java.lang.annotation.RetentionPolicy", "SOURCE"))
            .build())//添加Retention
        .addJavadoc("作用于Source源文件的注释\n")
        .addJavadoc("用于代码检查和辅助输入\n");

    ClassName apiClassName = ClassName.get(PACKAGE_API, ROUTE_CLASS_ROUTE_API);
    classInnerApi.addField(apiClassName, "api", Modifier.PRIVATE)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(apiClassName, "api")
            .addStatement("this.$N = $N", "api", "api")
            .build());

    classInnerApi.addMethods(makeMehtods());

    classSpecBuild
        .addModifiers(Modifier.PUBLIC)
        .addType(classInnerRoute.build())
        .addType(classInnerApi.build())
        .addType(annotationBuild.build());//添加类的注释字段

    classSpecBuild.addJavadoc("Created by @$L on $L \n", Route.class.getSimpleName(),
        Utils.getNowTime())
        .addJavadoc("自动生成的路由类 \n")
        .addJavadoc("功能\n")
        .addJavadoc("1：生成模块名和各@Route注解目标的静态常量\n")
        .addJavadoc("2：生成内部类Route收集路由信息\n")
        .addJavadoc("3：生成内部类Api提供模块内跳转并提供注解辅助输入\n")
    ;

    return classSpecBuild.build();
  }

  private void addClassAndAnnotationField(String moduleNameValue, String moduleName,
      Element element,
      Builder classSpecBuild, AnnotationSpec.Builder annotationBuilder,
      MethodSpec.Builder methodBuild) {
    String elementName = element.getSimpleName().toString();
    elementName = elementName.replace("Activity", "_Activity").toUpperCase();
    String name = element.getAnnotation(Route.class).name();

    if (CheckUtils.isEmpty(name)) {
      name = elementName;
    }

    //类中 添加静态变量
    classSpecBuild.addField(FieldSpec.builder(String.class, elementName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", name)
        .build());

    //注释中 添加字段
    annotationBuilder.addMember("value", "$L", elementName);

    //方法中 添加表达式
    methodBuild
        .addStatement(
            ROUTE_METHOD_LOAD_PARAMETER + ".put(\"$L$L$L\",$T.create($L.class,$L,$L))",
            moduleNameValue, PATH_SEPARATOR, name,
            RouteMeta.class, MoreElements.asType(element).getQualifiedName().toString(),
            elementName,
            moduleName);
  }

  private List<MethodSpec> makeMehtods() {

    ArrayList<MethodSpec> methodSpecs = new ArrayList<>();

    ParameterSpec parameterTarget = ParameterSpec
        .builder(String.class, ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET)
        .addAnnotation(ClassName.get(packName, className, ROUTE_ANNOTATION_PROTOCOL))
        .build();

    ParameterSpec parameterInterceptor = ParameterSpec
        .builder(ClassName.get(PACKAGE_API, ROUTE_CLASS_INTERCEPTOR),
            ROUTE_METHOD_NAVIGATION_PARAMETER_INTERCEPTOR)
        .build();

    ParameterSpec parameterActivity = ParameterSpec
        .builder(ClassName.get("android.app", "Activity"),
            ROUTE_METHOD_NAVIGATION_PARAMETER_ACTIVITY)
        .build();

    ParameterSpec parameterRequestCode = ParameterSpec
        .builder(int.class, ROUTE_METHOD_NAVIGATION_PARAMETER_REQUEST_CODE)
        .build();

    methodSpecs.add(MethodSpec.methodBuilder(ROUTE_METHOD_NAVIGATION)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(parameterTarget)
        .addStatement("api.navigation($T.makePath($L,$L))", RoutePath.class,
            ROUTE_FIELD_ROUTE_MODULE_NAME,
            ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET)
        .returns(void.class)
        .build());

    methodSpecs.add(MethodSpec.methodBuilder(ROUTE_METHOD_NAVIGATION)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(parameterTarget)
        .addParameter(parameterInterceptor)
        .addStatement("api.navigation($T.makePath($L,$L),$L)", RoutePath.class,
            ROUTE_FIELD_ROUTE_MODULE_NAME,
            ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET,
            ROUTE_METHOD_NAVIGATION_PARAMETER_INTERCEPTOR)
        .returns(void.class)
        .build());

    methodSpecs.add(MethodSpec.methodBuilder(ROUTE_METHOD_NAVIGATION)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(parameterTarget)
        .addParameter(parameterActivity)
        .addParameter(parameterRequestCode)
        .addStatement("api.navigation($T.makePath($L,$L),$L,$L)", RoutePath.class,
            ROUTE_FIELD_ROUTE_MODULE_NAME,
            ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET,
            ROUTE_METHOD_NAVIGATION_PARAMETER_ACTIVITY,
            ROUTE_METHOD_NAVIGATION_PARAMETER_REQUEST_CODE
        )
        .returns(void.class)
        .build());

    methodSpecs.add(MethodSpec.methodBuilder(ROUTE_METHOD_NAVIGATION)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(parameterTarget)
        .addParameter(parameterActivity)
        .addParameter(parameterRequestCode)
        .addParameter(parameterInterceptor)
        .addStatement("api.navigation($T.makePath($L,$L),$L,$L,$L)", RoutePath.class,
            ROUTE_FIELD_ROUTE_MODULE_NAME,
            ROUTE_METHOD_NAVIGATION_PARAMETER_TARGET,
            ROUTE_METHOD_NAVIGATION_PARAMETER_ACTIVITY,
            ROUTE_METHOD_NAVIGATION_PARAMETER_REQUEST_CODE,
            ROUTE_METHOD_NAVIGATION_PARAMETER_INTERCEPTOR
        )
        .returns(void.class)
        .build());

    return methodSpecs;
  }

}
