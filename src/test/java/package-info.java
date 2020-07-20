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
 **/
