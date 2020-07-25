/**
 * 缓存：一级缓存和二级缓存，区别在于作用域
 * 二级缓存全局默认是开启的，但是需要在每个 namespace 下单独开启（mapper.xml 的  <cache> 标签及对应 select|insert|update|delete 的 useCache 属性是否开启）
 *
 * 一级缓存不能关闭，但是能更改默认的作用域范围
 *
 * 一级缓存：单个 SqlSession Statement
 * 二级缓存：所有的 SqlSession
 *
 * 每个缓存的单位 namespace
 *
 * 单体项目中一般很少用二级缓存，分布式项目中使用可能较多
 *
 * cxy MyBatis 与 Spring 集成后，由于对象的生命周期交由 Spring 管理，一级缓存使用也比较少
 *
 * MyBatis 中的四大对象：
 * 1、Executor 决定 SQL 类型：select|update|delete|insert
 * 2、PreparedStatement 为 SQL 赋值
 * 3、StatementHandler 与数据库交互
 * 4、ResultHandler 结果处理器
 *
 * 分页插件：
 * 1、执行一条 count 语句
 *  1.1、获取连接
 *  1.2、预编译 SQL，拿到绑定的 SQL 语句
 *  1.3、执行 count 语句，怎么返回 count 结果
 * 2、重写 SQL
 *  2.1、怎么知道 start 和 limit
 *  2.2、拼接 start 和 limit
 *  2.3、替换原来绑定的 SQL
 **/
