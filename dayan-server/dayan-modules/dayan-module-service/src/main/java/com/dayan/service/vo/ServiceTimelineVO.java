package com.dayan.service.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 服务进度时间线聚合（需求/方案/安排/探访记录）。 */
@Data
public class ServiceTimelineVO {
    private List<Node> demands;
    private List<Node> solutions;
    private List<Node> arranges;
    private List<Node> visits;

    @Data
    public static class Node {
        /** 节点类型：demand/solution/arrange/visit */
        private String type;
        /** 标题 */
        private String title;
        /** 描述/内容摘要 */
        private String content;
        /** 时间 */
        private LocalDateTime time;
        /** 状态（如方案 isAccepted） */
        private Integer status;
    }
}
