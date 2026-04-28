package com.switchwon.api.repository;

import com.switchwon.api.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 내역 전체 조회 (최신순).
     * 과제 스펙 GET /order/list 의 응답 데이터 소스.
     */
    List<Order> findAllByOrderByCreatedAtDesc();
}
