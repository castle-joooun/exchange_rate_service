package com.switchwon.api.repository;

import com.switchwon.api.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 내역 전체 조회 (id 오름차순).
     * 과제 스펙 GET /order/list 의 응답 데이터 소스.
     * id 가 IDENTITY 자동 증가이므로 사실상 생성 순서(오래된 → 최신)와 동일하다.
     */
    List<Order> findAllByOrderByIdAsc();
}
