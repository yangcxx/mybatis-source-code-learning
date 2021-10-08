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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Clinton Begin
 */
public class XMLScriptBuilder extends BaseBuilder {

  private final XNode context;
  private boolean isDynamic;
  private final Class<?> parameterType;
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();

  public XMLScriptBuilder(Configuration configuration, XNode context) {
    this(configuration, context, null);
  }

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
    super(configuration);
    this.context = context;
    this.parameterType = parameterType;
    initNodeHandlerMap();
  }


  private void initNodeHandlerMap() {
    // insert|select|update|delete 里面支持的节点类型及对应的处理器
    nodeHandlerMap.put("trim", new TrimHandler());
    nodeHandlerMap.put("where", new WhereHandler());
    nodeHandlerMap.put("set", new SetHandler());
    nodeHandlerMap.put("foreach", new ForEachHandler());
    nodeHandlerMap.put("if", new IfHandler());
    nodeHandlerMap.put("choose", new ChooseHandler());
    nodeHandlerMap.put("when", new IfHandler());
    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
    nodeHandlerMap.put("bind", new BindHandler());
  }

  public SqlSource parseScriptNode() {
    /*
     * 判断SQL是动态的还是静态的
     * 是否为动态SQL：动态或静态并不只是说含有${}或#{}，是否含有if等条件语句也是判断依据之一（或者说除${}和#{}的规定外，还需要考虑SQL执行时是否需要动态拼接）
     *  1、含有#{}的简单SQL是静态的（简单：不含if等条件语句）
     *  2、含有${}的任意SQL都是动态的
     *  3、同时含有#{}和if等条件语句的SQL启动阶段也属于是动态的
     */
    MixedSqlNode rootSqlNode = parseDynamicTags(context);
    SqlSource sqlSource;
    if (isDynamic) {
      // 动态：不做任何处理
      sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
      // 静态：预编译将 #{} 替换为 ?
      sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
    }
    return sqlSource;
  }

  /**
   * insert|select|update|delete 节点校验，包括里面的静动态SQL，同时包含里面的子节点（if where 等）
   *
   * @param node 节点信息
   * @return 校验节点信息
   */
  protected MixedSqlNode parseDynamicTags(XNode node) {
    List<SqlNode> contents = new ArrayList<>();
    NodeList children = node.getNode().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      XNode child = node.newXNode(children.item(i));
      if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
        // Node 里面是纯文本
        String data = child.getStringBody("");
        TextSqlNode textSqlNode = new TextSqlNode(data);
        // 含有${}占位符或if等条件语句就是动态的
        if (textSqlNode.isDynamic()) {
          contents.add(textSqlNode);
          isDynamic = true;
        } else {
          // #{} 占位符就是静态的
          contents.add(new StaticTextSqlNode(data));
        }
      } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) {
        // issue #628
        // Node 包含子节点，比如 <if> <where> 等
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler == null) {
          throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
        }
        // 仅仅将配置信息存起来，TODO 启动时不做任何校验
        handler.handleNode(child, contents);
        isDynamic = true;
      }
    }
    return new MixedSqlNode(contents);
  }

  private interface NodeHandler {
    void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  }

  private class BindHandler implements NodeHandler {
    public BindHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      final String name = nodeToHandle.getStringAttribute("name");
      final String expression = nodeToHandle.getStringAttribute("value");
      final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
      targetContents.add(node);
    }
  }

  private class TrimHandler implements NodeHandler {
    public TrimHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String prefix = nodeToHandle.getStringAttribute("prefix");
      String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
      String suffix = nodeToHandle.getStringAttribute("suffix");
      String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
      TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
      targetContents.add(trim);
    }
  }

  private class WhereHandler implements NodeHandler {
    public WhereHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
      targetContents.add(where);
    }
  }

  private class SetHandler implements NodeHandler {
    public SetHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
      targetContents.add(set);
    }
  }

  private class ForEachHandler implements NodeHandler {
    public ForEachHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String collection = nodeToHandle.getStringAttribute("collection");
      String item = nodeToHandle.getStringAttribute("item");
      String index = nodeToHandle.getStringAttribute("index");
      String open = nodeToHandle.getStringAttribute("open");
      String close = nodeToHandle.getStringAttribute("close");
      String separator = nodeToHandle.getStringAttribute("separator");
      ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open,
        close, separator);
      targetContents.add(forEachSqlNode);
    }
  }

  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String test = nodeToHandle.getStringAttribute("test");
      IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
      targetContents.add(ifSqlNode);
    }
  }

  private class OtherwiseHandler implements NodeHandler {
    public OtherwiseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      targetContents.add(mixedSqlNode);
    }
  }

  private class ChooseHandler implements NodeHandler {
    public ChooseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      List<SqlNode> whenSqlNodes = new ArrayList<>();
      List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
      handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
      SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
      ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
      targetContents.add(chooseSqlNode);
    }

    private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes,
                                          List<SqlNode> defaultSqlNodes) {
      List<XNode> children = chooseSqlNode.getChildren();
      for (XNode child : children) {
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler instanceof IfHandler) {
          handler.handleNode(child, ifSqlNodes);
        } else if (handler instanceof OtherwiseHandler) {
          handler.handleNode(child, defaultSqlNodes);
        }
      }
    }

    private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
      SqlNode defaultSqlNode = null;
      if (defaultSqlNodes.size() == 1) {
        defaultSqlNode = defaultSqlNodes.get(0);
      } else if (defaultSqlNodes.size() > 1) {
        throw new BuilderException("Too many default (otherwise) elements in choose statement.");
      }
      return defaultSqlNode;
    }
  }

}
