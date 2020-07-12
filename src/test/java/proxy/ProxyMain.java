package proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * 最简单的动态代理<br/>
 * Date: 2020/7/1 7:23 <br/>
 *
 * @author YANGCX
 */
public class ProxyMain {

  public static void main(String[] args) {
    InvocationHandler invocationHandler = (proxy, method, pArgs) -> {
      // proxy 是 null
      // method 是目标方法
      // pArgs 是传入的方法参数
      System.out.println(method);
      if (method.getName().equals("morning")) {
        System.out.println("Good morning, " + pArgs[0]);
      }
      return null;
    };

    Hello hello = (Hello) Proxy.newProxyInstance(
      Hello.class.getClassLoader(),
      new Class[]{Hello.class},
      invocationHandler
    );

    hello.morning("Bob");
  }

}
