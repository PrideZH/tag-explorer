package cn.pridezh.tagexplore.core.domain.po;

import cn.pridezh.tagexplore.core.domain.common.BasePO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author PrideZH
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName
public class Tag extends BasePO {

    private String name;

    @TableField("`group`")
    private Integer group;

}
