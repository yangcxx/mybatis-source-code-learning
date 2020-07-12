import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * TODO<br/>
 * Date: 2020/7/13 6:58 <br/>
 *
 * @author YANGCX
 */
public class ParamMain {

  protected void add(String age, String address) {
    // TODO
  }

  public static void main(String[] args) throws NoSuchMethodException {
    // fixme JDK11 有飞机啊，拿不到真实的参数名称，只能拿到如 arg0、arg1 格式的参数名称
    Method method = ParamMain.class.getDeclaredMethod("add", String.class, String.class);
    for (Parameter parameter : method.getParameters()) {
      System.out.println(parameter.getName());
    }
  }

}
