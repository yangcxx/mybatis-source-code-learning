package cn.yangcx.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;

/**
 * 插件<br/>
 * Date: 2020/7/23 7:18 <br/>
 *
 * @author YANGCX
 */

@Intercepts(
  @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
)
@Getter
@Setter
public class MyPagePlugin implements Interceptor {

  /**
   * 从外部传入：数据库类型
   */
  private String dbType;

  // 当前拦截器需要执行的逻辑
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

    // 通过反射工具类获取 StatementHandler 中的目标参数
    MetaObject metaObject = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY,
      SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
    // 可以使用 sqlId 进行简单的正则匹配以决定当前SQL是否进行分页
    String sqlId = (String) metaObject.getValue("delegate.mappedStatement.id");

    // 需要 MyBatis 传入的参数可以从 Invocation 中获取
    Object[] args = invocation.getArgs();
    Connection connection = (Connection) args[0];
    Integer transactionTimeout = (Integer) args[1];

    // 新的查询总数 SQL 预处理
    BoundSql boundSql = statementHandler.getBoundSql();
    String sql = boundSql.getSql();
    String countSql = "select count(*) from (" + sql + ") a";
    PreparedStatement psts = connection.prepareStatement(countSql);

    // SQL 参数赋值
    ParameterHandler parameterHandler = statementHandler.getParameterHandler();
    parameterHandler.setParameters(psts);

    // 执行
    ResultSet resultSet = psts.executeQuery();
    int count = 0;
    if (resultSet.next()) {
      // 从 1 开始
      count = resultSet.getInt(1);
    }
    resultSet.close();
    psts.close();

    Map<String, Object> parameterObject = (Map<String, Object>) parameterHandler.getParameterObject();
    // 当前方法必须包含对应的分页信息
    // TODO 对象引用传递：参数被设置的值在当前过滤器执行完成后，外部的方法可以直接获取到 - 可以把结果传出去
    PageUtil pageUtil = (PageUtil) parameterObject.get("page");
    pageUtil.setCount(count);
    String pageSql = getPageSql(sql, pageUtil);
    // 把原来的SQL处理为新的分页SQL
    metaObject.setValue("delegate.boundSql.sql", pageSql);
    System.out.println(pageSql);
    // 后续的拦截器继续执行
    return invocation.proceed();
  }

  private String getPageSql(String sql, PageUtil pageUtil) {
    if (dbType.equals("mysql")) {
      return sql + " limit " + pageUtil.getStart() + " ," + pageUtil.getLimit();
    } else if (dbType.equals("oracle")) {
      // TODO
    }
    return null;
  }

  // 返回一个动态代理后的对象，target:StatementHandler
  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  // 根据传入的配置内容进行自定义
  @Override
  public void setProperties(Properties properties) {
    this.dbType = (String) properties.get("dbType");
  }
}
