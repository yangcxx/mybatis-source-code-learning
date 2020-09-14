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
 * cxy TODO MyBatis 与 Spring 集成后，由于对象的生命周期交由 Spring 管理，一级缓存使用也比较少
 *
 * MyBatis 中的四大对象：
 * 1、Executor 决定 SQL 类型：select|update|delete|insert
 * 2、ParameterHandler 为 SQL 赋值
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
 *
 * MyBatis 日志选取原理：优先选择自定义配置，否则根据既有流程进行实例化
 *
 * 相关面试题：
 * 1、与 Spring 整合后，为什么一级缓存会失效？
 * 一级缓存基于 SqlSession，集成后，只有存在事务管理时一级缓存才会生效（session 管理交由Spring管理【使用完马上销毁】，不在由用户|MyBatis管理）
 * 2、二级缓存
 * 2.1、二级缓存中的数据不能是持续增加的
 * 2.2、二级缓存是 Application 级别的
 * 2.3、二级缓存是基于命名空间的：不要使用不同的命名空间操作同一个表（否则就不要使用二级缓存）
 * 3、ResultMap与ResultType的区别：
 * 3.1、ResultType 一般是直接指定返回值与别名库中的 Java 对象的映射，如：String Map 自定义对象（属性名与数据库列名要相同）
 * 3.2、ResultMap 自己指定的返回值（对象）及返回值内部属性名和数据库列名的绑定
 * 4、#{} 与 ${} 的区别？
 * 5、MyBatis 与 Hibernate 的不同？
 * 6、为什么说MyBatis是半自动ORM映射工具？与全自动的区别是什么？
 * 全自动（Hibernate）：只需要Java对象等同于操作数据库；支持数据库底层自动适配；无法优化SQL
 * MyBatis：需要手动写SQL；对数据库依赖很大；可以自行优化SQL
 * 7、MyBatis映射文件时，如果A标签通过include引用了B标签的内容，B标签是否必须定义在A标签前面？
 * 解析映射文件时，存在重试机制：第一次解析时，会缓存无法解析的节点，待所有节点解析完成后在解析缓存的错误节点
 * 8、MyBatis的动态SQL是什么？执行原理？
 * 底层会判断是否为动态SQL，是则调用OGNL表达式去替换动态节点
 * 9、mapper的声明方式？
 * mapper-resource
 * mapper-url
 * mapper-class（注解）
 * （只能存在一种；存在优先级）
 * package
 * 10、MyBatis是如何进行分页的？分页插件原理是什么？
 * MyBatis 原生是进行内存分页
 **/
