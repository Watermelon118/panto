package com.panto.wms.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.customer.dto.CreateCustomerRequest;
import com.panto.wms.customer.dto.CustomerResponse;
import com.panto.wms.customer.dto.UpdateCustomerRequest;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import com.panto.wms.order.domain.OrderStatus;
import com.panto.wms.order.entity.Order;
import com.panto.wms.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 客户服务测试。
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CustomerService customerService;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    @Test
    void createCustomerShouldSaveCustomerWhenRequestIsValid() {
        CreateCustomerRequest request = buildCreateRequest();
        Customer savedCustomer = buildCustomer(1L, "Panto Trading Ltd");

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        mockEmptyOrderHistory(1L);

        CustomerResponse response = customerService.createCustomer(request, 7L);

        assertEquals(1L, response.id());
        assertEquals("Panto Trading Ltd", response.companyName());
        assertEquals("Alex Chen", response.contactPerson());
        assertTrue(response.active());

        verify(customerRepository).save(customerCaptor.capture());
        Customer captured = customerCaptor.getValue();
        assertEquals("Panto Trading Ltd", captured.getCompanyName());
        assertEquals("Alex Chen", captured.getContactPerson());
        assertEquals("021888999", captured.getPhone());
        assertEquals("alex@panto.co.nz", captured.getEmail());
        assertEquals("99 Queen Street", captured.getAddress());
        assertEquals("GST-7788", captured.getGstNumber());
        assertEquals("Preferred morning delivery", captured.getRemarks());
        assertEquals(Boolean.TRUE, captured.getActive());
        assertEquals(7L, captured.getCreatedBy());
        assertEquals(7L, captured.getUpdatedBy());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void updateCustomerShouldSaveUpdatedFieldsWhenCustomerExists() {
        Customer existingCustomer = buildCustomer(1L, "Panto Trading Ltd");
        UpdateCustomerRequest request = new UpdateCustomerRequest(
            "Harbour Foods Ltd",
            "Mia Zhang",
            "0221234567",
            "mia@harbourfoods.co.nz",
            "12 Customhouse Quay",
            "GST-9000",
            "Call before arrival"
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockEmptyOrderHistory(1L);

        CustomerResponse response = customerService.updateCustomer(1L, request, 11L);

        assertEquals("Harbour Foods Ltd", response.companyName());
        assertEquals("Mia Zhang", response.contactPerson());
        assertEquals("0221234567", response.phone());
        assertEquals("mia@harbourfoods.co.nz", response.email());

        verify(customerRepository).save(customerCaptor.capture());
        Customer captured = customerCaptor.getValue();
        assertEquals("Harbour Foods Ltd", captured.getCompanyName());
        assertEquals("Mia Zhang", captured.getContactPerson());
        assertEquals("0221234567", captured.getPhone());
        assertEquals("mia@harbourfoods.co.nz", captured.getEmail());
        assertEquals("12 Customhouse Quay", captured.getAddress());
        assertEquals("GST-9000", captured.getGstNumber());
        assertEquals("Call before arrival", captured.getRemarks());
        assertEquals(11L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void getCustomerShouldThrowWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> customerService.getCustomer(99L)
        );

        assertEquals(ErrorCode.CUSTOMER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getCustomerShouldReturnOrderHistoryAndCumulativeSpend() {
        Customer customer = buildCustomer(1L, "Panto Trading Ltd");
        Order order = new Order();
        order.setId(300L);
        order.setOrderNumber("ORD-20260426-001");
        order.setStatus(OrderStatus.ACTIVE);
        order.setTotalAmount(new BigDecimal("276.00"));
        order.setCreatedAt(OffsetDateTime.parse("2026-04-26T10:00:00+12:00"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(
            1L,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        )).thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));
        when(orderRepository.sumTotalAmountByCustomerIdAndStatus(1L, OrderStatus.ACTIVE))
            .thenReturn(new BigDecimal("276.00"));

        CustomerResponse response = customerService.getCustomer(1L);

        assertEquals(new BigDecimal("276.00"), response.cumulativeSpend());
        assertEquals(1L, response.totalOrderCount());
        assertEquals(1, response.orderHistory().size());
        assertEquals("ORD-20260426-001", response.orderHistory().getFirst().orderNumber());
        assertEquals(OrderStatus.ACTIVE, response.orderHistory().getFirst().status());
    }

    @Test
    void updateCustomerStatusShouldPersistActiveFlag() {
        Customer existingCustomer = buildCustomer(1L, "Panto Trading Ltd");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockEmptyOrderHistory(1L);

        CustomerResponse response = customerService.updateCustomerStatus(1L, false, 15L);

        assertFalse(response.active());

        verify(customerRepository).save(customerCaptor.capture());
        Customer captured = customerCaptor.getValue();
        assertFalse(captured.getActive());
        assertEquals(15L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void listCustomersShouldReturnPagedItems() {
        Customer customer = buildCustomer(1L, "Panto Trading Ltd");
        PageImpl<Customer> page = new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1);

        when(customerRepository.search(
            "%panto%",
            true,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"))
        )).thenReturn(page);

        var response = customerService.listCustomers("Panto", true, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("Panto Trading Ltd", response.items().getFirst().companyName());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    private CreateCustomerRequest buildCreateRequest() {
        return new CreateCustomerRequest(
            "Panto Trading Ltd",
            "Alex Chen",
            "021888999",
            "alex@panto.co.nz",
            "99 Queen Street",
            "GST-7788",
            "Preferred morning delivery"
        );
    }

    private Customer buildCustomer(Long id, String companyName) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCompanyName(companyName);
        customer.setContactPerson("Alex Chen");
        customer.setPhone("021888999");
        customer.setEmail("alex@panto.co.nz");
        customer.setAddress("99 Queen Street");
        customer.setGstNumber("GST-7788");
        customer.setRemarks("Preferred morning delivery");
        customer.setActive(true);
        customer.setCreatedAt(OffsetDateTime.now().minusDays(1));
        customer.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        customer.setCreatedBy(1L);
        customer.setUpdatedBy(1L);
        return customer;
    }

    private void mockEmptyOrderHistory(Long customerId) {
        when(orderRepository.findByCustomerId(
            customerId,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(orderRepository.sumTotalAmountByCustomerIdAndStatus(customerId, OrderStatus.ACTIVE))
            .thenReturn(BigDecimal.ZERO);
    }
}
