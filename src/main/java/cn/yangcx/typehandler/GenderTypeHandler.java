package cn.yangcx.typehandler;

import cn.yangcx.GenderEnum;
import org.apache.ibatis.type.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器<br/>
 * Date: 2020/6/23 7:24 <br/>
 *
 * @author YANGCX
 */
@MappedTypes(value = GenderEnum.class)
@MappedJdbcTypes(value = {JdbcType.INTEGER})
public class GenderTypeHandler extends EnumTypeHandler<GenderEnum> {

  public GenderTypeHandler(Class<GenderEnum> type) {
    super(type);
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, GenderEnum parameter, JdbcType jdbcType) throws SQLException {
    ps.setInt(i, parameter.getCode());
  }

  @Override
  public GenderEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return GenderEnum.get(rs.getInt(columnName));
  }

  @Override
  public GenderEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return GenderEnum.get(rs.getInt(columnIndex));
  }

  @Override
  public GenderEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return GenderEnum.get(cs.getInt(columnIndex));
  }
}
