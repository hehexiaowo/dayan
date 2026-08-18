package com.dayan.tool.dto;

import com.dayan.tool.vo.ToolAiCreatorRefsVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 创建 AI 创作项目（目的+素材引用与快照+形态/风格/读者+主题）；素材由前端聚合提交 */
@Data
public class ToolAiCreatorCreateDTO {
    /** product=产品宣传/park=机构推荐/science=科普获客 */
    @NotBlank(message = "文章目的必选")
    private String purpose;
    /** 1图文 2朋友圈 3视频脚本 4小红书 */
    @NotNull(message = "内容形态必选")
    @Min(value = 1, message = "内容形态取值 1-4")
    @Max(value = 4, message = "内容形态取值 1-4")
    private Integer contentType;
    @NotBlank(message = "写作风格必选")
    private String styleCode;
    private String audience;
    @Size(max = 500, message = "主题不能超过 500 字")
    private String topic;
    /** 素材引用（含展示名，保存成品与回显用） */
    @Valid
    private ToolAiCreatorRefsVO materialRefs;
    /** 素材快照（前端聚合 {type,title,text} 块，digest 阶段一次性消费） */
    @Valid
    private List<AiMaterialBlockDTO> materials;
}
