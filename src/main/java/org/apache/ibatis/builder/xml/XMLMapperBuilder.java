/**
 * Copyright ${license.git.copyrightYears} the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.*;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource,
                          Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource,
                          Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
      configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
                          Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
                          Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
      configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource,
                           Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  public void parse() {
    // 不论采用哪种配置，最终都会调用到此方法
    if (!configuration.isResourceLoaded(resource)) {
      configurationElement(parser.evalNode("/mapper"));
      configuration.addLoadedResource(resource);
      // 解析并绑定 namespace cxy 目前不知道有什么用，注释上说是与Spring集成有关系
      // 解析并绑定 namespace
      // org.apache.ibatis.builder.annotation.MapperAnnotationBuilder.loadXmlResource
      // Spring可能不知道资源的真正名称，此操作是防止资源二次加载 todo 用来解决与Spring集成无法扫描到*Mapper.java的问题
      bindMapperForNamespace();
    }

    // 所有的解析完成后，在对暂存的异常节点进行解析
    // TODO 这个机制可以保证所有的 XML 书写不用按照严格的顺序实现
    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  private void configurationElement(XNode context) {
    try {
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.isEmpty()) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      builderAssistant.setCurrentNamespace(namespace);
      // cxy 存在重试机制，可以保证最终解析正确
      //  为啥 cache-ref 会在 cache 解析之前？？
      cacheRefElement(context.evalNode("cache-ref"));
      cacheElement(context.evalNode("cache"));
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      // 公共SQL代码段校验：同时根据配置的databaseId进行选择启用
      sqlElement(context.evalNodes("/mapper/sql"));
      // 根据databaseId配置是否启用
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    // 遍历 select|insert|update|delete 节点
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context,
        requiredDatabaseId);
      try {
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        // 解析存在问题时，暂时跳过，等解析完成后再统一解析异常的节点
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  private void parsePendingCacheRefs() {
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  private void cacheRefElement(XNode context) {
    if (context != null) {
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute(
        "namespace"));
      try {
        // 验证缓存配置，验证成功则为当前namespace配置使用缓存
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        // 没找到缓存配置先暂存后面继续处理
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  private void cacheElement(XNode context) {
    if (context != null) {
      // 解析 Mapper.xml 的二级缓存配置
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      Properties props = context.getChildrenAsProperties();
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        // IN OUT INOUT
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        // javaType -> jdbcType
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property,
          javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  private void resultMapElements(List<XNode> list) {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  private ResultMap resultMapElement(XNode resultMapNode) {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  private ResultMap
  resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings,
                   Class<?> enclosingType) {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    String type = resultMapNode.getStringAttribute("type",
      resultMapNode.getStringAttribute("ofType",
        resultMapNode.getStringAttribute("resultType",
          resultMapNode.getStringAttribute("javaType"))));
    Class<?> typeClass = resolveClass(type);
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>(additionalResultMappings);
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        // 获取到mapper.xml中配置的完整构造入参名称，同时标记出ID属性
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    String id = resultMapNode.getStringAttribute("id",
      resultMapNode.getValueBasedIdentifier());
    String extend = resultMapNode.getStringAttribute("extends");
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend,
      discriminator, resultMappings, autoMapping);
    try {
      // 使用mapper.xml的配置校验真实的构造函数 fixme 构造函数必须使用 @Param 注解才能获取真实入参名称
      return resultMapResolver.resolve();
    } catch (IncompleteElementException e) {
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      return enclosingType;
    }
    return null;
  }

  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      // 标识构造函数的ID属性
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType,
                                                    List<ResultMapping> resultMappings) {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild,
        resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass,
      discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 多数据源匹配校验（出现重复时丢弃后来的SQL代码段）
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * mapper.xml 自定义 sql 的 databaseId 与 config.xml 启用 databaseId 匹配校验（已存在则忽略）
   *
   * @param id                 namespace+"."+sql.id
   * @param databaseId         databaseId（来自于mapper.xml配置的SQL代码段）
   * @param requiredDatabaseId databaseId（来自于config.xml的配置）
   * @return 校验结果
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      // 优先匹配 config.xml 中启用的 databaseId
      return requiredDatabaseId.equals(databaseId);
    }
    // config.xml 中未启用 databaseId 全部忽略
    if (databaseId != null) {
      return false;
    }

    // config.xml && mapper.xml 都未配置 databaseId

    if (!this.sqlFragments.containsKey(id)) {
      return true;
    }
    // skip this fragment if there is a previous one with a not null databaseId
    // 出现重复丢弃后来的SQL代码段
    XNode context = this.sqlFragments.get(id);
    return context.getStringAttribute("databaseId") == null;
  }

  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap", () ->
      processNestedResultMappings(context, Collections.emptyList(), resultType));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy"
      : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum,
      nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn,
      lazy);
  }

  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings,
                                             Class<?> enclosingType) {
    if (Arrays.asList("association", "collection", "case").contains(context.getName())
      && context.getStringAttribute("select") == null) {
      validateCollection(context, enclosingType);
      ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
      return resultMap.getId();
    }
    return null;
  }

  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
      && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
          "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        // ignore, bound type is not required
      }
      // 存在namespace且类型有效并且没有被解析过
      if (boundType != null && !configuration.hasMapper(boundType)) {
        // Spring may not know the real resource name so we set a flag
        // to prevent loading again this resource from the mapper interface
        // look at MapperAnnotationBuilder#loadXmlResource
        // Spring 可能扫描不到MyBatis特有的 *Mapper.java
        configuration.addLoadedResource("namespace:" + namespace);
        configuration.addMapper(boundType);
      }
    }
  }

}
