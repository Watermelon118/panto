package com.panto.wms.destruction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建销毁记录请求体。
 *
 * @param batchId 目标批次 ID
 * @param quantityDestroyed 销毁数量
 * @param reason 销毁原因
 */
public record CreateDestructionRequest(
    @NotNull Long batchId,
    @NotNull @Min(1) Integer quantityDestroyed,
    @NotBlank @Size(max = 1000) String reason
) {
}
