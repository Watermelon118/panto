package com.panto.wms.inbound.repository;

import com.panto.wms.inbound.entity.InboundItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 入库明细数据访问接口。
 */
public interface InboundItemRepository extends JpaRepository<InboundItem, Long> {

    /**
     * 查询指定入库单的所有明细行。
     *
     * @param inboundRecordId 入库单 ID
     * @return 明细列表
     */
    List<InboundItem> findByInboundRecordId(Long inboundRecordId);

    /**
     * 删除指定入库单的所有明细行，用于更新时先删后插。
     *
     * @param inboundRecordId 入库单 ID
     */
    void deleteByInboundRecordId(Long inboundRecordId);
}
