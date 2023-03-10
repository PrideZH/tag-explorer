package cn.pridezh.tagexplore.core.domain.po;

import cn.pridezh.tagexplore.core.domain.common.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.ibatis.type.ByteArrayTypeHandler;

/**
 * @author PrideZH
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(autoResultMap = true)
public class Cover extends BasePO {

    private Long resourceId;

    @TableField(typeHandler = ByteArrayTypeHandler.class)
    private byte[] data;

}
