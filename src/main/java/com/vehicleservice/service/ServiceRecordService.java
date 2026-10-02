package com.vehicleservice.service;

import com.vehicleservice.dto.ServiceRecordForm;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.ServiceRecordRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ServiceRecordService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleService vehicleService;
    private final ReminderService reminderService;

    public ServiceRecordService(ServiceRecordRepository serviceRecordRepository,
                                VehicleService vehicleService,
                                ReminderService reminderService) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.vehicleService = vehicleService;
        this.reminderService = reminderService;
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> search(Long userId, Long vehicleId, ServiceType serviceType,
                                      LocalDate fromDate, LocalDate toDate, String query, String sortKey) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return serviceRecordRepository.search(userId, vehicleId, serviceType, fromDate, toDate, q, toSort(sortKey));
    }

    /** Full history of one vehicle (ownership checked), newest first. */
    @Transactional(readOnly = true)
    public List<ServiceRecord> getVehicleHistory(Long vehicleId, Long userId, ServiceType serviceType,
                                                 LocalDate fromDate, LocalDate toDate) {
        vehicleService.getVehicle(vehicleId, userId);
        return serviceRecordRepository.search(userId, vehicleId, serviceType, fromDate, toDate, null, toSort("date_desc"));
    }

    @Transactional(readOnly = true)
    public ServiceRecord getRecord(Long recordId, Long userId) {
        return serviceRecordRepository.findByIdAndVehicleUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record", recordId));
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> getRecentRecords(Long userId) {
        return serviceRecordRepository.findTop5ByVehicleUserIdOrderByServiceDateDescIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countRecords(Long userId) {
        return serviceRecordRepository.countByVehicleUserId(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalServiceCost(Long userId) {
        return serviceRecordRepository.sumCostByUserId(userId);
    }

    public ServiceRecord create(Long userId, ServiceRecordForm form) {
        Vehicle vehicle = vehicleService.getVehicle(form.getVehicleId(), userId);
        ServiceRecord record = new ServiceRecord();
        record.setVehicle(vehicle);
        applyForm(record, form);
        ServiceRecord saved = serviceRecordRepository.save(record);
        vehicleService.updateMileageIfHigher(vehicle, form.getMileage());
        if (form.isCreateReminder()) {
            reminderService.createFromServiceRecord(saved);
        }
        return saved;
    }

    public ServiceRecord update(Long recordId, Long userId, ServiceRecordForm form) {
        ServiceRecord record = getRecord(recordId, userId);
        Vehicle vehicle = vehicleService.getVehicle(form.getVehicleId(), userId);
        record.setVehicle(vehicle);
        applyForm(record, form);
        vehicleService.updateMileageIfHigher(vehicle, form.getMileage());
        return record;
    }

    public void delete(Long recordId, Long userId) {
        serviceRecordRepository.delete(getRecord(recordId, userId));
    }

    /** Adds up the cost of a list of records (used for filtered totals). */
    public static BigDecimal sumCost(List<ServiceRecord> records) {
        return records.stream()
                .map(ServiceRecord::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Maps the "sort" dropdown value to a database sort order. */
    public static Sort toSort(String sortKey) {
        String key = sortKey == null ? "date_desc" : sortKey;
        return switch (key) {
            case "date_asc" -> Sort.by(Sort.Order.asc("serviceDate"), Sort.Order.asc("id"));
            case "cost_desc" -> Sort.by(Sort.Order.desc("cost"), Sort.Order.desc("serviceDate"));
            case "cost_asc" -> Sort.by(Sort.Order.asc("cost"), Sort.Order.desc("serviceDate"));
            case "mileage_desc" -> Sort.by(Sort.Order.desc("mileage"), Sort.Order.desc("serviceDate"));
            default -> Sort.by(Sort.Order.desc("serviceDate"), Sort.Order.desc("id"));
        };
    }

    private void applyForm(ServiceRecord record, ServiceRecordForm form) {
        record.setServiceType(form.getServiceType());
        record.setServiceDate(form.getServiceDate());
        record.setMileage(form.getMileage());
        record.setCost(form.getCost());
        record.setServiceCenter(blankToNull(form.getServiceCenter()));
        record.setDescription(blankToNull(form.getDescription()));
        record.setNextServiceDate(form.getNextServiceDate());
        record.setNextServiceMileage(form.getNextServiceMileage());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
