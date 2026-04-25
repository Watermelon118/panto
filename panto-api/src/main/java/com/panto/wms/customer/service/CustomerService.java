package com.panto.wms.customer.service;

import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.customer.dto.CreateCustomerRequest;
import com.panto.wms.customer.dto.CustomerPageResponse;
import com.panto.wms.customer.dto.CustomerResponse;
import com.panto.wms.customer.dto.CustomerSummaryResponse;
import com.panto.wms.customer.dto.UpdateCustomerRequest;
import com.panto.wms.customer.entity.Customer;
import com.panto.wms.customer.repository.CustomerRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户管理业务服务。
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * 创建客户服务。
     *
     * @param customerRepository 客户数据访问接口
     */
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * 返回分页筛选后的客户列表。
     *
     * @param keyword 公司名称或电话关键字，可为空
     * @param active 启用状态筛选，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页客户列表
     */
    @Transactional(readOnly = true)
    public CustomerPageResponse listCustomers(String keyword, Boolean active, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Customer> customers = customerRepository.search(toLikePattern(keyword), active, pageRequest);

        List<CustomerSummaryResponse> items = customers.getContent().stream()
            .map(this::toSummaryResponse)
            .toList();

        return new CustomerPageResponse(
            items,
            customers.getNumber(),
            customers.getSize(),
            customers.getTotalElements(),
            customers.getTotalPages()
        );
    }

    /**
     * 返回客户详情。
     *
     * @param customerId 客户 ID
     * @return 客户详情
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        return toResponse(findCustomerOrThrow(customerId));
    }

    /**
     * 创建客户。
     *
     * @param request 创建请求
     * @param operatorId 当前操作人 ID
     * @return 创建后的客户
     */
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, Long operatorId) {
        OffsetDateTime now = OffsetDateTime.now();

        Customer customer = new Customer();
        applyEditableFields(
            customer,
            request.companyName(),
            request.contactPerson(),
            request.phone(),
            request.email(),
            request.address(),
            request.gstNumber(),
            request.remarks()
        );
        customer.setActive(true);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setCreatedBy(operatorId);
        customer.setUpdatedBy(operatorId);

        return toResponse(customerRepository.save(customer));
    }

    /**
     * 更新客户。
     *
     * @param customerId 客户 ID
     * @param request 更新请求
     * @param operatorId 当前操作人 ID
     * @return 更新后的客户
     */
    @Transactional
    public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request, Long operatorId) {
        Customer customer = findCustomerOrThrow(customerId);

        applyEditableFields(
            customer,
            request.companyName(),
            request.contactPerson(),
            request.phone(),
            request.email(),
            request.address(),
            request.gstNumber(),
            request.remarks()
        );
        customer.setUpdatedAt(OffsetDateTime.now());
        customer.setUpdatedBy(operatorId);

        return toResponse(customerRepository.save(customer));
    }

    /**
     * 更新客户启用状态。
     *
     * @param customerId 客户 ID
     * @param active 目标启用状态
     * @param operatorId 当前操作人 ID
     * @return 更新后的客户
     */
    @Transactional
    public CustomerResponse updateCustomerStatus(Long customerId, boolean active, Long operatorId) {
        Customer customer = findCustomerOrThrow(customerId);
        customer.setActive(active);
        customer.setUpdatedAt(OffsetDateTime.now());
        customer.setUpdatedBy(operatorId);

        return toResponse(customerRepository.save(customer));
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private void applyEditableFields(
        Customer customer,
        String companyName,
        String contactPerson,
        String phone,
        String email,
        String address,
        String gstNumber,
        String remarks
    ) {
        customer.setCompanyName(normalizeRequired(companyName, "companyName"));
        customer.setContactPerson(normalize(contactPerson));
        customer.setPhone(normalize(phone));
        customer.setEmail(normalize(email));
        customer.setAddress(normalize(address));
        customer.setGstNumber(normalize(gstNumber));
        customer.setRemarks(normalize(remarks));
    }

    private CustomerSummaryResponse toSummaryResponse(Customer customer) {
        return new CustomerSummaryResponse(
            customer.getId(),
            customer.getCompanyName(),
            customer.getContactPerson(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getActive()
        );
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getCompanyName(),
            customer.getContactPerson(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getAddress(),
            customer.getGstNumber(),
            customer.getRemarks(),
            customer.getActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt(),
            customer.getCreatedBy(),
            customer.getUpdatedBy()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toLikePattern(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " must not be blank");
        }
        return normalized;
    }
}
